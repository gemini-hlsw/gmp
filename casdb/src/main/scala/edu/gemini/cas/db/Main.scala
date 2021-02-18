package edu.gemini.cas.db

/**
 * Starting point of the Application
 * Loans the code for options processing from gds-config-validator
 */
object Main {
  def main(args: Array[String]):Unit = {
    val usage = """Usage: ./casdb.sh [-f|--file filename]
"""
    if (args.length == 0) {
      println(usage)
    }
    val arglist = args.toList
    type OptionMap = Map[Symbol, Any]

    def nextOption(map: OptionMap, list: List[String]): OptionMap = {
      def isShortSwitch(s: String) = (s(0) == '-' && s(1) != '-')
      def isLongSwitch(s: String) = (s(0) == '-' && s(1) == '-')
      def isSwitch(s: String) = s.length() > 1 && (isShortSwitch(s) || isLongSwitch(s))
      list match {
        case Nil => map
        case "--file" :: value :: tail => {
          nextOption(map ++ Map(Symbol("file") -> value), tail)
        }
        case "-f" :: value :: tail => {
          nextOption(map ++ Map(Symbol("file") -> value), tail)
        }
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
        case _ => sys.error("Unexpected value")
      }
    }
    val options = nextOption(Map(), arglist)

    if (options.getOrElse(Symbol("help"), false) == true) {
      println(usage)
    } else {
      options.get(Symbol("file")) map {
        case x: String => new ChannelBuilder(x)
        case _ =>
      }
    }
  }
}