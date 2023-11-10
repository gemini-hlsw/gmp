package edu.gemini.aspen.gds.observations

import edu.gemini.aspen.giapi.data.{ DataLabel, ObservationEvent, ObservationEventHandler }
import java.util.logging.Logger

class ObservationEventReceiver(eventHandler: (DataLabel, ObservationEvent) => Unit)
    extends ObservationEventHandler {
  private val logger = Logger.getLogger(this.getClass.getName)

  def onObservationEvent(event: ObservationEvent, dataLabel: DataLabel): Unit = {
    logger.fine(s"Event $event received for $dataLabel")
    eventHandler(dataLabel, event)
  }
}
