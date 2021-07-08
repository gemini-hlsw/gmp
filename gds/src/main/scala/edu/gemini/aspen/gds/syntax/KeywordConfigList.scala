package edu.gemini.aspen.gds.syntax

import cats.syntax.all._
import edu.gemini.aspen.gds.configuration.KeywordConfigurationItem
import edu.gemini.aspen.gds.model.KeywordSource
import edu.gemini.aspen.giapi.data.ObservationEvent

import instances._

class KeywordConfigListOps(val list: List[KeywordConfigurationItem]) extends AnyVal {
  def forSource(source: KeywordSource): List[KeywordConfigurationItem] =
    list.filter(_.keywordSource === source)

  def forEvent(event: ObservationEvent): List[KeywordConfigurationItem] =
    list.filter(_.event === event)

  def nonInstrument: List[KeywordConfigurationItem] =
    list.filterNot(_.keywordSource === KeywordSource.Instrument)
}

trait ToKeywordConfigListOps {
  implicit def ToKeywordConfigListOps(list: List[KeywordConfigurationItem]) =
    new KeywordConfigListOps(list)
}
object KeywordConfigList extends ToKeywordConfigListOps
