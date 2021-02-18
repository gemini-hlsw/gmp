package edu.gemini.aspen.gds.actors

import edu.gemini.aspen.gds.api._
import edu.gemini.aspen.giapi.data.{ObservationEvent, DataLabel}
import scala.collection._

/**
 * Dummy actors factory that uses a configuration list */
class ConfigurableActorsFactory extends AbstractKeywordActorsFactory {

  override def buildActors(obsEvent: ObservationEvent, dataLabel: DataLabel) = actorsConfiguration map {
    c => new KeywordValueActor {
      override def collectValues: immutable.List[CollectedValue[_]] = immutable.List(CollectedValue(c.keyword, "", "", 0, None))
    }
  }

  override def configure(configuration: List[GDSConfiguration]):Unit = {
    actorsConfiguration = configuration
  }
}

