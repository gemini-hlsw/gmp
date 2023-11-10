package edu.gemini.aspen.gds.observations

import edu.gemini.aspen.giapi.data.{ DataLabel, ObservationEvent, ObservationEventHandler }

class ObservationEventReceiver(eventHandler: (DataLabel, ObservationEvent) => Unit)
    extends ObservationEventHandler {
  private val logger = Logger.getLogger(this.getClass.getName)

  def onObservationEvent(event: ObservationEvent, dataLabel: DataLabel): Unit = {
    logger.debug(s"Event $event received for $dataLabel")
    eventHandler(dataLabel, event)
  }
}
