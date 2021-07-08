package edu.gemini.aspen.gds.keywords

import cats.ApplicativeError
import cats.effect.Async
import cats.syntax.all._
import edu.gemini.aspen.gds.configuration.KeywordConfigurationItem
import edu.gemini.aspen.gds.fits.FitsValue
import edu.gemini.aspen.gds.model.{ GdsError, KeywordSource }

object ConstantKeywordCollector {
  def apply[F[_]](config: List[KeywordConfigurationItem])(implicit
    F:                    Async[F]
  ): KeywordCollector[F] =
    KeywordCollector(KeywordSource.Constant, config, retriever[F])

  private def retriever[G[_]](
    item:       KeywordConfigurationItem
  )(implicit G: ApplicativeError[G, Throwable]): G[FitsValue] =
    item.defaultValue match {
      case Left(e)  => G.raiseError(GdsError(e))
      case Right(v) => v.pure
    }
}
