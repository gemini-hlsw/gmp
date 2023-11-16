package edu.gemini.aspen.gds.configuration

import edu.gemini.aspen.gds._
import edu.gemini.aspen.gds.util.Enumerated
import edu.gemini.aspen.gds.model.KeywordSource
import edu.gemini.aspen.giapi.data.ObservationEvent
import fits._
import scala.io.Source
import scala.util.parsing.combinator.RegexParsers

case class Space(length: Int)

case class Comment(comment: String) {
  override def toString: String = comment
}

class KeywordConfigurationParser extends RegexParsers {
  override val skipWhitespace = false

  def lines = rep1sep(line, whitespace ~ CRLF) <~ (whitespace ~ EOF)

  def line = opt(comment | configuration)

  def configuration = (spaces ~ instrument
    ~ spaces ~ observationEvent
    ~ spaces ~ keyword
    ~ spaces ~ headerIndex
    ~ spaces ~ datatype
    ~ spaces ~ mandatory
    ~ spaces ~ (defaultValueNonQuotes | defaultValueInQuotes)
    ~ spaces ~ keywordSource
    ~ spaces ~ channelName
    ~ spaces ~ arrayIndex
    ~ spaces ~ format
    ~ spaces ~ fitscomment) ^^ {
    case _ ~ instrument
        ~ _ ~ observationEvent
        ~ _ ~ keyword
        ~ _ ~ headerIndex
        ~ _ ~ dataType
        ~ _ ~ mandatory
        ~ _ ~ nullValue
        ~ _ ~ keywordSource
        ~ _ ~ channelName
        ~ _ ~ arrayIndex
        ~ _ ~ format
        ~ _ ~ fitsComment =>
      KeywordConfigurationItem(instrument,
                               observationEvent,
                               keyword,
                               headerIndex,
                               dataType,
                               mandatory,
                               nullValue,
                               keywordSource,
                               channelName,
                               arrayIndex,
                               format,
                               fitsComment
      )
  }

  def instrument = """\w+""".r ^^ { x =>
    Instrument(x)
  }

  def observationEvent =
    ObservationEvent.values().map(_.name).mkString("|").r ^^ ObservationEvent.valueOf

  def keyword = FitsKeyword.KeyFormat.r ^^ FitsKeyword.unsafeFromString

  def headerIndex = """\d+""".r ^^ { x =>
    HeaderIndex(x.toInt)
  }

  def datatype =
    FitsType.validationRegex.r ^^ Enumerated[FitsType].unsafeFromTag

  def mandatory = """[tTfF]""".r ^^ {
    case "F" => Mandatory(false)
    case "f" => Mandatory(false)
    case "T" => Mandatory(true)
    case "t" => Mandatory(true)
  }

  // Default value can be anything that does not contain spaces or is in quotes
  def defaultValue = defaultValueInQuotes | defaultValueNonQuotes

  def defaultValueNonQuotes = """[^'"\s]+""".r ^^ { x: String =>
    DefaultValue(x)
  }

  def defaultValueInQuotes = "\"" ~> internalComment <~ "\"" ^^ { x: String =>
    DefaultValue(x)
  }

  def keywordSource = KeywordSource.validationRegex.r ^^ Enumerated[KeywordSource].unsafeFromTag

  // Default value can be anything that does not contain a space
  def channelName = """[:\w\.\]\[]+""".r ^^ { x: String =>
    Channel(x)
  }

  def arrayIndex = """\d+""".r ^^ { x =>
    ArrayIndex(x.toInt)
  }

  def comment = spaces ~> """#.*""".r ^^ { x =>
    Comment(x.trim)
  }

  def fitscomment = "\"" ~> internalComment <~ "\"" ^^ { x =>
    if (x.isEmpty) FitsComment(None) else FitsComment(Some(x))
  }

  def format = "\"" ~> GdsConfigurationParser.internalFormat <~ "\"" ^^ { x =>
    if (x.isEmpty) Format(None) else Format(Some(x))
  }

  def internalComment = """[^"]*""".r

  def spaces = opt(whitespace) ^^ {
    case Some(spaces) => Space(spaces.length)
    case None         => Space(0)
  }

  def whitespace = """[ \t]*""".r

  def CRLF = "\r\n" | "\n"

  def EOF = "\\z".r

  def parseFileRawResult(fileName: String) = {
    val file = Source.fromFile(fileName, "UTF8")
    parseAll(lines, file.bufferedReader())
  }

  def parseText(text: String) =
    parseAll(lines, text)
}

object GdsConfigurationParser {
  // todo: improve internal format, for now we accept anything that doesn't include quotes and does include %
  def internalFormat = """([^"]*%[^"]*)|()""".r

}
