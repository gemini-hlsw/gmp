package edu.gemini.aspen.gds.keywords

import cats.syntax.all._
import edu.gemini.aspen.gds.configuration.KeywordConfigurationItem
import edu.gemini.aspen.gds.fits._
import edu.gemini.aspen.gds.model.KeywordSource
import edu.gemini.aspen.giapi.data.ObservationEvent

sealed trait CollectedKeyword {
  val keyword: FitsKeyword
  val keywordSource: KeywordSource
  val event: Option[ObservationEvent] // is this needed? Should we have event None?
}

object CollectedKeyword {

  final case class Value(
    keyword:       FitsKeyword,
    keywordSource: KeywordSource,
    event:         Option[ObservationEvent],
    value:         FitsValue
  ) extends CollectedKeyword

  final case class Error(
    keyword:       FitsKeyword,
    keywordSource: KeywordSource,
    event:         Option[ObservationEvent],
    message:       String
  ) extends CollectedKeyword

  def value(configItem: KeywordConfigurationItem, fitsValue: FitsValue): CollectedKeyword =
    Value(configItem.keyword, configItem.keywordSource, configItem.event.some, fitsValue)

  def error(configItem: KeywordConfigurationItem, message: String): CollectedKeyword =
    Error(configItem.keyword, configItem.keywordSource, configItem.event.some, message)
}
