package edu.gemini.aspen.gds.observationstate.impl

import java.util.logging.Logger

import edu.gemini.aspen.gds.api._
import edu.gemini.aspen.gds.api.configuration.GDSConfigurationService
import edu.gemini.aspen.gds.observationstate.ObservationStateRegistrar
import edu.gemini.aspen.giapi.data.DataLabel

/**
 * This policy checks for errors and missing values and adds them to the ObservationStateRegistrar.
 * It should be run after EnforceMandatory policy and before ErrorsRemoved policy applied.
 */
class InspectPolicy(configService: GDSConfigurationService, obsState: ObservationStateRegistrar) extends DefaultPostProcessingPolicy {
  protected override val LOG: Logger = Logger.getLogger(this.getClass.getName)
  override val priority = 5

  override def applyPolicy(dataLabel: DataLabel, headers: List[CollectedValue[_]]): List[CollectedValue[_]] = {
    LOG.fine("Inspect headers looking for missing value")

    checkErrors(dataLabel, headers)
    checkMissing(dataLabel, headers)
    headers
  }

  private def checkErrors(label: DataLabel, headers: List[CollectedValue[_]]):Unit = {
    obsState.registerCollectionError(label, headers collect {
      case c: ErrorCollectedValue => (c.keyword, c.error)
    })
  }

  private def checkMissing(label: DataLabel, headers: List[CollectedValue[_]]):Unit = {
    val configList = configService.getConfiguration filterNot {
      _.subsystem.name == KeywordSource.INSTRUMENT //IFS keywords are not written by us, they are already in the file
    }
    obsState.registerMissingKeyword(label, configList filterNot {
      config => headers exists {
        collected => collected.keyword == config.keyword
      }
    } collect {
      case config => config.keyword
    })
  }

  override def toString: String = this.getClass.getSimpleName
}