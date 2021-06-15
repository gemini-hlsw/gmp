package edu.gemini.aspen.gds.configuration

import edu.gemini.aspen.gds.configuration.KeywordConfigurationItem
import util.parsing.input.Position

/**
 * This object provides utility methods to manipulate a configuration file
 * Adapted from GDSConfigurationFile in previous (actor) version of GDS, with all
 * file writing/updating removed.
 */
object KeywordConfigurationFile {
  // TODO: This assumes no parsing errors and returns an empty list if there is. There was a
  // gds-config-validator project that I guess was used for validation. I think we should do
  // something to force validation, even if it is an exception. Successfully starting with no
  // warning seems inadvisable, but that may be by design?
  // We should also, somewhere, make sure that all the types for an epics array are the same.
  def loadConfiguration(configurationFile: String): KeywordConfiguration = {
    var results =
      new KeywordConfigurationParser().parseFileRawResult(configurationFile).getOrElse(Nil)

    //if last line is empty, remove it
    if (!results.isEmpty && results.last.isEmpty) {
      results = results.reverse.tail.reverse
    }

    val items = results.collect { case Some(x: KeywordConfigurationItem) => x }
    KeywordConfiguration(items)
  }

  def hasError(configurationFile: String): Boolean =
    !new KeywordConfigurationParser().parseFileRawResult(configurationFile).successful

  def errors(configurationFile: String): Option[(String, Int, Position)] = {
    val parser = new KeywordConfigurationParser()
    parser.parseFileRawResult(configurationFile) match {
      case parser.Failure(msg, next) => Some((msg, next.offset, next.pos))
      case _                         => None
    }
  }
}
