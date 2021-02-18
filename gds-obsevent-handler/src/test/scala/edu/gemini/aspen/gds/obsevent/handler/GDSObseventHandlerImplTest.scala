package edu.gemini.aspen.gds.obsevent.handler

import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import edu.gemini.aspen.giapi.data.{DataLabel, ObservationEvent}
import edu.gemini.aspen.gds.keywords.database.impl.KeywordsDatabaseImpl
import edu.gemini.aspen.gds.actors.factory.CompositeActorsFactory
import java.io.File
import java.util

import edu.gemini.aspen.gmp.services.PropertyHolder
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite, OneInstancePerTest}
import java.util.concurrent.TimeUnit

import edu.gemini.aspen.gds.api._
import org.osgi.service.event.{Event, EventAdmin}

@RunWith(classOf[JUnitRunner])
class GDSObseventHandlerImplTest extends FunSuite with OneInstancePerTest with BeforeAndAfter {
  val actorsFactory = mock(classOf[CompositeActorsFactory])
  val propertyHolder = mock(classOf[PropertyHolder])
  val keywordsDatabase = new KeywordsDatabaseImpl()
  val tempDir = System.getProperty("java.io.tmpdir")
  when(propertyHolder.getProperty(anyString())).thenReturn(tempDir)

  val dataLabel = new DataLabel("GS-2011.fits")
  val dummyFile = new File(tempDir, dataLabel.getName)
  val mockPublisher = mock(classOf[EventAdmin])

  before {
    dummyFile.createNewFile()
  }

  private val observationHandler = new GDSObseventHandlerImpl(actorsFactory, keywordsDatabase, new CompositePostProcessingPolicyImpl(), propertyHolder, mockPublisher)

