package edu.gemini.aspen.gds.api

import edu.gemini.aspen.giapi.data.{ObservationEvent, DataLabel}
import java.util.logging.Logger
import scala.collection._

/**
 * Trait for objects that can provide a set of Actors
 * that can in turn retrieve keyword values
 */
trait KeywordActorsFactory {
  var actorsConfiguration: List[GDSConfiguration] = List.empty

  /**
   * Request the factory to create and start actors required for the given observation event
   */
  def buildActors(obsEvent: ObservationEvent, dataLabel: DataLabel): immutable.List[KeywordValueActor]

  /**
   * Passes the global GDS configuration along
   */
  def configure(configuration: immutable.List[GDSConfiguration]):Unit = {
    actorsConfiguration = configuration filter {
      _.subsystem.name == getSource
    }
  }

  /**
   * Returns the source where the actors created by this factory will retrieve their data
   */
  def getSource: KeywordSource.Value = KeywordSource.NONE
}

abstract class AbstractKeywordActorsFactory extends KeywordActorsFactory {
  protected val LOG = Logger.getLogger(this.getClass.getName)
}