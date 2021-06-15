package edu.gemini.aspen.gds.observations

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.effect.std.QueueSink
import edu.gemini.aspen.giapi.data.{ DataLabel, ObservationEvent }
import org.osgi.service.event.{ Event, EventHandler }

class ObservationEventReceiver(obsStateQ: QueueSink[IO, ObservationStateEvent])(implicit
  rt:                                     IORuntime
) extends EventHandler {
  def handleEvent(event: Event): Unit =
    event.getProperty(ObservationEventReceiver.ObsEventKey) match {
      case (e: ObservationEvent, d: DataLabel) =>
        obsStateQ.offer(ObservationStateEvent.AddObservationEvent(d, e)).unsafeRunSync()
      // sys.error is what the old event handler did here, so I replicated.
      case _                                   => sys.error("Unknown message from observation event")
    }
}

object ObservationEventReceiver {
  val ObsEventTopic = "edu/gemini/aspen/gds/obsevent/handler"
  val ObsEventKey   = "observationevent"
}
