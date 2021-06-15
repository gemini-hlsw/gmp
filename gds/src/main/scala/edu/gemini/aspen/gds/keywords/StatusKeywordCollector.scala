package edu.gemini.aspen.gds.keywords

import cats.effect.{ Async, Ref }
import cats.syntax.all._
import edu.gemini.aspen.gds.configuration.{ KeywordConfiguration, KeywordConfigurationItem }
import edu.gemini.aspen.gds.fits.FitsValue
import edu.gemini.aspen.gds.model.KeywordSource
import edu.gemini.aspen.giapi.status.StatusDatabaseService

object StatusKeywordCollector {
  def apply[F[_]](statusDbRef: Ref[F, Option[StatusDatabaseService]], config: KeywordConfiguration)(
    implicit F:                Async[F]
  ) = {
    def retriever(item: KeywordConfigurationItem): F[FitsValue] =
      statusDbRef.get.flatMap {
        case None           =>
          F.raiseError(new Exception("StatusDatabaseService not available."))
        case Some(statusDb) => getStatus(statusDb, item)
      }

    def getStatus(statusDb: StatusDatabaseService, item: KeywordConfigurationItem): F[FitsValue] =
      // Note: Previous GDS reported the time it takes to get the status item via Log.fine
      F.blocking(Option(statusDb.getStatusItem[Any](item.channel.name)))
        .flatMap {
          case None     =>
            F.raiseError(new Exception("No value in StatusDatabaseService (returned null)"))
          case Some(si) => FitsValue.fromAny(item.dataType, si.getValue)
        }

    KeywordCollector(KeywordSource.Status, config, retriever)
  }
}
