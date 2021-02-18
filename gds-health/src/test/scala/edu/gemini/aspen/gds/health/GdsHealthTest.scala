package edu.gemini.aspen.gds.health

import org.junit.Assert._
import edu.gemini.jms.activemq.provider.ActiveMQJmsProvider
import edu.gemini.aspen.giapi.statusservice.{StatusHandlerAggregate, StatusService}
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import edu.gemini.aspen.giapi.status.{Health, StatusHandler, StatusItem}
import edu.gemini.aspen.gds.api.{GDSObseventHandler, KeywordActorsFactory, KeywordSource}
import java.util.concurrent.{CountDownLatch, TimeUnit}

import actors.threadpool.AtomicInteger
import edu.gemini.gmp.top.Top
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import edu.gemini.aspen.giapi.status.setter.StatusSetterService

@RunWith(classOf[JUnitRunner])
class GdsHealthTest extends FunSuite with MockitoSugar with BeforeAndAfter {
  val healthName = "gpitest:gds:health"
  val origHealthName = "gds:health"
  val healthMessageName = "gpitest:gds:health:message"
  val origHealthMessageName = "gds:health:message"
  val testCounter = new AtomicInteger(0)

  val agg = new StatusHandlerAggregate
  val provider = new ActiveMQJmsProvider("vm://GdsHealthTest?broker.useJmx=false&broker.persistent=false")
  val top = mock[Top]
  var setter: StatusSetterService = _

  // Remove non actor based sources and add 2 for GDSObseventHandlerImpl and HeaderReceiver
  val expectedUpdates = (KeywordSource.values - KeywordSource.NONE - KeywordSource.INSTRUMENT).size + 2

  var statusservice: StatusService = _

  private class TestHandler(retries: Int) extends StatusHandler {
    override def getName = "Test Handler for GdsHealthTest"

    val counter = new AtomicInteger(0)
    val latch = new CountDownLatch(retries)
    var lastHealthStatusItem: StatusItem[_] = _

    override def update[T](item: StatusItem[T]):Unit = {
      if (item.getName.equals(healthName)) {
        lastHealthStatusItem = item

        counter.incrementAndGet()
        latch.countDown()
      }
    }

    def waitForCompletion():Unit = {
      assertTrue(latch.await(5, TimeUnit.SECONDS))
    }

  }

  before {
    provider.startConnection()
    statusservice = new StatusService(agg, "Status Service " + testCounter.incrementAndGet(), ">")
    statusservice.startJms(provider)
    setter = new StatusSetterService
    setter.startJms(provider)

    when(top.buildStatusItemName(same(origHealthName))).thenReturn(healthName)
    when(top.buildStatusItemName(same(origHealthMessageName))).thenReturn(healthMessageName)
  }

  after {
    setter.stopJms()
    statusservice.stopJms()
  }

  test("Bad Health") {
    val gdsHealth = new GdsHealth(top, setter)
    gdsHealth.startJms(provider)

    val handler = new TestHandler(1)
    agg.bindStatusHandler(handler)

    handler.waitForCompletion()
    assertEquals(1, handler.counter.get())
    assertEquals(healthName, handler.lastHealthStatusItem.getName)
    assertEquals(Health.BAD, handler.lastHealthStatusItem.getValue)
    agg.unbindStatusHandler(handler)

  }

  ignore("Warning Health") {
    val gdsHealth = new GdsHealth(top, setter)
    gdsHealth.startJms(provider)

    val handler = new TestHandler(2)
    agg.bindStatusHandler(handler)

    gdsHealth.bindGDSObseventHandler(mock[GDSObseventHandler])
    handler.waitForCompletion()
    assertEquals(2, handler.counter.get())
    assertEquals(healthName, handler.lastHealthStatusItem.getName)
    assertEquals(Health.WARNING, handler.lastHealthStatusItem.getValue)
    agg.unbindStatusHandler(handler)
  }

  def bindAllHealthSources(gdsHealth: GdsHealth):Unit = {
    gdsHealth.bindGDSObseventHandler(mock[GDSObseventHandler])
    val fact = mock[KeywordActorsFactory]
    for (source <- KeywordSource.values - KeywordSource.NONE - KeywordSource.INSTRUMENT - KeywordSource.ODB) {
      println("Bind source " + source)
      when(fact.getSource).thenReturn(source)
      gdsHealth.bindActorFactory(fact)
    }

    gdsHealth.bindHeaderReceiver()
  }

  ignore("Good Health") {
    val gdsHealth = new GdsHealth(top, setter)
    gdsHealth.startJms(provider)

    val handler = new TestHandler(expectedUpdates + 1)
    agg.bindStatusHandler(handler)

    bindAllHealthSources(gdsHealth)
    TimeUnit.SECONDS.sleep(2)
    handler.waitForCompletion()
    assertEquals(healthName, handler.lastHealthStatusItem.getName)
    assertEquals(Health.GOOD, handler.lastHealthStatusItem.getValue)
    agg.unbindStatusHandler(handler)

  }

  ignore("Unbind some elements to warning") {
    val gdsHealth = new GdsHealth(top, setter)
    gdsHealth.startJms(provider)

    val startHandler = new TestHandler(expectedUpdates - 1)
    agg.bindStatusHandler(startHandler)
    bindAllHealthSources(gdsHealth)
    startHandler.waitForCompletion()
    agg.unbindStatusHandler(startHandler)

    val handler = new TestHandler(2)
    agg.bindStatusHandler(handler)

    gdsHealth.unbindHeaderReceiver()
    handler.waitForCompletion()
    assertEquals(healthName, handler.lastHealthStatusItem.getName)
    assertEquals(Health.WARNING, handler.lastHealthStatusItem.getValue)
    agg.unbindStatusHandler(handler)

  }

  ignore("Unbind some elements to bad") {
    val gdsHealth = new GdsHealth(top, setter)
    gdsHealth.startJms(provider)

    val startHandler = new TestHandler(expectedUpdates)
    agg.bindStatusHandler(startHandler)
    bindAllHealthSources(gdsHealth)
    startHandler.waitForCompletion()
    agg.unbindStatusHandler(startHandler)

    val warningHandler = new TestHandler(2)
    agg.bindStatusHandler(warningHandler)

    gdsHealth.unbindHeaderReceiver()
    warningHandler.waitForCompletion()
    agg.unbindStatusHandler(warningHandler)

    val handler = new TestHandler(1)
    agg.bindStatusHandler(handler)

    gdsHealth.unbindGDSObseventHandler(mock[GDSObseventHandler])
    handler.waitForCompletion()
    assertEquals(1, handler.counter.get())
    assertEquals(healthName, handler.lastHealthStatusItem.getName)
    assertEquals(Health.BAD, handler.lastHealthStatusItem.getValue)
    agg.unbindStatusHandler(handler)

  }

}