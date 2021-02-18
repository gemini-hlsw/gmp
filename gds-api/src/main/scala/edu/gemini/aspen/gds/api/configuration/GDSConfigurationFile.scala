package edu.gemini.aspen.gds.api.configuration

import edu.gemini.aspen.gds.api.GDSConfiguration
import java.io.{FileWriter, BufferedWriter, File}
import edu.gemini.aspen.gds.api.Predef._
import util.parsing.input.Position

/**
 * This object provides utility methods to manipulate a configuration file
 */
object GDSConfigurationFile {
  def getConfiguration(configurationFile: String): List[GDSConfiguration] = getConfiguration(getFullConfiguration(configurationFile))

  def hasError(configurationFile: String): Boolean = !new GDSConfigurationParser().parseFileRawResult(configurationFile).successful

  def errors(configurationFile: String): Option[(String, Int, Position)] = {
    val parser = new GDSConfigurationParser()
    parser.parseFileRawResult(configurationFile) match {
      case parser.Failure(msg, next) => Some(msg, next.offset, next.pos)
      case _ => None
    }
  }

  def getConfiguration(contents: List[ConfigItem[_]]): List[GDSConfiguration] = contents filter {
      _.isInstanceOf[ConfigItem[_]]
    } map {
      _.value
    } collect {
      case c:GDSConfiguration => c
    }

  def getFullConfiguration(configurationFile: String): List[ConfigItem[_]] = {
    var results = new GDSConfigurationParser().parseFileRawResult(configurationFile).getOrElse(Nil)

    //if last line is empty, remove it
    if (!results.isEmpty && results.last.isEmpty) {
      results = results.reverse.tail.reverse
    }

    results map {
      case Some(x: GDSConfiguration) => new ConfigItem(x)
      case Some(x: Comment) => new ConfigItem(x)
      case Some(x) => sys.error("Should not happen")
      case None => new ConfigItem(new BlankLine())
    }
  }

  def saveConfiguration(configurationFile: String, contents: List[ConfigItem[_]]):Unit = {
    val newFile = new File(configurationFile)
    use(new BufferedWriter(new FileWriter(newFile))) {
      writer: BufferedWriter => for (configLine <- contents) {
        Some(configLine) map {
          case x => writer.write(x._type.ConfigTypeToString(x))
        }
        writer.newLine()
      }
    }

  }

}