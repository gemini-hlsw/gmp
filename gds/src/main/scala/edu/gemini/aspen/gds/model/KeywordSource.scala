package edu.gemini.aspen.gds.model

import edu.gemini.aspen.gds.util.Enumerated

sealed abstract class KeywordSource(val name: String) extends Product with Serializable

// format: off
object KeywordSource {
  case object SeqExec    extends KeywordSource("SEQEXEC")     // sent previously by seqexec
  case object Epics      extends KeywordSource("EPICS")       // to be collected from EPICS channels
  case object Status     extends KeywordSource("STATUS")      // sent previously by the instrument as StatusItem
  case object Constant   extends KeywordSource("CONSTANT")    // constant value read from gds config file
  case object Property   extends KeywordSource("PROPERTY")    // value read out of a system or java property
  case object Instrument extends KeywordSource( "INSTRUMENT") // value already written in the FITS file by the Instrument. GDS will check that these items are in place.
  // format: on

  val all: List[KeywordSource] =
    List(SeqExec, Epics, Status, Constant, Property, Instrument)

  val validationRegex: String = all.map(_.name).mkString("|")

  implicit val KeywordSourceEnumerated: Enumerated[KeywordSource] = new Enumerated[KeywordSource] {
    def all = KeywordSource.all
    def tag(a:                      KeywordSource): String = a.name
    override def unsafeFromTag(tag: String): KeywordSource =
      fromTag(tag).getOrElse(sys.error(s"Invalid KeywordSource: $tag"))
  }
}
