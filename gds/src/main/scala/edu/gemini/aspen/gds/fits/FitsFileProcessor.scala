package edu.gemini.aspen.gds.fits

import cats.effect.Async
import cats.syntax.all._
import edu.gemini.aspen.gds.configuration.{ KeywordConfiguration, KeywordConfigurationItem }
import edu.gemini.aspen.gds.keywords.CollectedKeyword
import edu.gemini.aspen.gds.model.{ GdsError, KeywordSource }
import edu.gemini.aspen.gds.syntax.all._
import edu.gemini.aspen.giapi.data.DataLabel
import java.nio.file.Paths
import java.util.logging.Logger
import edu.gemini.aspen.gds.transfer.FitsFileTransferrer

sealed trait FitsFileProcessor[F[_]] {
  def processFile(dataLabel: DataLabel, keywords: List[CollectedKeyword]): F[Unit]
}

object FitsFileProcessor {
  private val logger = Logger.getLogger(this.getClass.getName)

  // TODO: Add to configuration and implement change owner and permissions policies
  // TODO: Add config item for addition of ".fits" to data label for file name.
  val sourceDir = "../src/main/config/fits/src"
  val destDir   = "../src/main/config/fits/dest"

  def apply[F[_]](keywordConfig: KeywordConfiguration)(implicit F: Async[F]): FitsFileProcessor[F] =
    new FitsFileProcessor[F] {
      val requiredKeywords: Map[Int, List[String]] =
        keywordConfig
          .forKeywordSource(KeywordSource.Instrument)
          .items
          .groupMap(_.index.index)(_.keyword.key)

      def processFile(dataLabel: DataLabel, keywords: List[CollectedKeyword]): F[Unit] = {
        val fileName = dataLabel.getName()

        val result = for {
          _      <- logger.infoF(s"Preparing to transfer FITS file $fileName")
          source <- F.delay(Paths.get(sourceDir, fileName))
          dest   <-
            F.delay(Paths.get(destDir, fileName))
              .handleErrorWith(e => F.raiseError(GdsError(s"Error creating output path: ${e}")))
          cards  <- processKeywordsToCards(keywords)
          _      <- FitsFileTransferrer.transfer(source, dest, requiredKeywords, cards)
          _      <- logger.infoF(s"FITS file $fileName transfer completed.")
        } yield ()
        result.handleErrorWith(e =>
          logger.severeF(s"Error transferring fits file $fileName: ${e.getMessage}", e)
        )
      }

      def processKeywordsToCards(
        keywords: List[CollectedKeyword]
      ): F[Map[Int, List[FitsHeaderCard]]] =
        keywordConfig.nonInstrument.items.traverse(item => processItem(item, keywords)).map {
          otuples =>
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
