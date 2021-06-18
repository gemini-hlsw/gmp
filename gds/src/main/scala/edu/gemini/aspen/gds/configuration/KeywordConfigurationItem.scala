package edu.gemini.aspen.gds.configuration

import edu.gemini.aspen.giapi.data.ObservationEvent
import edu.gemini.aspen.gds.fits._
import edu.gemini.aspen.gds.model.KeywordSource

case class Instrument(name: String)

case class HeaderIndex(index: Int) {
  require(index >= 0)
}

// A keyword marked as mandatory will be left empty and an error will be put in the log
// if the value is not found
case class Mandatory(mandatory: Boolean)

// This value will be used if not mandatory and the value is not found
case class DefaultValue(value: String)

case class Channel(name: String) {
  require(name.nonEmpty)
}

case class ArrayIndex(value: Int) {
  require(value >= 0)
}

case class FitsComment(value: Option[String])

case class Format(value: Option[String]) {
  def getAsString =
    value.getOrElse("")
}

/**
 * Encapsulates a configuration item of GDS
 */
case class KeywordConfigurationItem(
  instrument:    Instrument,
  event:         ObservationEvent,
  keyword:       FitsKeyword,
  index:         HeaderIndex,
  dataType:      FitsType,
  mandatory:     Mandatory,
  nullValue:     DefaultValue,
  keywordSource: KeywordSource,
  channel:       Channel,
  arrayIndex:    ArrayIndex,
  format:        Format,
  fitsComment:   FitsComment
) {
  def isMandatory = mandatory.mandatory

  def defaultValue: Either[String, FitsValue] = dataType match {
    // Anything can be converted to a string but we remove start and end quotes
    case FitsType.StringType =>
      FitsValue.StringValue.parse(
        nullValue.value.replaceAll("""(^')|(^")|('$)|("$)""", "")
      )
    case _                   => FitsValue.parse(dataType, nullValue.value)
  }
}
