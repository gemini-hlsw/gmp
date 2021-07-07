package edu.gemini.aspen.gds.fits

import cats.effect.Async
import cats.syntax.all._
import edu.gemini.aspen.gds.configuration.{ FitsConfig, KeywordConfigurationItem }
import edu.gemini.aspen.gds.keywords.CollectedKeyword
import edu.gemini.aspen.gds.model.KeywordSource
import edu.gemini.aspen.gds.syntax.all._
import edu.gemini.aspen.gds.transfer.FitsFileTransferrer
import edu.gemini.aspen.giapi.data.DataLabel
import fs2.io.file.{ Files => Fs2Files }
import java.nio.file.Path
import java.util.logging.Logger
import com.google.common.io.{ Files => GFiles }

sealed trait FitsFileProcessor[F[_]] {
  def processFile(dataLabel: DataLabel, keywords: List[CollectedKeyword]): F[Unit]
}

object FitsFileProcessor {
  private val logger = Logger.getLogger(this.getClass.getName)

  // TODO: Implement change owner and permissions and other postprocessing policies if needed

  def apply[F[_]](
    fitsConfig:     FitsConfig,
    keywordConfigs: List[KeywordConfigurationItem]
  )(implicit F:     Async[F]): FitsFileProcessor[F] =
    new FitsFileProcessor[F] {
      val requiredKeywords: Map[Int, List[String]] =
        keywordConfigs
          .forSource(KeywordSource.Instrument)
          .groupMap(_.index.index)(_.keyword.key)

      def processFile(dataLabel: DataLabel, keywords: List[CollectedKeyword]): F[Unit] = {
        val inFileName  = dataLabel.getName
        val outFileName =
          if (fitsConfig.addSuffix && !dataLabel.getName.endsWith(".fits"))
            s"${dataLabel.getName}.fits"
          else dataLabel.getName

        val result = for {
          _      <- logger.infoF(s"Preparing to transfer FITS file $outFileName")
          source <- F.delay(fitsConfig.sourceDir.resolve(inFileName))
          dest   <- safeDestinationFile(fitsConfig.destDir, outFileName)
          cards  <- processKeywordsToCards(keywords)
          _      <- FitsFileTransferrer.transfer(source, dest, requiredKeywords, cards)
          _      <- logger.infoF(s"FITS file $dest transfer completed.")
        } yield ()
        result.handleErrorWith(e =>
          logger.severeF(
            s"Error transferring fits file for observation $dataLabel: ${e.getMessage}",
            e
          )
        )
      }

      def safeDestinationFile(dir: Path, name: String): F[Path] = for {
        fullPath <- F.delay(dir.resolve(name))
        exists   <- Fs2Files[F].exists(fullPath)
        safePath <-
          if (exists)
            logger.infoF(
              s"Output file $fullPath already exists - generating new name."
            ) >> safeDestinationFile(dir, newDestinationFileName(name))
          else F.delay(fullPath)
      } yield safePath

      def newDestinationFileName(fullName: String): String = {
        val nameRegex = """(\w*)-(\d+)""".r
        val name      = GFiles.getNameWithoutExtension(fullName)
        val ext       = GFiles.getFileExtension(fullName)
        name match {
          case nameRegex(n, d) => s"$n-${d.toInt + 1}.$ext"
          case _               => s"$name-1.$ext"
        }
      }

      def processKeywordsToCards(
        keywords: List[CollectedKeyword]
      ): F[Map[Int, List[FitsHeaderCard]]] =
        keywordConfigs.nonInstrument.traverse(item => processItem(item, keywords)).map { otuples =>
          val tuples = otuples.collect { case Some(tuple) => tuple }
          tuples.groupMap(_._1)(_._2)
        }

      def processItem(
        item:     KeywordConfigurationItem,
        keywords: List[CollectedKeyword]
      ): F[Option[(Int, FitsHeaderCard)]] =
        for {
          ocard <- getHeaderCard(item, keywords)
          tuple  = ocard.map((item.index.index, _))
        } yield tuple

      def getHeaderCard(
        item:     KeywordConfigurationItem,
        keywords: List[CollectedKeyword]
      ): F[Option[FitsHeaderCard]] =
        keywords.find(ckw =>
          ckw.keyword === item.keyword && ckw.keywordSource === item.keywordSource && eventMatches(
            ckw,
            item
          )
        ) match {
          case Some(keyword) => handleCollectedValue(item, keyword)
          case None          => handleMissing(item)
        }

      def eventMatches(keyword: CollectedKeyword, item: KeywordConfigurationItem): Boolean =
        keyword.event match {
          case Some(event) => event === item.event
          case None        => true // only happens for Seqexec
        }

      def handleMissing(item: KeywordConfigurationItem): F[Option[FitsHeaderCard]] =
        if (item.isMandatory) logError(item, "Mandatory keyword was not collected") >> none.pure
        else
          item.defaultValue
            .fold(
              e =>
                logError(item,
                         s"Keyword was not collected, and default value was bad: $e"
                ) >> none.pure,
              toHeaderCard(item, _).some.pure
            )

      def handleCollectedValue(
        item:    KeywordConfigurationItem,
        keyword: CollectedKeyword
      ): F[Option[FitsHeaderCard]] =
        keyword match {
          case CollectedKeyword.Value(_, _, _, value)   =>
            toHeaderCard(item, value).some.pure
          case CollectedKeyword.Error(_, _, _, message) =>
            if (item.isMandatory)
              logError(item, s"Collection error for manadatory keyword: $message") >> none.pure
            else if (item.keywordSource === KeywordSource.Constant)
              logError(item, s"The constant value is bad: $message") >> none.pure
            else
              logWarning(item, s"Collection error, using default: $message") >>
                item.defaultValue
                  .fold(e => logError(item, s"Default value is bad: $e") >> none.pure,
                        toHeaderCard(item, _).some.pure
                  )
        }

      def toHeaderCard(
        item:  KeywordConfigurationItem,
        value: FitsValue
      ): FitsHeaderCard =
        FitsHeaderCard(item.keyword, value, item.fitsComment.value, item.format.value)

      def logWarning(item: KeywordConfigurationItem, msg: String): F[Unit] =
        logger.warningF(formatMessage(item, msg))

      def logError(item: KeywordConfigurationItem, msg: String): F[Unit] =
        logger.severeF(formatMessage(item, msg))

      def formatMessage(item: KeywordConfigurationItem, msg: String): String =
        s"Keyword: ${item.keyword.key}: $msg"
    }

}
