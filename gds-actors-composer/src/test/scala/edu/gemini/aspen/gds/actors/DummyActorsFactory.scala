package edu.gemini.aspen.gds.actors

import edu.gemini.aspen.gds.api._
import edu.gemini.aspen.giapi.data.{ObservationEvent, DataLabel}
import fits.FitsKeyword
import scala.collection._

class DummyActorsFactory extends AbstractKeywordActorsFactory {
  override def buildActors(obsEvent: ObservationEvent, dataLabel: DataLabel) = {
    val dummyActor = new KeywordValueActor {
      override def collectValues: immutable.List[CollectedValue[_]] = immutable.List(CollectedValue(FitsKeyword("KEYWORD1"), "", "", 0, None))
    }
    immutable.List(dummyActor)
  }

  override def configure(configuration: List[GDSConfiguration]):Unit = {}
}

