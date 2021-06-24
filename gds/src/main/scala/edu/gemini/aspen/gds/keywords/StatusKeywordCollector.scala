package edu.gemini.aspen.gds.keywords

import cats.effect.kernel.{ Async, RefSource }
import cats.syntax.all._
import edu.gemini.aspen.gds.configuration.KeywordConfigurationItem
import edu.gemini.aspen.gds.fits.FitsValue
import edu.gemini.aspen.gds.model.{ GdsError, KeywordSource }
import edu.gemini.aspen.giapi.status.StatusDatabaseService

object StatusKeywordCollector {
  def apply[F[_]](
    statusDbRef: RefSource[F, Option[StatusDatabaseService]],
    config:      List[KeywordConfigurationItem]
  )(implicit
    F:           Async[F]
  ) = {
    def retriever(item: KeywordConfigurationItem): F[FitsValue] =
      statusDbRef.get.flatMap {
        case None           =>
          F.raiseError(GdsError("StatusDatabaseService not available."))
        case Some(statusDb) => getStatus(statusDb, item)
      }

    def getStatus(statusDb: StatusDatabaseService, item: KeywordConfigurationItem): F[FitsValue] =
      // Note: Previous GDS reported the time it takes to get the status item via Log.fine
      F.blocking(Option(statusDb.getStatusItem[Any](item.channel.name)))
        .flatMap {
          case None     =>
            F.raiseError(GdsError("No value in StatusDatabaseService (returned null)"))
          case Some(si) => FitsValue.fromAny(item.dataType, si.getValue)
        }

    KeywordCollector(KeywordSource.Status, config, retriever)
  }
}
