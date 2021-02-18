package edu.gemini.aspen.gds.obsevent.handler

import java.util.logging.Logger
import edu.gemini.aspen.gds.performancemonitoring.EventLogger
import edu.gemini.aspen.giapi.data.{ObservationEvent, DataLabel}
import edu.gemini.aspen.giapi.data.ObservationEvent._

/**
 * Extension of EventLogger adding methods to log event processing timing
 * @param collectDeadline The constraints on how long an event should take to complete
 * @param LOG Implicit variable to log the results of measurements
 */
class ObservationEventLogger(val collectDeadline: Long = 5000L)(implicit LOG: Logger) extends EventLogger[DataLabel,
  ObservationEvent] {

  /**
   * Checks whether the timing for a given event and dataLabel is within constraints and writes to log */
  def checkTimeWithinLimits(obsEvent: ObservationEvent, dataLabel: DataLabel):Unit = {
    // Function with side effects
    // check if the keyword recollection was performed on time
    if (!check(dataLabel, obsEvent, collectDeadline)) {
      LOG.severe("Dataset " + dataLabel + ", Event " + obsEvent + ", didn't finish on time")
    }
  }

  /**
   * Logs the timing of an ObservationEvent of a datalabel */
  def logTiming(evt: ObservationEvent, label: DataLabel):Unit = {
    val avgTime = average(evt) map {
      x => x.toMillis
    } getOrElse {
      "unknown"
    }
    val currTime = retrieve(label, evt) map {
      x => x.toMillis
    } getOrElse {
      "unknown"
    }
    LOG.info("Average timing for event " + evt + ": " + avgTime + "[ms]")
    LOG.info("Timing for event " + evt + " DataLabel " + label + ": " + currTime + "[ms]")
  }

  /**
   * Verifies that the time constraints requested for certain events are fulfilled */
  def enforceTimeConstraints(evt: ObservationEvent, label: DataLabel):Unit = {
    evt match {
      case OBS_START_ACQ => checkTimeWithinLimits(evt, label)
      case OBS_END_ACQ => checkTimeWithinLimits(evt, label)
      case _ =>
    }
  }

}