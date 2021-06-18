package edu.gemini.aspen.gds.configuration

import cats.syntax.all._
import edu.gemini.aspen.gds.model.KeywordSource
import edu.gemini.aspen.gds.syntax.instances._
import edu.gemini.aspen.giapi.data.ObservationEvent

final case class KeywordConfiguration(items: List[KeywordConfigurationItem]) {
  def forKeywordSource(source: KeywordSource): KeywordConfiguration =
    KeywordConfiguration(items.filter(_.keywordSource === source))

  def forEvent(event: ObservationEvent): KeywordConfiguration =
    KeywordConfiguration(items.filter(_.event === event))

  def nonInstrument: KeywordConfiguration = KeywordConfiguration(
    items.filterNot(_.keywordSource === KeywordSource.Instrument)
  )
}
