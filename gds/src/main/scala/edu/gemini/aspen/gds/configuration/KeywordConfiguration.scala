package edu.gemini.aspen.gds.configuration

import edu.gemini.aspen.gds.model.KeywordSource
import edu.gemini.aspen.giapi.data.ObservationEvent

final case class KeywordConfiguration(items: List[KeywordConfigurationItem]) {
  def forKeywordSource(source: KeywordSource): KeywordConfiguration =
    copy(items = items.filter(_.keywordSource == source))

  def forEvent(event: ObservationEvent): KeywordConfiguration =
    copy(items = items.filter(_.event == event))
}
