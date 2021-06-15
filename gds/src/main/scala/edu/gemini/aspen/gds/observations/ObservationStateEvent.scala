package edu.gemini.aspen.gds.observations

import edu.gemini.aspen.gds.keywords.CollectedKeyword
import edu.gemini.aspen.giapi.data.{ DataLabel, ObservationEvent }

sealed trait ObservationStateEvent

object ObservationStateEvent {
  final case class Start(dataLabel: DataLabel, programId: String) extends ObservationStateEvent
  final case class Stop(dataLabel: DataLabel) extends ObservationStateEvent
  final case class Abort(dataLabel: DataLabel) extends ObservationStateEvent
  final case class Delete(dataLabel: DataLabel) extends ObservationStateEvent
  final case class AddObservationEvent(dataLabel: DataLabel, obsEvent: ObservationEvent)
      extends ObservationStateEvent
  final case class AddKeyword(dataLabel: DataLabel, keyword: CollectedKeyword)
      extends ObservationStateEvent
  final case class CollectKeywords(dataLabel: DataLabel, obsEvent: ObservationEvent)
      extends ObservationStateEvent
  // used by ObservationFSM to step through the observation completion.
  final case class Step(dataLabel: DataLabel) extends ObservationStateEvent
  final case class Complete(dataLabel: DataLabel) extends ObservationStateEvent
  final case object PurgeStale extends ObservationStateEvent
}
