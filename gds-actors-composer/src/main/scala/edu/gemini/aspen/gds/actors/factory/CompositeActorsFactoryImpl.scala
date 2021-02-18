package edu.gemini.aspen.gds.actors.factory

import edu.gemini.aspen.giapi.data.{DataLabel, ObservationEvent}
import edu.gemini.aspen.gds.api.configuration.GDSConfigurationService
import edu.gemini.aspen.gds.api.{AbstractKeywordActorsFactory, GDSConfiguration, KeywordActorsFactory, KeywordValueActor}

import scala.collection._

/**
 * Interface for a Composite of Actors Factory required by OSGi
 */
trait CompositeActorsFactory extends KeywordActorsFactory

/**
 * A composite Actors Factory that can listen for OSGi services registered as
 * keyword actors factories */
class CompositeActorsFactoryImpl(configService: GDSConfigurationService) extends AbstractKeywordActorsFactory with CompositeActorsFactory {
  // List of composed factories
  @volatile var factories = immutable.List[KeywordActorsFactory]()
  actorsConfiguration = configService.getConfiguration
  //this had to be added for the epics factory. Channels take time to connect, so we want them early
  configure(actorsConfiguration)

  override def configure(configuration: immutable.List[GDSConfiguration]):Unit = {
    // Configure each factory in the composite
    factories foreach {
      _.configure(configuration)
    }
  }

  override def buildActors(obsEvent: ObservationEvent, dataLabel: DataLabel): List[KeywordValueActor] = {
    // Fetch the latest configuration
    actorsConfiguration = configService.getConfiguration

    configure(actorsConfiguration)

    // Collects all actors built by each factory
    factories flatMap {
      _.buildActors(obsEvent, dataLabel)
    }
  }

  /**
   * Method called when a new KeywordActorsFactory is registered */
  def addFactory(keywordFactory: KeywordActorsFactory): Unit = {
    keywordFactory.configure(actorsConfiguration)
    factories = keywordFactory :: factories
  }

  /**
   * Method called when a KeywordActorsFactory is unregistered */
  def removeFactory(keywordFactory: KeywordActorsFactory): Unit = {
    factories = factories filterNot {
      _ == keywordFactory
    }
  }

}