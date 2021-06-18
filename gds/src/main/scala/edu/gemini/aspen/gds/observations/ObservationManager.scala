package edu.gemini.aspen.gds.observations

import cats.effect.{ Async, Clock }
import cats.effect.std.QueueSink
import cats.syntax.all._
import edu.gemini.aspen.gds.keywords.{ CollectedKeyword, KeywordManager }
import edu.gemini.aspen.gds.observations.ObservationStateEvent._
import edu.gemini.aspen.gds.syntax.all._
import edu.gemini.aspen.giapi.data.DataLabel
import io.chrisdavenport.mapref.MapRef
import io.chrisdavenport.mapref.implicits._
import java.util.logging.Logger
import scala.concurrent.duration._

trait ObservationManager[F[_]] {
  def process(stateEvent: ObservationStateEvent): F[Unit]
}

object ObservationManager {
  private val logger = Logger.getLogger(this.getClass.getName)

  // TODO: Make this a configuration value.
  val lifetime = 20.minutes

  def apply[F[_]: Async](
    keywordManager: KeywordManager[F],
    obsStateQ:      QueueSink[F, ObservationStateEvent],
    fitsQ:          QueueSink[F, (DataLabel, List[CollectedKeyword])]
  ): F[ObservationManager[F]] =
    MapRef.ofConcurrentHashMap[F, DataLabel, ObservationItem[F]]().map { mapref =>
      new ObservationManager[F] {
        def process(stateEvent: ObservationStateEvent): F[Unit] = stateEvent match {
          case Start(dataLabel, programId) =>
            for {
              _   <- logger.infoF(s"Starting observation $dataLabel")
              now <- Clock[F].realTime
              fsm <-
                ObservationFSM(dataLabel, obsStateQ)
              // TODO: If it already exists, log an error and keep going. Should never happen.
              _   <- mapref.setKeyValue(dataLabel, ObservationItem(programId, fsm, now + lifetime))
              _   <- keywordManager.initialize(dataLabel)
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
            withObsItem(dataLabel, e)(_.fsm.addObservationEvent(obsEvent))

          case e @ AddKeyword(dataLabel, keyword) =>
            withObsItem(dataLabel, e)(_ => keywordManager.add(dataLabel, keyword))

          case e @ CollectKeywords(dataLabel, obsEvent) =>
            withObsItem(dataLabel, e)(_ => keywordManager.collect(dataLabel, obsEvent))

          case e @ Step(dataLabel) => withObsItem(dataLabel, e)(_.fsm.step)

          case e @ Complete(dataLabel) =>
            withObsItem(dataLabel, e) { _ =>
              for {
                kws <- keywordManager.get(dataLabel)
                _   <- logger.infoF(s"Got these keywords for $dataLabel:\n\t${kws.mkString("\n\t")}")
                _   <- fitsQ.offer((dataLabel, kws))
                _   <- obsStateQ.offer(Delete(dataLabel))
              } yield ()
            }

          case PurgeStale =>
            for {
              _    <- logger.infoF("Checking for expired observations.")
              keys <- mapref.keys
              now  <- Clock[F].realTime
              _    <- keys.traverse(purgeIfNeeded(_, now))
            } yield ()
        }

        def withObsItem(dataLabel: DataLabel, event: ObservationStateEvent)(
          action:                  ObservationItem[F] => F[Unit]
        ): F[Unit] =
          mapref(dataLabel).get.flatMap {
            case None       => logger.warningF(s"Observation not found for event: $event")
            case Some(item) => action(item)
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

  // need to add an expiration
  final case class ObservationItem[F[_]](
    programId:  String,
    fsm:        ObservationFSM[F],
    expiration: FiniteDuration
  )
}