  test("verify all events") {
    for (evt <- ObservationEvent.values()) {
      when(actorsFactory.buildActors(evt, dataLabel)).thenReturn(List[KeywordValueActor]())

      val event: Event = buildGDSEvent(evt)
      observationHandler.handleEvent(event)

      sleep(300)

      // verify mock
      verify(actorsFactory).buildActors(evt, dataLabel)
    }
    verify(mockPublisher).postEvent(buildNotificationEvent(GDSStartObservation(dataLabel)))
    sleep(600)
    // use refEq to skip comparing the "times" field
//    verify(mockPublisher).postEvent(buildNotificationEvent(GDSObservationTimes(dataLabel, Nil)))
//    verify(mockPublisher).postEvent(buildNotificationEvent(GDSEndObservation(dataLabel, 40L)))
  }

//  test("with a missing event") {
//    when(actorsFactory.buildActors(any[ObservationEvent], any[DataLabel])).thenReturn(List[KeywordValueActor]())
//
//    for {evt <- ObservationEvent.values()
//         if (evt != ObservationEvent.EXT_END_OBS && evt != ObservationEvent.EXT_START_OBS && evt != ObservationEvent.OBS_START_ACQ)} {
//      observationHandler.onObservationEvent(evt, dataLabel)
//    }
//    sleep(300)
//    verify(mockPublisher).postEvent(GDSStartObservation(dataLabel))
//    verify(mockPublisher, times(0)).postEvent(refEq(GDSEndObservation(dataLabel, 40L), "writeTime"))
//    sleep(6500)
//    verify(mockPublisher).postEvent(refEq(new GDSObservationTimes(dataLabel, Nil), "times"))
//    verify(mockPublisher).postEvent(refEq(GDSEndObservation(dataLabel, 200L), "writeTime"))
//  }
//
//  test("with slow event") {
//    when(actorsFactory.buildActors(any[ObservationEvent], any[DataLabel])).thenReturn(List[KeywordValueActor]())
//
//    for (evt <- ObservationEvent.values()) {
//      if (evt != ObservationEvent.OBS_START_ACQ) {
//        observationHandler.onObservationEvent(evt, dataLabel)
//      }
//    }
//    sleep(300)
//    verify(mockPublisher).postEvent(GDSStartObservation(dataLabel))
//    verify(mockPublisher, times(0)).postEvent(refEq(GDSEndObservation(dataLabel, 200L), "writeTime"))
//    sleep(500)
//    observationHandler.onObservationEvent(ObservationEvent.OBS_START_ACQ, dataLabel)
//    sleep(1500)
//    verify(mockPublisher).postEvent(refEq(GDSEndObservation(dataLabel, 200L), "writeTime"))
//    verify(mockPublisher).postEvent(refEq(new GDSObservationTimes(dataLabel, Nil), "times"))
//  }
//
//  test("without start/end transaction") {
//    when(actorsFactory.buildActors(any[ObservationEvent], any[DataLabel])).thenReturn(List[KeywordValueActor]())
//
//    for {evt <- ObservationEvent.values()
//         if (evt != ObservationEvent.EXT_END_OBS && evt != ObservationEvent.EXT_START_OBS)
//    } {
//      observationHandler.onObservationEvent(evt, dataLabel)
//    }
//    sleep(300)
//    verify(mockPublisher).postEvent(GDSStartObservation(dataLabel))
//    sleep(1500)
//    verify(mockPublisher).postEvent(refEq(GDSEndObservation(dataLabel, 200L), "writeTime"))
//    verify(mockPublisher).postEvent(refEq(new GDSObservationTimes(dataLabel, Nil), "times"))
//  }
//
//  test("with start transaction but no end transaction") {
//    when(actorsFactory.buildActors(any[ObservationEvent], any[DataLabel])).thenReturn(List[KeywordValueActor]())
//
//    // Simulate an ext start obs arriving
//    observationHandler.onObservationEvent(ObservationEvent.EXT_START_OBS, dataLabel)
//
//    for {evt <- ObservationEvent.values()
//         if (evt != ObservationEvent.EXT_END_OBS && evt != ObservationEvent.EXT_START_OBS)} {
//      observationHandler.onObservationEvent(evt, dataLabel)
//    }
//    sleep(500)
//    verify(mockPublisher).postEvent(GDSStartObservation(dataLabel))
//    sleep(500)
//    verify(mockPublisher, times(0)).postEvent(refEq(GDSEndObservation(dataLabel, 200L), "writeTime"))
//  }
//
//  test("with start transaction and end transaction") {
//    // Simulate an ext start obs arriving
//    when(actorsFactory.buildActors(any[ObservationEvent], any[DataLabel])).thenReturn(List[KeywordValueActor]())
//
//    // Simulate an ext start obs arriving
//    observationHandler.onObservationEvent(ObservationEvent.EXT_START_OBS, dataLabel)
//
//    for {evt <- ObservationEvent.values()
//         if (evt != ObservationEvent.EXT_END_OBS && evt != ObservationEvent.EXT_START_OBS)} {
//      observationHandler.onObservationEvent(evt, dataLabel)
//    }
//    sleep(500)
//    verify(mockPublisher).postEvent(GDSStartObservation(dataLabel))
//    sleep(500)
//    verify(mockPublisher, times(0)).postEvent(refEq(GDSEndObservation(dataLabel, 200L), "writeTime"))
//
//    // Simulate an end obs arriving
//    observationHandler.onObservationEvent(ObservationEvent.EXT_END_OBS, dataLabel)
//    sleep(500)
//    verify(mockPublisher).postEvent(refEq(GDSEndObservation(dataLabel, 200L), "writeTime"))
//    verify(mockPublisher).postEvent(refEq(new GDSObservationTimes(dataLabel, Nil), "times"))
//  }
//
//  test("with writing error") {
//    // Simulate an ext start obs arriving
//    when(actorsFactory.buildActors(any[ObservationEvent], any[DataLabel])).thenReturn(List[KeywordValueActor]())
//
//    val dummyDataLabel = new DataLabel("dummy")
//
//    for {evt <- ObservationEvent.values()
//         if (evt != ObservationEvent.EXT_END_OBS && evt != ObservationEvent.EXT_START_OBS)} {
//      observationHandler.onObservationEvent(evt, dummyDataLabel)
//    }
//    sleep(500)
//    verify(mockPublisher).postEvent(GDSStartObservation(dummyDataLabel))
//    sleep(2500)
//    verify(mockPublisher, times(0)).postEvent(refEq(GDSEndObservation(dataLabel, 200L), "writeTime"))
//    verify(mockPublisher).postEvent(refEq(GDSObservationError(dummyDataLabel, ""), "msg"))
//  }

  private def buildNotificationEvent(notification: GDSNotification) = {
    val properties: util.HashMap[String, GDSNotification] = new java.util.HashMap()
    properties.put(GDSNotification.GDSNotificationKey, notification)
    val event = new Event(GDSNotification.GDSEventsTopic, properties)
    event
  }

  private def buildGDSEvent(evt: ObservationEvent) = {
    val properties: util.HashMap[String, (ObservationEvent, DataLabel)] = new java.util.HashMap()
    properties.put(GDSObseventHandler.ObsEventKey, (evt, dataLabel))
    new Event(GDSObseventHandler.ObsEventTopic, properties)
  }

  private def sleep(time: Long):Unit = {
    TimeUnit.MILLISECONDS.sleep(time)
  }

  after {
    dummyFile.delete()
  }
}