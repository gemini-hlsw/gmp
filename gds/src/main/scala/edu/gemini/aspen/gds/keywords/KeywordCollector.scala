package edu.gemini.aspen.gds.keywords

import cats.effect._
import cats.effect.syntax.all._
import cats.syntax.all._
import edu.gemini.aspen.gds.configuration.{ KeywordConfiguration, KeywordConfigurationItem }
import edu.gemini.aspen.gds.fits.FitsValue
import edu.gemini.aspen.gds.model.KeywordSource
import edu.gemini.aspen.gds.syntax.all._
import edu.gemini.aspen.giapi.data.ObservationEvent
import java.util.logging.Logger

trait KeywordCollector[F[_]] {
  def collect(event: ObservationEvent, count: Count[F], adder: CollectedKeyword => F[Unit]): F[Unit]
}

// The retriver function can return a failed IO, but should handle it's own timeouts and return
// within a few seconds at most. If needed, it should also do it's own shift to a blocking pool.
object KeywordCollector {
  private val logger = Logger.getLogger(this.getClass.getName)

  def apply[F[_]: Async](
    keywordSource: KeywordSource,
    config:        KeywordConfiguration,
    retriever:     KeywordConfigurationItem => F[FitsValue]
  ): KeywordCollector[F] =
    new KeywordCollector[F] {
      val configForSource = config.forKeywordSource(keywordSource)

      def collect(
        event: ObservationEvent,
        count: Count[F],
        adder: CollectedKeyword => F[Unit]
      ): F[Unit] =
        // If we decide to support cancellation, need to worry about decrementing count.
        configForSource
          .forEvent(event)
          .items
          .parTraverseN(10)(ci => count.incr >> retrieveOne(ci, count, adder).start)
          .void

      private def retrieveOne(
        configItem: KeywordConfigurationItem,
        count:      Count[F],
        adder:      CollectedKeyword => F[Unit]
      ): F[Unit] = for {
        _  <-
          logger.infoF(
            s"Collecting keyword ${configItem.keyword} for keyword source $keywordSource, event ${configItem.event}"
          )
        kw <- safeRetrieve(configItem)
        _  <- logger.infoF(s"Collected $kw")
        _  <- adder(kw)
        _  <- count.decr
      } yield ()

      private def safeRetrieve(
        configItem: KeywordConfigurationItem
      ): F[CollectedKeyword] =
        retriever(configItem).attempt.map {
          case Right(value) =>
            CollectedKeyword.value(configItem, value)
          case Left(e)      =>
            CollectedKeyword.error(configItem, e.getMessage())
        }
    }
}
