package edu.gemini.aspen.gds.observationstate.impl

import edu.gemini.aspen.giapi.data.{FitsKeyword, DataLabel}
import edu.gemini.aspen.gds.api.CollectionError
import edu.gemini.aspen.gds.observationstate.{ObservationStateConsumer, ObservationStatePublisher}
import org.apache.felix.ipojo.annotations._
import collection.mutable.HashSet
import collection.mutable.Set

/**
 * Component that publishes Observation state changes
 */
@Component
@Instantiate
@Provides(specifications = Array(classOf[ObservationStatePublisher]))
class ObservationStatePublisherImpl extends ObservationStatePublisher {
    val registeredConsumers: Set[ObservationStateConsumer] = HashSet[ObservationStateConsumer]()

    def publishStartObservation(label: DataLabel) {
        for (consumer <- registeredConsumers) {
            consumer.receiveStartObservation(label)
        }
    }

    def publishEndObservation(label: DataLabel, missingKeywords: Traversable[FitsKeyword], errorKeywords: Traversable[(FitsKeyword, CollectionError.CollectionError)]) {
        for (consumer <- registeredConsumers) {
            consumer.receiveEndObservation(label, missingKeywords, errorKeywords)
        }
    }

    @Bind(aggregate = true)
    def bindConsumer(consumer: ObservationStateConsumer) {
        registeredConsumers += consumer
    }

    @Unbind(aggregate = true)
    def unbindConsumer(consumer: ObservationStateConsumer) {
        registeredConsumers -= consumer
    }
}
