package edu.gemini.aspen.gds.keywords

import cats.effect.{ Async, Ref }
import cats.effect.syntax.all._
import cats.syntax.all._
import edu.gemini.epics.EpicsReader
import edu.gemini.aspen.gds.configuration.{ KeywordConfiguration, KeywordConfigurationItem }
import edu.gemini.aspen.gds.model.KeywordSource
import edu.gemini.aspen.gds.syntax.all._
import edu.gemini.aspen.giapi.data.ObservationEvent
import java.util.logging.Logger
import edu.gemini.aspen.gds.fits.FitsValue

object EpicsKeywordCollector {
  private val logger = Logger.getLogger(this.getClass.getName)

  def apply[F[_]](
    reader:     Ref[F, Option[EpicsReader]],
    config:     KeywordConfiguration
  )(implicit F: Async[F]): KeywordCollector[F] = {

    def processConfig(
      config: KeywordConfiguration
    ): Map[ObservationEvent, Map[String, List[KeywordConfigurationItem]]] = {
      val forEpics = config.forKeywordSource(KeywordSource.Epics)
      val byEvent  = forEpics.items.groupBy(_.event)
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
                // increment the counter for each channel, then start collection on a fiber
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
      ): F[Unit] = reader.get.flatMap {
        // Instead of just a logger warning, we could add error CollectedKeywords for each item in the list if
        // that would be preferable from a diagnostic standpoint. We wouldn't have to include the event and channel
        // in all of the error messages, then.
        case None          =>
          logger.warningF(
            s"EpicsReader service not available for keyword collection for event $event, channel $channelName"
          )
        case Some(ereader) =>
          F.blocking(ereader.getChannelAsync(channelName))
            .flatMap { channel =>
              if (!channel.isValid)
                logger.warningF(
                  s"Epics channel $channel not valid for keyword collection for event $event"
                )
              else
                F.blocking(Option(channel.getAll())).flatMap {
                  case None       =>
                    logger.warningF(s"Expics channel $channelName returned null for event $event")
                  case Some(data) => processChannel(data, items, adder)
                }
            }
            .attempt
            .flatMap {
              case Left(e)  =>
                logger.warningF(
                  s"Error getting EPICS keywords for event $event, channel $channelName: ${e.getMessage}"
                )
              case Right(_) => F.unit
            }
      } >> count.decr // should always get decremented because of the `attempt`s

      def processChannel(
        data:  java.util.List[_],
        items: List[KeywordConfigurationItem],
        adder: CollectedKeyword => F[Unit]
      ): F[Unit] =
        items.traverse { item =>
          if (item.arrayIndex.value >= data.size())
            adder(
              CollectedKeyword.error(item, s"Epics return array only had ${data.size()} elements.")
            )
          else
            // data is a mutable list, so use delay just to be sure
            F.delay(data.get(item.arrayIndex.value)).attempt.flatMap {
              case Left(e)      =>
                adder(CollectedKeyword.error(item, s"Error reading epics array: ${e.getMessage}"))
              case Right(value) =>
                FitsValue
                  .fromAny(item.dataType, value)
                  .attempt
                  .map {
                    case Left(e)   => CollectedKeyword.error(item, e.getMessage)
                    case Right(fv) => CollectedKeyword.value(item, fv)
                  }
                  .flatMap(adder)
            }
        }.void
    }
  }
}
