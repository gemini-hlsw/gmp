package edu.gemini.aspen.gds.keywordssets.configuration

import util.parsing.combinator.RegexParsers
import io.Source
import edu.gemini.aspen.giapi.data.FitsKeyword

case class Comment(comment: String)

case class Instrument(name: String)

case class GDSEvent(name: String)

case class HeaderIndex(index: Int)

case class DataType(name: String)

case class Mandatory(value: Boolean)

case class NullValue(value: String)

case class Subsystem(name: String)

case class Channel(name: String)

case class ArrayIndex(value: String)

case class FitsComment(value: String)

case class Space(length: Int)

case class GDSConfiguration(instrument: Instrument, event: GDSEvent, keyword: FitsKeyword, index:HeaderIndex, dataType: DataType, mandatory: Mandatory, nullValue: NullValue, subsystem: Subsystem, channel: Channel, arrayIndex: ArrayIndex, fitsComment:FitsComment)

class GDSConfigurationParser extends RegexParsers {
    override val skipWhitespace = false

    def lines = rep(line) <~ EOF

    def line = (comment | configuration | CRLF)

    def configuration = (spaces ~ instrument
            ~ spaces ~ observationEvent
            ~ spaces ~ keyword
            ~ spaces ~ headerIndex
            ~ spaces ~ datatype
            ~ spaces ~ mandatory
            ~ spaces ~ nullValue
            ~ spaces ~ subsystem
            ~ spaces ~ channelName
            ~ spaces ~ arrayIndex
            ~ spaces ~ fitscomment) ^^ {
        case s1~ instrument
            ~ s2 ~ observationEvent
            ~ s3 ~ keyword
            ~ s4 ~ headerIndex
            ~ s5 ~ dataType
            ~ s6 ~ mandatory
            ~ s7 ~ nullValue
            ~ s8 ~ subsystem
            ~ s9 ~ channelName
            ~ s10 ~ arrayIndex
            ~ s11 ~ fitsComment => GDSConfiguration(instrument, observationEvent, keyword, headerIndex, dataType, mandatory, nullValue, subsystem, channelName, arrayIndex, fitsComment)
    }

    def instrument = """\w*""".r ^^ {
        x => Instrument(x)
    }

    def observationEvent = """[\p{Upper}_]*""".r ^^ {
        x => GDSEvent(x)
    }

    def keyword = """[\p{Upper}\d]{1,8}""".r ^^ {
        x => new FitsKeyword(x)
    }

    def headerIndex = """\d*""".r ^^ {
        x => HeaderIndex(x.toInt)
    }

    def datatype = "DOUBLE" ^^ {
        x => DataType(x)
    }

    def mandatory = """[tTfF]""".r ^^ {
        case "F" => Mandatory(false)
        case "f" => Mandatory(false)
        case "T" => Mandatory(true)
        case "t" => Mandatory(true)
    }

    def nullValue = """\w*""".r ^^ {
        x => NullValue(x)
    }

    def subsystem = """\w*""".r ^^ {
        x => Subsystem(x)
    }

    def channelName = """[:\w]*""".r ^^ {
        x => Channel(x)
    }

    def arrayIndex = """\w*""".r ^^ {
        x => ArrayIndex(x)
    }

    def comment = """#.*""".r ^^ {
        x => Comment(x)
    }

    def fitscomment = "\"" ~> internalComment <~ "\"" ^^ {
        x => FitsComment(x)
    }

    def internalComment = """[^"]*""".r

    def spaces = opt(whitespace) ^^ {
        case Some(spaces) => Space(spaces.length)
        case None => Space(0)
    }

    def whitespace = """[ \t]*""".r

    def CRLF = "\r\n" | "\n"

    def EOF = "\\z".r

    def parseFile(fileName: String) = {
        val file = Source.fromFile(fileName, "UTF8")
        parseAll(lines, file.bufferedReader).get
    }

    def parseText(text: String) = {
        parseAll(lines, text)
    }
}

object GDSConfigurationParser {
    def main(args: Array[String]) {
        val parser = new GDSConfigurationParser()
        parser.parseFile("src/main/resources/gds-keywords.conf")
    }
}