package edu.gemini.aspen.gds.obsevent.handler

import edu.gemini.aspen.giapi.data.ObservationEvent._
import edu.gemini.aspen.giapi.data.{ObservationEvent, DataLabel, ObservationEventHandler}
import org.apache.felix.ipojo.annotations.{Requires, Provides, Instantiate, Component}
import edu.gemini.aspen.gds.fits.FitsUpdater
import edu.gemini.aspen.gds.keywords.database.{Retrieve, Clean, KeywordsDatabase}
import java.io.File
import edu.gemini.fits.Header
import java.util.logging.Logger
import edu.gemini.aspen.gds.actors.factory.CompositeActorsFactory
import edu.gemini.aspen.gds.actors._
import actors.Reactor

/**
 * Simple Observation Event Handler that creates a KeywordSetComposer and launches the
 * keyword values acquisition process
 */
@Component
@Instantiate
@Provides(specifications = Array(classOf[ObservationEventHandler]))
class GDSObseventHandler(@Requires actorsFactory: CompositeActorsFactory, @Requires keywordsDatabase: KeywordsDatabase) extends ObservationEventHandler {
  private val LOG = Logger.getLogger(classOf[GDSObseventHandler].getName)

  //todo: private[handler] is just for testing. Need to find a better way to test this class
  private[handler] val replyHandler = new ReplyHandler(actorsFactory, keywordsDatabase)

  def onObservationEvent(event: ObservationEvent, dataLabel: DataLabel) {
    event match {
      case OBS_PREP => replyHandler ! PrepareObservation(dataLabel)
      case OBS_START_ACQ => replyHandler ! StartAcquisition(dataLabel)
      case OBS_END_ACQ => replyHandler ! EndAcquisition(dataLabel)
      case OBS_START_READOUT =>
      case OBS_END_READOUT =>
      case OBS_START_DSET_WRITE =>
      case OBS_END_DSET_WRITE => replyHandler ! EndWrite(dataLabel)
      case e: ObservationEvent => LOG.info("Non handled observation event: " + e)
    }
  }

}

class ReplyHandler(actorsFactory: CompositeActorsFactory, keywordsDatabase: KeywordsDatabase) extends Reactor[AcquisitionRequestReply] {
  start()

  def act() {
    loop {
      react {
        case PrepareObservation(dataLabel) => prepareObservation(dataLabel)
        case PrepareObservationReply(dataLabel) => prepareObservationReply(dataLabel)
        case StartAcquisition(dataLabel) => startAcquisition(dataLabel)
        case StartAcquisitionReply(dataLabel) => startAcquisitionReply(dataLabel)
        case EndAcquisition(dataLabel) => endAcquisition(dataLabel)
        case EndAcquisitionReply(dataLabel) => endAcquisitionReply(dataLabel)
        case EndWrite(dataLabel) => endWrite(dataLabel)
        case x: Any => throw new RuntimeException("Argument not known " + x)
      }
    }
  }

  //set of observations that have completed the OBS_PREP header collection
  private var prepared: Set[DataLabel] = Set[DataLabel]()

  //set of observations that have completed the OBS_START_ACQ header collection
  private var started: Set[DataLabel] = Set[DataLabel]()

  //set of observations that have completed the OBS_END_ACQ header collection
  private var ended: Set[DataLabel] = Set[DataLabel]()

  private def prepareObservation(dataLabel: DataLabel) {
    new KeywordSetComposer(actorsFactory, keywordsDatabase) ! PrepareObservation(dataLabel)
  }

  private def prepareObservationReply(dataLabel: DataLabel) {
    prepared += dataLabel
  }

  private def startAcquisition(dataLabel: DataLabel) {
    if (prepared.contains(dataLabel)) {
      prepared -= dataLabel

      new KeywordSetComposer(actorsFactory, keywordsDatabase) ! StartAcquisition(dataLabel)
    } else {
      throw new RuntimeException("Dataset " + dataLabel + " started acquisition but never preped")
    }
  }

  private def startAcquisitionReply(dataLabel: DataLabel) {
    started += dataLabel
  }

  private def endAcquisition(dataLabel: DataLabel) {
    if (started.contains(dataLabel)) {
      started -= dataLabel

      new KeywordSetComposer(actorsFactory, keywordsDatabase) ! EndAcquisition(dataLabel)
    } else {
      throw new RuntimeException("Dataset " + dataLabel + " ended acquisition but never started it")
    }
  }

  private def endAcquisitionReply(dataLabel: DataLabel) {
    ended += dataLabel
  }

  private def endWrite(dataLabel: DataLabel) {
    if (ended.contains(dataLabel)) {
      ended -= dataLabel
      updateFITSFile(dataLabel)
      keywordsDatabase ! Clean(dataLabel)
    } else {
      throw new RuntimeException("Dataset " + dataLabel + " ended writing dataset but never ended acquisition")
    }
  }

  private def updateFITSFile(dataLabel: DataLabel): Unit = {
    val list = (keywordsDatabase !? Retrieve(dataLabel)).asInstanceOf[Option[List[Header]]]
    list match {
      case Some(headersList) => new FitsUpdater(new File("/tmp"), dataLabel, headersList).updateFitsHeaders
      case None =>
    }
  }

}