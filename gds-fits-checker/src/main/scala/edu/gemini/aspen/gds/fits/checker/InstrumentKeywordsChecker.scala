package edu.gemini.aspen.gds.fits.checker

import java.io.File
import java.util.logging.Logger

import edu.gemini.aspen.gds.api.configuration.GDSConfigurationService
import edu.gemini.aspen.gds.api.fits.FitsKeyword
import edu.gemini.aspen.gds.api.{GDSConfiguration, KeywordSource}
import edu.gemini.aspen.gds.fits.FitsReader
import edu.gemini.aspen.gds.observationstate.ObservationStateRegistrar
import edu.gemini.aspen.giapi.data.{DataLabel, ObservationEvent, ObservationEventHandler}
import edu.gemini.aspen.gmp.services.PropertyHolder

import scala.actors.Actor._

/**
 * Factory class that instead of adding keywords to a file verifies those already on the file */
class InstrumentKeywordsChecker(configService: GDSConfigurationService, obsState: ObservationStateRegistrar, propertyHolder: PropertyHolder) extends ObservationEventHandler {
  protected val LOG: Logger = Logger.getLogger(this.getClass.getName)

  override def onObservationEvent(event: ObservationEvent, dataLabel: DataLabel):Unit = {
    event match {
      case ObservationEvent.OBS_END_DSET_WRITE => actor {
        checkMissing(dataLabel, configService.getConfiguration)
      }
      case _ =>
    }
  }

  private[checker] def checkMissing(label: DataLabel, config: List[GDSConfiguration]):Unit = {
    val file = new File(propertyHolder.getProperty("DHS_SCIENCE_DATA_PATH"), label.toString)

    LOG.info("Verifying original keywords of " + file)
    val readerOpt: Option[FitsReader] = try {
      Some(new FitsReader(file))
    } catch {
      case _:Exception => None
    }

    val configKeywords: Map[Int, Set[GDSConfiguration]] = config.filter{
      _.subsystem.name == KeywordSource.INSTRUMENT
    }.toSet.groupBy(_.index.index)

    configKeywords foreach {
      case (index: Int, value: Set[_]) => {
        val keysInFile = readerOpt.map(_.keys(index)).getOrElse(List.empty)
        val keysInConfig = value.map(_.keyword)
        val missingKeywords:Traversable[FitsKeyword] = keysInConfig &~ keysInFile.toSet

        if (missingKeywords.nonEmpty) {
          obsState.registerMissingKeyword(label, missingKeywords)
        }
      }
    }
  }

}