package edu.gemini.aspen.gds.obsevent.handler

import java.io.FileNotFoundException
import java.util
import java.util.logging.{Level, Logger}

import edu.gemini.aspen.gds.actors._
import edu.gemini.aspen.gds.actors.factory.CompositeActorsFactory
import edu.gemini.aspen.gds.api._
import edu.gemini.aspen.gds.keywords.database.{Clean, KeywordsDatabase, Retrieve}
import edu.gemini.aspen.giapi.data.ObservationEvent._
import edu.gemini.aspen.giapi.data.{DataLabel, ObservationEvent}
import edu.gemini.aspen.gmp.services.PropertyHolder
import org.osgi.service.event.{Event, EventAdmin}

import scala.actors.Actor
import scala.actors.Actor.actor

class ReplyHandler(actorsFactory: CompositeActorsFactory,
  keywordsDatabase: KeywordsDatabase,
  postProcessingPolicy: PostProcessingPolicy,
  propertyHolder: PropertyHolder,
  publisher: EventAdmin) extends Actor {
  private implicit val LOG: Logger = Logger.getLogger(this.getClass.getName)
  private val eventLogger = new ObservationEventLogger
  private val bookKeeper = new ObsEventBookKeeping
  private val fileProcessor = new FitsFileProcessor(propertyHolder)
  private val obsTransactions = new ObservationTransactionsStore()

  start()

  def sendData(not: GDSNotification): Unit = {
    val properties: util.HashMap[String, GDSNotification] = new java.util.HashMap()
    properties.put(GDSNotification.GDSNotificationKey, not)
    val event = new Event(GDSNotification.GDSEventsTopic, properties)
    publisher.postEvent(event)
  }

  def act():Unit = {
    loop {
      react {
        case AcquisitionRequest(obsEvent, dataLabel) => acqRequest(obsEvent, dataLabel)
        case AcquisitionRequestReply(obsEvent, dataLabel) => acqRequestReply(obsEvent, dataLabel)
        case x: Any => throw new RuntimeException("Argument not known " + x)
      }
    }
  }

  private def acqRequest(obsEvent: ObservationEvent, dataLabel: DataLabel):Unit = {
    LOG.info("ObservationEvent " + obsEvent + " for " + dataLabel)
    obsEvent match {
      case OBS_PREP =>
        eventLogger.addEventSet(dataLabel)
        sendData(GDSStartObservation(dataLabel))
      case EXT_START_OBS =>
        // This indicates that the observation was started by the seqexec using a "transaction" of sorts
        obsTransactions.startTransaction(dataLabel)
      case _ =>
    }

    //check that all previous obsevents have arrived
    if (!bookKeeper.previousArrived(obsEvent, dataLabel)) {
      LOG.warning("Received observation event " + obsEvent + " for datalabel " + dataLabel + " out of order")
    }
    eventLogger.start(dataLabel, obsEvent)
    bookKeeper.addObs(obsEvent, dataLabel)

    new KeywordSetComposer(actorsFactory, keywordsDatabase) ! AcquisitionRequest(obsEvent, dataLabel)
  }

  def writeFinalFile(dataLabel: DataLabel, obsEvent: ObservationEvent):Unit = {

    def retry(retries: Int, sleep: Long):Unit = {
      Thread.sleep(sleep)
      if (bookKeeper.allRepliesArrived(dataLabel)) {
        //OK, can continue
        completeFile(obsEvent, dataLabel)
      } else if (retries > 1) {
        //retry
        LOG.warning("Still haven't completed data collection on other observation events. Wait and retry.")
        retry(retries - 1, sleep)
      } else {
        //we failed, wrap things up anyway
        completeFile(obsEvent, dataLabel)
        LOG.severe("Retry limit for " + obsEvent + " for dataset " + dataLabel + " reached. FITS file is probably incomplete.")
      }
    }

    //if all obsevents replies have arrived
    if (bookKeeper.allRepliesArrived(dataLabel)) {
      completeFile(obsEvent, dataLabel)
    } else {
      LOG.warning("Received data collection reply for " + obsEvent + " for dataset " + dataLabel + ", but data collection on other observation events hasn't finished. Wait and retry.")
      //sleep one second and retry (5 times)
      actor {
        retry(5, 1000)
      }
    }
  }

  private def acqRequestReply(obsEvent: ObservationEvent, dataLabel: DataLabel):Unit = {
    //check that this obsevent collection reply hasn't already arrived but that the obsevent has.
    if (bookKeeper.replyArrived(obsEvent, dataLabel)) {
      LOG.severe("Received data collection reply for observation event " + obsEvent + " for datalabel " + dataLabel + " twice")
      return
    }
    if (!bookKeeper.obsArrived(obsEvent, dataLabel)) {
      LOG.severe("Received data collection reply for observation event " + obsEvent + " for datalabel " + dataLabel + ", but never received the observation event")
      return
    }
    bookKeeper.addReply(obsEvent, dataLabel)

    obsEvent match {
      case OBS_END_DSET_WRITE if !obsTransactions.hasTransaction(dataLabel) => writeFinalFile(dataLabel, obsEvent)
      case EXT_END_OBS if obsTransactions.hasTransaction(dataLabel) => writeFinalFile(dataLabel, obsEvent)
      case _ => endAcqRequestReply(obsEvent, dataLabel)
    }

  }

  private def endAcqRequestReply(obsEvent: ObservationEvent, dataLabel: DataLabel):Unit = {
    eventLogger.end(dataLabel, obsEvent)
    eventLogger.enforceTimeConstraints(obsEvent, dataLabel)

    obsEvent match {
      case OBS_END_DSET_WRITE =>
        //log timing stats for this datalabel
        for (evt <- ObservationEvent.values()) {
          eventLogger.logTiming(evt, dataLabel)
        }
      case _ =>
    }
  }

  private def completeFile(obsEvent: ObservationEvent, dataLabel: DataLabel):Unit = {
    bookKeeper.clean(dataLabel)

    try {
      val list = (keywordsDatabase !? Retrieve(dataLabel)).asInstanceOf[List[CollectedValue[_]]]
      val processedList = postProcessingPolicy.applyPolicy(dataLabel, list)
      val cleanedList = processedList.filterNot(_.isError)

      fileProcessor.updateFITSFile(dataLabel, cleanedList) match {
        case Right(FitsWriteResult(message, writeTime, srcFile, destFile)) =>
          LOG.info(message)
          sendData(GDSObservationTimes(dataLabel, eventLogger.retrieve(dataLabel)))
          sendData(GDSEndObservation(dataLabel, writeTime, processedList))

          postProcessingPolicy.fileReady(srcFile, destFile)
        case Left(errorMsg: String) =>
          LOG.severe(errorMsg)
          sendData(GDSObservationError(dataLabel, errorMsg))
        case a => println(a)
      }
    } catch {
      case ex: FileNotFoundException => LOG.log(Level.SEVERE, ex.getMessage)
    }

    endAcqRequestReply(obsEvent, dataLabel)
    keywordsDatabase ! Clean(dataLabel)
  }

}