package edu.gemini.aspen.gds.observationstate.impl

import java.time.Duration
import java.util

import org.junit.runner.RunWith
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.scalatest.mock.MockitoSugar
import edu.gemini.aspen.gds.observationstate.ObservationStateRegistrar
import edu.gemini.aspen.giapi.data.{DataLabel, ObservationEvent}
import edu.gemini.aspen.gds.api._
import org.osgi.service.event.Event

@RunWith(classOf[JUnitRunner])
class GDSEventsListenerTest extends FunSuite with MockitoSugar {
  val dataLabel = new DataLabel("label")

  test("start observation") {
    val registrar = mock[ObservationStateRegistrar]
    val listener = new GDSEventsListener(registrar)
    val event: Event = buildEvent(GDSStartObservation(dataLabel))

    listener.handleEvent(event)
    verify(registrar).startObservation(dataLabel)
  }

  private def buildEvent(not: GDSNotification) = {
    val properties: util.HashMap[String, GDSNotification] = new java.util.HashMap()
    properties.put(GDSNotification.GDSNotificationKey, not)
    new Event(GDSNotification.GDSEventsTopic, properties)
  }

  test("end observation") {
    val registrar = mock[ObservationStateRegistrar]
    val listener = new GDSEventsListener(registrar)

    listener.handleEvent(buildEvent(GDSEndObservation(dataLabel, 200L)))
    verify(registrar).endObservation(dataLabel, 200L, Nil)
  }

  test("register times") {
    val registrar = mock[ObservationStateRegistrar]
    val listener = new GDSEventsListener(registrar)

    listener.handleEvent(buildEvent(GDSObservationTimes(dataLabel, Traversable((ObservationEvent.OBS_START_ACQ, Some(Duration.ofSeconds(100)))))))
    verify(registrar).registerTimes(same(dataLabel), any())
  }

  test("observation error") {
    val registrar = mock[ObservationStateRegistrar]
    val listener = new GDSEventsListener(registrar)

    listener.handleEvent(buildEvent(GDSObservationError(dataLabel, "msg")))
    verify(registrar).registerError(dataLabel, "msg")
  }
}