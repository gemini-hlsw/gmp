package edu.gemini.aspen.gds.obsevent.handler

import edu.gemini.aspen.gds.api.GDSObseventHandler
import edu.gemini.aspen.giapi.data.{DataLabel, ObservationEvent, ObservationEventHandler}
import org.osgi.service.event.{Event, EventAdmin}

/**
 * Very simple object that passes observation events through OSGi EventAdmin */
class ObservationEvent2EventAdmin(publisher: EventAdmin) extends ObservationEventHandler {

  def onObservationEvent(obsEvent: ObservationEvent, dataLabel: DataLabel):Unit = {
    val properties: java.util.HashMap[String, (ObservationEvent, DataLabel)] = new java.util.HashMap()
    properties.put(GDSObseventHandler.ObsEventKey, (obsEvent, dataLabel))
    val event = new Event(GDSObseventHandler.ObsEventTopic, properties)
    publisher.postEvent(event)
  }

}