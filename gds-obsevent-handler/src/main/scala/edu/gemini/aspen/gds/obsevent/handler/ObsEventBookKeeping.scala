package edu.gemini.aspen.gds.obsevent.handler

import java.util.EnumSet
import edu.gemini.aspen.giapi.data.{DataLabel, ObservationEvent}
import collection.mutable.{SynchronizedMap, HashMap}
import scala.jdk.CollectionConverters._

/**
 * Keep track of which observation events have arrived and which data collection are done
 */
class ObsEventBookKeeping {
  private val obsEventsMap = new HashMap[DataLabel, EnumSet[ObservationEvent]]() with SynchronizedMap[DataLabel, EnumSet[ObservationEvent]]
  private val repliesMap = new HashMap[DataLabel, EnumSet[ObservationEvent]]() with SynchronizedMap[DataLabel, EnumSet[ObservationEvent]]

  /**
   * Ask if all previous observation events have arrived
   */
  def previousArrived(obsEvent: ObservationEvent, dataLabel: DataLabel) = {
    EnumSet.complementOf(EnumSet.range(obsEvent, ObservationEvent.values().last)).equals(obsEventsMap.getOrElse(dataLabel, EnumSet.noneOf(classOf[ObservationEvent])))
  }

  /**
   * Ask if a given observation event has arrived
   */
  def obsArrived(obsEvent: ObservationEvent, dataLabel: DataLabel) = {
    obsEventsMap.getOrElse(dataLabel, EnumSet.noneOf(classOf[ObservationEvent])).contains(obsEvent)
  }

  /**
   * Ask if a reply for a given obs event has arrived
   */
  def replyArrived(obsEvent: ObservationEvent, dataLabel: DataLabel) = {
    repliesMap.getOrElse(dataLabel, EnumSet.noneOf(classOf[ObservationEvent])).contains(obsEvent)
  }

  /**
   * Ask if replies for data collection for all observation events have arrived
   */
  def allRepliesArrived(dataLabel: DataLabel):Boolean = {
    repliesMap.getOrElse(dataLabel, EnumSet.noneOf(classOf[ObservationEvent])).containsAll(EnumSet.allOf(classOf[ObservationEvent]) filter {_.getObservationEventName.startsWith("OBS")})
  }

  /**
   * Indicate that data collection for a given obs event is done
   */
  def addReply(obsEvent: ObservationEvent, dataLabel: DataLabel):Unit = {
    val repliesSet = repliesMap.getOrElseUpdate(dataLabel, EnumSet.noneOf(classOf[ObservationEvent]))
    repliesSet.add(obsEvent)
  }

  /**
   * Indicate that an obs event has arrived
   */
  def addObs(obsEvent: ObservationEvent, dataLabel: DataLabel):Unit = {
    val obsSet = obsEventsMap.getOrElseUpdate(dataLabel, EnumSet.noneOf(classOf[ObservationEvent]))
    obsSet.add(obsEvent)
  }

  /**
   * Delete all data for a given DataLabel
   */
  def clean(dataLabel: DataLabel):Unit = {
    repliesMap -= dataLabel
    obsEventsMap -= dataLabel
  }
}