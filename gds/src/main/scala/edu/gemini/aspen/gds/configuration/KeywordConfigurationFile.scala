package edu.gemini.aspen.gds.configuration

import cats.data.{ Chain, NonEmptyChain, ValidatedNec }
import cats.syntax.all._
import edu.gemini.aspen.gds.configuration.KeywordConfigurationItem
import edu.gemini.aspen.gds.model.KeywordSource
import edu.gemini.aspen.gds.syntax.all._
import scala.util.{ Failure, Success, Try }

/**
 * This object provides utility methods to manipulate a configuration file
 * Adapted from GDSConfigurationFile in previous (actor) version of GDS, with all
 * file writing/updating removed.
 */
object KeywordConfigurationFile {
  def loadConfiguration(
    configurationFile: String
  ): ValidatedNec[String, List[KeywordConfigurationItem]] = {
    val parser = new KeywordConfigurationParser()
    Try((parser.parseFileRawResult(configurationFile) match {
      case parser.Error(msg, next)   =>
        s"Error parsing keywords config file: '$msg' at line ${next.pos.line}, column ${next.pos.column}, offset: ${next.offset}".invalidNec
      case parser.Failure(msg, next) =>
        s"Failure parsing keywords config file: '$msg' at line ${next.pos.line}, column ${next.pos.column}, offset: ${next.offset}".invalidNec
      case parser.Success(result, _) =>
        validateConfig(result.collect { case Some(x: KeywordConfigurationItem) => x })
    })) match {
      case Failure(e)     =>
        s"Failure loading keywords config file `$configurationFile`: ${e.getMessage}".invalidNec
      case Success(value) => value
    }
  }

  def validateConfig(
    items: List[KeywordConfigurationItem]
  ): ValidatedNec[String, List[KeywordConfigurationItem]] = {
    // Non-mandatory and Constant keywords should have valid "defaults"
    val defaultErrors = items
      .filter(item => !item.isMandatory || item.keywordSource === KeywordSource.Constant)
      .foldLeft(Chain.empty[String]) { (errors, item) =>
        item.defaultValue match {
          case Left(error) =>
            errors :+ s"Keyword config item `${item.keyword.key}` has invalid default: $error"
          case Right(_)    => errors
        }
      }

    // All keywords for the same epics channel should have the same data type.
    val epicsErrors = items
      .forSource(KeywordSource.Epics)
      .groupMap(_.channel.name)(_.dataType.repr)
      .foldLeft(Chain.empty[String]) { (errors, kvp) =>
        val types = kvp._2.distinct
        if (types.length > 1)
          errors :+ s"Epics channel in keyword config `${kvp._1}` has keywords with differing datatypes: ${types
            .mkString(", ")}"
        else errors
      }

    // TODO: Should we validate duplicates? It seems Contants, Properties and Seqexec should be unique by keyword. Others by keyword and event
    NonEmptyChain
      .fromChain(defaultErrors ++ epicsErrors)
      .fold(items.validNec[String])(_.invalid)
  }
}
