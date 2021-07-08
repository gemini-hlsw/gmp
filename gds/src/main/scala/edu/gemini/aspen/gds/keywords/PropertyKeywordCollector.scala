package edu.gemini.aspen.gds.keywords

import cats.effect.Async
import cats.syntax.all._
import edu.gemini.aspen.gds.configuration.KeywordConfigurationItem
import edu.gemini.aspen.gds.fits.FitsValue
import edu.gemini.aspen.gds.model.{ GdsError, KeywordSource }

object PropertyKeywordCollector {
  def apply[F[_]](
    config:     List[KeywordConfigurationItem]
  )(implicit F: Async[F]): KeywordCollector[F] = {
    def retriever(item: KeywordConfigurationItem): F[FitsValue] =
      F.delay(Option(System.getProperty(item.channel.name))).flatMap {
        case None    => F.raiseError(GdsError(s"Property ${item.channel.name} not found."))
        case Some(p) =>
          FitsValue.parse(item.dataType, p) match {
            case Left(e)  => F.raiseError(GdsError(e))
            case Right(v) => v.pure
          }
      }

    KeywordCollector(KeywordSource.Property, config, retriever)
  }
}
