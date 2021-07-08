package edu.gemini.aspen.gds.keywords

import cats.effect.kernel.{ Async, RefSource }
import cats.effect.syntax.all._
import cats.syntax.all._
import edu.gemini.epics.EpicsReader
import edu.gemini.aspen.gds.configuration.KeywordConfigurationItem
import edu.gemini.aspen.gds.model.{ GdsError, KeywordSource }
import edu.gemini.aspen.gds.syntax.all._
import edu.gemini.aspen.giapi.data.ObservationEvent
import java.util.logging.Logger
import edu.gemini.aspen.gds.fits.FitsValue

object EpicsKeywordCollector {
  private val logger = Logger.getLogger(this.getClass.getName)

  def apply[F[_]](
    readerRef:  RefSource[F, Option[EpicsReader]],
    config:     List[KeywordConfigurationItem]
  )(implicit F: Async[F]): KeywordCollector[F] = {

    def processConfig(
      config: List[KeywordConfigurationItem]
    ): Map[ObservationEvent, Map[String, List[KeywordConfigurationItem]]] = {
      val forEpics = config.forSource(KeywordSource.Epics)
      val byEvent  = forEpics.groupBy(_.event)
      byEvent.map { case (k, v) => (k, v.groupBy(_.channel.name)) }
    }

    new KeywordCollector[F] {
      val configMap = processConfig(config)

      def collect(
        event: ObservationEvent,
        count: Count[F],
        adder: CollectedKeyword => F[Unit]
      ): F[Unit] =
        configMap.get(event) match {
          case None             => F.unit
          case Some(channelMap) =>
            channelMap.toList
              .parTraverseN(10) { case (c, l) =>
                // Increment the counter for each channel, then start collection on a fiber.
                // This is probably not cancellation safe because of the `start`.
                count.incr >> getForChannel(c, l, event, count, adder).start
              }
              .void
        }

      def getForChannel(
        channelName: String,
        items:       List[KeywordConfigurationItem],
        event:       ObservationEvent,
        count:       Count[F],
        adder:       CollectedKeyword => F[Unit]
      ): F[Unit] = {
        val keywordsF = for {
          _        <-
            logger.infoF(
              s"Collecting ${items.length} EPICS keywords for channel $channelName, event $event: ${items
                .map(_.keyword.key)
                .mkString(", ")}"
            )
          oreader  <- readerRef.get
          keywords <- oreader match {
                        case Some(reader) => getAndReadChannel(reader, channelName, items)
                        case None         =>
                          logger.warningF("EpicsReader service not available") >>
                            F.raiseError[List[CollectedKeyword]](
                              GdsError(
                                s"EpicsReader service not available"
                              )
                            )
                      }
        } yield keywords
        keywordsF
          .handleError(e => allBad(items, e.getMessage))
          .flatMap(_.traverse(kw => logger.infoF(s"Collected $kw") >> adder(kw)))
          .void >> count.decr
      }

      def getAndReadChannel(
        reader:      EpicsReader,
        channelName: String,
        items:       List[KeywordConfigurationItem]
      ): F[List[CollectedKeyword]] =
        F.blocking(reader.getChannelAsync(channelName))
          .flatMap { channel =>
            if (!channel.isValid)
              allBad(items, s"Epics channel $channelName not valid").pure
            else
              F.blocking(Option(channel.getAll())).flatMap {
                case None       =>
                  allBad(items, s"Epics channel $channelName returned null").pure
                case Some(data) => processChannelData(data, items)
              }
          }

      def processChannelData(
        data:  java.util.List[_],
        items: List[KeywordConfigurationItem]
      ): F[List[CollectedKeyword]] =
        items.traverse { item =>
          if (item.arrayIndex.value >= data.size())
            CollectedKeyword
              .error(item, s"Epics return array only had ${data.size()} elements.")
              .pure
          else
            // data is a mutable list, so use delay just to be sure
            F.delay(data.get(item.arrayIndex.value)).attempt.flatMap {
              case Left(e)      =>
                CollectedKeyword.error(item, s"Error reading epics array: ${e.getMessage}").pure
              case Right(value) =>
                FitsValue
                  .fromAny(item.dataType, value)
                  .attempt
                  .map {
                    case Left(e)   => CollectedKeyword.error(item, e.getMessage)
                    case Right(fv) => CollectedKeyword.value(item, fv)
                  }
            }
        }

      def allBad(items: List[KeywordConfigurationItem], message: String): List[CollectedKeyword] =
        items.map(item => CollectedKeyword.error(item, message))
    }
  }
}
