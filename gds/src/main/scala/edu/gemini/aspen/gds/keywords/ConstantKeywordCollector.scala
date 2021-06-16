package edu.gemini.aspen.gds.keywords

import cats.effect.Async
import cats.syntax.all._
import edu.gemini.aspen.gds.configuration.{ KeywordConfiguration, KeywordConfigurationItem }
import edu.gemini.aspen.gds.fits.FitsValue
import edu.gemini.aspen.gds.fits.FitsType.StringType
import edu.gemini.aspen.gds.model.{ GdsError, KeywordSource }

object ConstantKeywordCollector {
  def apply[F[_]](config: KeywordConfiguration)(implicit F: Async[F]): KeywordCollector[F] = {
    def retriever(item: KeywordConfigurationItem): F[FitsValue] = {
      val efv = item.dataType match {
        // Anything can be converted to a string but we remove start and end quotes
        case StringType =>
          FitsValue.StringValue.parse(
            item.nullValue.value.replaceAll("""(^')|(^")|('$)|("$)""", "")
          )
        case _          => FitsValue.parse(item.dataType, item.nullValue.value)
      }
      efv match {
        case Left(e)  => F.raiseError(GdsError(e))
        case Right(v) => v.pure
      }
    }

    KeywordCollector(KeywordSource.Constant, config, retriever)
  }
}
