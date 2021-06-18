package edu.gemini.aspen.gds.keywords

import cats.effect.kernel._
import cats.effect.syntax.all._
import cats.syntax.all._
import edu.gemini.aspen.gds.syntax.all._
import edu.gemini.aspen.giapi.data.{ DataLabel, ObservationEvent }
import io.chrisdavenport.mapref.MapRef
import io.chrisdavenport.mapref.implicits._
import java.util.logging.Logger
import scala.concurrent.duration._

sealed trait KeywordManager[F[_]] {
  def initialize(dataLabel: DataLabel): F[Unit]
  def get(dataLabel:        DataLabel): F[List[CollectedKeyword]]
  def add(dataLabel:        DataLabel, keyword: CollectedKeyword): F[Unit]
  def collect(dataLabel:    DataLabel, event:   ObservationEvent): F[Unit]
  def delete(dataLabel:     DataLabel): F[Unit]
}

object KeywordManager {
  private val logger = Logger.getLogger(this.getClass.getName)

  // TODO: Make sleep time and number of attempts configuration values.
  val numAttempts = 5
  val sleeptTime  = 5.seconds

  def apply[F[_]: Async](
    collectors: KeywordCollector[F]*
  ): F[KeywordManager[F]] =
    MapRef.ofConcurrentHashMap[F, DataLabel, KeywordItem]().map { mapref =>
      new KeywordManager[F] {

        def initialize(dataLabel: DataLabel): F[Unit] =
          // TODO: If it already exists, log an error and keep going. Should never happen.
          logger.infoF(s"Initializing keyword manager for observation $dataLabel") >>
            mapref.setKeyValue(dataLabel, KeywordItem(0, List.empty))

        def get(dataLabel: DataLabel): F[List[CollectedKeyword]] = tryGet(dataLabel, 3)

        def add(dataLabel: DataLabel, keyword: CollectedKeyword): F[Unit] =
          mapref
            .modifyKeyValueIfSet(dataLabel,
                                 item => item.copy(keywords = keyword :: item.keywords) -> true
            )
            .flatMap {
              case None    =>
                logger
                  .warningF(s"Received, too late, keyword $keyword for observation $dataLabel")
              case Some(_) => logger.infoF(s"Added keyword $keyword to observation $dataLabel")
            }

        def collect(dataLabel: DataLabel, event: ObservationEvent): F[Unit] = {
          val count = makeCounter(dataLabel)
          val adder = adderFunc(dataLabel)
          logger.infoF(s"Collecting keywords for observation $dataLabel, event $event") >>
            collectors.parTraverseN(10)(_.collect(event, count, adder)).void
        }

        def delete(dataLabel: DataLabel): F[Unit] = logger.infoF(
          s"Removing keywords for observation $dataLabel"
        ) >> mapref.unsetKey(dataLabel)

        private def updateCount(dataLabel: DataLabel, incr: Int): F[Unit] =
          mapref.updateKeyValueIfSet(dataLabel,
                                     item => item.copy(waitingCount = item.waitingCount + incr)
          )

        private def makeCounter(dataLabel: DataLabel): Count[F] = new Count[F] {
          def incr: F[Unit] = updateCount(dataLabel, 1)
          def decr: F[Unit] = updateCount(dataLabel, -1)
        }

        private def adderFunc(dataLabel: DataLabel): CollectedKeyword => F[Unit] =
          ckw => add(dataLabel, ckw)

        private def tryGet(dataLabel: DataLabel, remaining: Int): F[List[CollectedKeyword]] =
          mapref(dataLabel).get.flatMap {
            case None                                             =>
              logger.severeF(
                s"Observation $dataLabel not found - no collected keywords are available"
              ) >> List.empty.pure[F]
            case Some(ki) if ki.waitingCount > 0 && remaining > 0 =>
              logger.infoF(
                s"Waiting for ${ki.waitingCount} keywords for observation $dataLabel, $remaining attempts left"
              ) >> Async[F].sleep(5.seconds) >> tryGet(dataLabel, remaining - 1)
            case Some(ki) if ki.waitingCount > 0                  =>
              logger.warningF(
                s"Waiting for ${ki.waitingCount} keywords for observation $dataLabel, no attempts left. Returning what we have"
              ) >> ki.keywords.pure[F]
            case Some(ki)                                         =>
              logger.infoF(s"Returning keywords for observation $dataLabel") >> ki.keywords
                .pure[F]
          }
      }
    }

  final case class KeywordItem(waitingCount: Int, keywords: List[CollectedKeyword])
}
