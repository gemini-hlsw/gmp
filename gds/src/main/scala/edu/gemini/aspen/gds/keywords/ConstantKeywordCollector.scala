package edu.gemini.aspen.gds.keywords

import cats.effect.Async
import cats.syntax.all._
import edu.gemini.aspen.gds.configuration.{ KeywordConfiguration, KeywordConfigurationItem }
import edu.gemini.aspen.gds.fits.FitsValue
import edu.gemini.aspen.gds.model.{ GdsError, KeywordSource }

object ConstantKeywordCollector {
  def apply[F[_]](config: KeywordConfiguration)(implicit F: Async[F]): KeywordCollector[F] = {
    def retriever(item: KeywordConfigurationItem): F[FitsValue] =
      item.defaultValue match {
        case Left(e)  => F.raiseError(GdsError(e))
        case Right(v) => v.pure
      }

    KeywordCollector(KeywordSource.Constant, config, retriever)
  }
}
