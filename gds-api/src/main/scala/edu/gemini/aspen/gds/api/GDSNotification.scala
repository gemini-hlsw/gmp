package edu.gemini.aspen.gds.api

import edu.gemini.aspen.giapi.data.{ObservationEvent, DataLabel}
import org.joda.time.Duration


/**
 * Parent trait of case classes defining notifications or events produced by the GDS */
sealed trait GDSNotification {
  val dataLabel: DataLabel
}

/**
 * Notification that an observation has started
 * @param dataLabel The data label of the observation
 */
case class GDSStartObservation(dataLabel: DataLabel) extends GDSNotification

/**
 * Notification that an observation has completed
 * @param dataLabel The data label of the observation
 */
case class GDSEndObservation(dataLabel: DataLabel) extends GDSNotification

/**
 * Notification that an observation has stopped with an error
 * @param dataLabel The data label of the observation
 */
case class GDSObservationError(dataLabel: DataLabel, msg: String) extends GDSNotification

/**
 * Register how long observation stage have taken
 * @param dataLabel The data label of the observation
 * @param times List of the duration of each observation stage
 * @tparam A Type of the observation, has to be a subclass of Traversable
 */
case class GDSObservationTimes[A <: Traversable[(ObservationEvent, Option[Duration])]](dataLabel: DataLabel, times: A) extends GDSNotification