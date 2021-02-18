package edu.gemini.aspen.gds.actors

import edu.gemini.aspen.gds.api._
import edu.gemini.aspen.giapi.data.{ObservationEvent, DataLabel}
import scala.actors._
import scala.collection._
import edu.gemini.aspen.gds.keywords.database.{StoreList, KeywordsDatabase}
import java.util.logging.{Level, Logger}
import com.google.common.base.Stopwatch
import java.util.concurrent.TimeUnit

/**
 * Message to indicate that FITS header data collection should begin
 */
case class AcquisitionRequest(obsEvent: ObservationEvent, dataLabel: DataLabel)

/**
 * Message to indicate the data collection w  as completed
 * It is sent in reply to an AcquisitionRequest message
 */
case class AcquisitionRequestReply(obsEvent: ObservationEvent, dataLabel: DataLabel)

/**
 * An actor that composes the items required to complete an observation using a set of actors
 */
class KeywordSetComposer(actorsFactory: KeywordActorsFactory, keywordsDatabase: KeywordsDatabase) extends Actor {
  private val LOG = Logger.getLogger(this.getClass.getName)

  // Start automatically
  start()

  def act():Unit = {
    react {
      case AcquisitionRequest(obsEvent, dataLabel) => doCollection(sender, obsEvent, dataLabel)
      case _ => sys.error("Unknown request type ")
    }
  }

  private def doCollection(sender: OutputChannel[Any], obsEvent: ObservationEvent, dataLabel: DataLabel):Unit = {
    LOG.info(s"Starting keyword collection on dataset $dataLabel for event ${obsEvent.name}")

    val s = System.currentTimeMillis()
    val dataFutures = requestCollection(obsEvent, dataLabel, actorsFactory.buildActors)

    waitForDataAndReply(dataLabel, dataFutures) {
      LOG.info(s"Finished keyword collection: ${dataFutures.size} collecting actors for $obsEvent completed in ${System.currentTimeMillis() - s} [ms]")
      // Reply to the original sender
      sender ! AcquisitionRequestReply(obsEvent, dataLabel)
    }
  }

  private def requestCollection(obsEvent: ObservationEvent, dataLabel: DataLabel, actorsBuilder: (ObservationEvent, DataLabel) => immutable.List[Actor]): immutable.List[Future[Any]] = {
    // Get the actors from the factory
    val actors = measureDuration("Building actors for event:" + obsEvent) {
      try {
        actorsBuilder(obsEvent, dataLabel)
      } catch {
        case e:Exception =>
          LOG.log(Level.SEVERE, "Caught an exception from an actor factory", e)
          Nil
      }
    }

    // Start collecting
    val dataFutures = measureDuration("Sending collection request for %s to %d actors".format(obsEvent, actors.size)) {
      for (dataActor <- actors) yield
        dataActor !! Collect
    }

    dataFutures
  }

  private def waitForDataAndReply(dataLabel: DataLabel, dataFutures: immutable.List[Future[Any]])(postAction: => Unit):Unit = {
    measureDuration("Waiting for " + dataFutures.size + " data items ") {
      // Wait for response
      val v = Futures.awaitAll(5000, dataFutures: _*).collect {
        case Some(l: List[_]) => l collect {
          case x: CollectedValue[_] => x
        }
        case None =>
          LOG.warning("Missing return of collecting data")
          Nil
      }

      keywordsDatabase ! StoreList(dataLabel, v.flatten)
    }

    postAction
  }

  /**
   * Interceptor method to measure how long a given action takes */
  private def measureDuration[T](msg: String)(action: => T): T = {
    val s = Stopwatch.createStarted()
    val result = action
    val duration = s.stop().elapsed(TimeUnit.MILLISECONDS)
    LOG.fine(s"Message $msg processing took $duration [ms]")
    result
  }

}