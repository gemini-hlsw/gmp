package edu.gemini.aspen.gds.observations

import cats.effect.{ Async, Clock, Ref }
import cats.effect.std.MapRef
import cats.effect.std.QueueSink
import cats.syntax.all._
import edu.gemini.aspen.gds.configuration.ObservationConfig
import edu.gemini.aspen.gds.keywords.{ CollectedKeyword, KeywordManager }
import edu.gemini.aspen.gds.observations.ObservationStateEvent._
import edu.gemini.aspen.gds.syntax.all._
import edu.gemini.aspen.giapi.data.DataLabel
import java.util.logging.Logger
import scala.concurrent.duration._

trait ObservationManager[F[_]] {
  def process(stateEvent: ObservationStateEvent): F[Unit]
}

object ObservationManager {
  private val logger = Logger.getLogger(this.getClass.getName)

  def apply[F[_]](
    config:         ObservationConfig,
    keywordManager: KeywordManager[F],
    obsStateQ:      QueueSink[F, ObservationStateEvent],
    fitsQ:          QueueSink[F, (DataLabel, List[CollectedKeyword])]
  )(implicit F: Async[F]): F[ObservationManager[F]] =
    Ref.of(Map[DataLabel, ObservationItem[F]]()).map { refOfMap =>
      val mapref = MapRef.fromSingleImmutableMapRef(refOfMap)

      def addDataLabel(dataLabel: DataLabel): F[ObservationItem[F]] =
        for {
          _   <- logIfExists(dataLabel)
          now <- Clock[F].realTime
          fsm <-
            ObservationFSM(config.eventRetries, dataLabel, obsStateQ)
          item = ObservationItem(fsm, now + config.lifespan)
          _   <- mapref.setKeyValue(dataLabel, item)
          _   <- keywordManager.initialize(dataLabel)
        } yield item

      def logIfExists(dataLabel: DataLabel): F[Unit] =
        mapref(dataLabel).get.flatMap {
          case None    => F.unit
          case Some(_) =>
            logger.severeF(
              s"A new start event has been received for observation $dataLabel, which already exists. Overwriting old observation."
            )
        }

      def logEvent(stateEvent: ObservationStateEvent): F[Unit] =
        logger.infoF(s"Got an event $stateEvent")

      new ObservationManager[F] {
        def process(stateEvent: ObservationStateEvent): F[Unit] =
          stateEvent match {
            case Start(dataLabel) =>
              for {
                _ <- logger.infoF(s"Starting observation $dataLabel")
                i <- addDataLabel(dataLabel)
                _ <- i.fsm.startObservation
              } yield ()

            case e @ Stop(dataLabel) => withObsItem(dataLabel, e)(_.fsm.stopObservation)

            case Abort(dataLabel) =>
              logger.warningF(s"Received Abort event for observation $dataLabel") >>
                obsStateQ.offer(Delete(dataLabel))

            case Delete(dataLabel) =>
              logger.infoF(s"Removing observation $dataLabel") >> keywordManager.delete(
                dataLabel
              ) >> mapref.unsetKey(dataLabel)

            case e @ AddObservationEvent(dataLabel, obsEvent) =>
              logEvent(e) *> withObsItem(dataLabel, e)(_.fsm.addObservationEvent(obsEvent))

            case e @ AddKeyword(dataLabel, keyword) =>
              withObsItem(dataLabel, e)(_ => keywordManager.add(dataLabel, keyword))

            case e @ CollectKeywords(dataLabel, obsEvent) =>
              logEvent(e) *> withObsItem(dataLabel, e)(_ =>
                keywordManager.collect(dataLabel, obsEvent)
              )

            case e @ Step(dataLabel) => withObsItem(dataLabel, e)(_.fsm.step)

            case e @ Complete(dataLabel) =>
              withObsItem(dataLabel, e) { _ =>
                for {
                  kws <- keywordManager.get(dataLabel)
                  _   <-
                    logger.infoF(s"Got these keywords for $dataLabel:\n\t${kws.mkString("\n\t")}")
                  _   <- fitsQ.offer((dataLabel, kws))
                  _   <- obsStateQ.offer(Delete(dataLabel))
                } yield ()
              }

            case PurgeStale =>
              for {
                _    <- logger.infoF("Checking for expired observations.")
                keys <- refOfMap.get.map(_.keys.toList)
                now  <- Clock[F].realTime
                _    <- keys.traverse(purgeIfNeeded(_, now))
              } yield ()
          }

        def withObsItem(dataLabel: DataLabel, event: ObservationStateEvent)(
          action: ObservationItem[F] => F[Unit]
        ): F[Unit] =
          mapref(dataLabel).get.flatMap {
            case None =>
              for {
                _    <- logger.warningF(s"Observation not found for event: $event")
                item <- addDataLabel(dataLabel)
                _    <- action(item)
              } yield ()

            case Some(item) =>
              logger.infoF(s"with obs item $event") *> action(item)
          }

        def purgeIfNeeded(dataLabel: DataLabel, now: FiniteDuration): F[Unit] =
          mapref(dataLabel).get.flatMap {
            case Some(item) if item.expiration < now =>
              logger.infoF(s"Purging observation $dataLabel due to expiration") >> obsStateQ
                .offer(
                  Delete(dataLabel)
                )
            case _                                   => Async[F].unit
          }

      }
    }

  final case class ObservationItem[F[_]](
    fsm:        ObservationFSM[F],
    expiration: FiniteDuration
  )
}
