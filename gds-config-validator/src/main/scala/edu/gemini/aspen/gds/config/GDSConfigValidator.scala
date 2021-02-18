package edu.gemini.aspen.gds.config

import edu.gemini.aspen.gds.api.GDSConfiguration
import edu.gemini.aspen.gds.api.configuration.{Comment, GDSConfigurationParser}
import scala.collection._

class GDSConfigValidator extends GDSConfigurationParser {

}

//options: -h, -f file, -p (print)
object GDSConfigValidator {
  def main(args: Array[String]):Unit = {

    val usage = """
    Usage: ./gds-validator.sh [-h|--help] [-p|--print] [-f|--file filename]
  """
    if (args.length == 0) {
      println(usage)
    }
    val arglist = args.toList
    type OptionMap = immutable.Map[Symbol, Any]

    def nextOption(map: OptionMap, list: immutable.List[String]): OptionMap = {
      def isShortSwitch(s: String) = (s(0) == '-' && s(1) != '-')
      def isLongSwitch(s: String) = (s(0) == '-' && s(1) == '-')
      def isSwitch(s: String) = s.length() > 1 && (isShortSwitch(s) || isLongSwitch(s))
      list match {
        case Nil => map
        case "--file" :: value :: tail => {
          nextOption(map ++ immutable.Map('file -> value), tail)
        }
        case "-f" :: value :: tail => {
          nextOption(map ++ immutable.Map('file -> value), tail)
        }
        case "--help" :: tail => {
          nextOption(map ++ immutable.Map('help -> true), tail)
        }
        case "-h" :: tail => {
          nextOption(map ++ immutable.Map('help -> true), tail)
        }
        case "--print" :: tail => {
          nextOption(map ++ immutable.Map('print -> true), tail)
        }
        case "-p" :: tail => {
          nextOption(map ++ immutable.Map('print -> true), tail)
        }
        //                case string :: opt2 :: tail if isSwitch(opt2) => nextOption(map ++ Map('file -> string), list.tail)
        //                case string :: Nil => nextOption(map ++ Map('file -> string), list.tail)
        case option :: tail if isSwitch(option) => {
          println("Unknown option: " + option);
          println(usage)
          sys.exit(1)
        }
        case option :: tail if !isSwitch(option) => {
          println("Lone parameters not supported: " + option);
          println(usage)
          sys.exit(1)
        }
      }
    }
    val options = nextOption(immutable.Map(), arglist)

    if (options.getOrElse('help, false) == true) {
      println(usage)
    } else {
      options.get('file) map {
        case x: String =>
          val parser = new GDSConfigValidator()
          val result = parser.parseFileRawResult(x)
          if (result.successful) {
            println("File seems to be correct: " + x)
          } else {
            println("There seems to be a problem parsing the file: " + x)
            println(result)
          }
          if (options.getOrElse('print, false) == true) {
            val results = result.get map {
              case Some(x: GDSConfiguration) => x.formatForConfigFile
              case Some(x: Comment) => x.comment
              case Some(_) => sys.error("Should not happen")
              case None => "\n"
            }
            for (res <- results) {
              println(res)
            }
          }
        case _ =>
      }
    }
  }
}