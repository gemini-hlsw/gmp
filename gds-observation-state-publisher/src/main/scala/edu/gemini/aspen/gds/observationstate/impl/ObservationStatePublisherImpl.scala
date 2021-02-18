package edu.gemini.aspen.gds.observationstate.impl

import edu.gemini.aspen.gds.observationstate.{ObservationInfo, ObservationStateConsumer, ObservationStatePublisher}
import edu.gemini.aspen.giapi.data.DataLabel

import scala.collection.mutable.{HashSet, Set, SynchronizedSet}

/**
 * Component that publishes Observation state changes
 */
class ObservationStatePublisherImpl extends ObservationStatePublisher {
  val registeredConsumers: Set[ObservationStateConsumer] = new HashSet[ObservationStateConsumer] with SynchronizedSet[ObservationStateConsumer]

  def publishStartObservation(label: DataLabel):Unit = {
    for (consumer <- registeredConsumers) {
      consumer.receiveStartObservation(label)
    }
  }

  override def publishEndObservation(observationInfo: ObservationInfo):Unit = {
    registeredConsumers foreach {
      _.receiveEndObservation(observationInfo)
    }
  }

  override def publishObservationError(observationInfo: ObservationInfo):Unit = {
    registeredConsumers foreach {
      _.receiveObservationError(observationInfo)
    }
  }

  def bindConsumer(consumer: ObservationStateConsumer):Unit = {
    registeredConsumers += consumer
  }

  def unbindConsumer(consumer: ObservationStateConsumer):Unit = {
    registeredConsumers -= consumer
  }

}
