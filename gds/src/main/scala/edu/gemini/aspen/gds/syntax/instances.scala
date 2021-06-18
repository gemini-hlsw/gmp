package edu.gemini.aspen.gds.syntax

import cats.Eq
import edu.gemini.aspen.giapi.data.ObservationEvent

trait instances {
  implicit val eqObservationEvent: Eq[ObservationEvent] = Eq.by(_.ordinal)
}

object instances extends instances
