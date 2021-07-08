package edu.gemini.aspen.gds.observations

import edu.gemini.aspen.giapi.data.{ DataLabel, ObservationEvent }
import java.util.logging.Logger
import org.osgi.service.event.{ Event, EventHandler }

class ObservationEventReceiver(eventHandler: (DataLabel, ObservationEvent) => Unit)
    extends EventHandler {
  private val logger = Logger.getLogger(this.getClass.getName)

  def handleEvent(event: Event): Unit =
    event.getProperty(ObservationEventReceiver.ObsEventKey) match {
      case (e: ObservationEvent, d: DataLabel) => eventHandler(d, e)
      // Original GDS did a sys.error here. Switched to logging it.
      case e                                   => logger.severe(s"Received unknown event: $e")
    }
}

object ObservationEventReceiver {
  val ObsEventTopic = "edu/gemini/aspen/gds/obsevent/handler"
  val ObsEventKey   = "observationevent"
}
