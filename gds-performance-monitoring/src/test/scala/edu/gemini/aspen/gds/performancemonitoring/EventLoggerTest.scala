package edu.gemini.aspen.gds.performancemonitoring

import java.time.{Duration, LocalDateTime}

import org.scalatest.junit.AssertionsForJUnit
import org.junit.{Before, Test}

class EventLoggerTest extends AssertionsForJUnit {

  private def check(value: Duration, target: Duration, margin: Duration) = {
    (value.compareTo(target) > 0 || value == target) && (value.minus(target).compareTo(margin) < 0) ||
      ((value.compareTo(target) < 0 || value == target) && target.minus(value).compareTo(margin) < 0)
  }

  private def busyWait(value: Duration): Unit = {
    var start = LocalDateTime.now
    while (Duration.between(start, LocalDateTime.now).compareTo(value) < 0 ) {}
  }

  val delay: Duration = Duration.ofMillis(100)
  val precision: Duration = Duration.ofMillis(100)
  var el: EventLogger[String, String] = _

  var holaInterval: Duration = _

  @Before
  def setup():Unit = {
    el = new EventLogger

    el.addEventSet("set")
    el.start("set", "hola")
    val startTime = LocalDateTime.now
    busyWait(delay)
    el.end("set", "hola")
    holaInterval = Duration.between(startTime, LocalDateTime.now)

    el.addEventSet("otroset")
    el.start("otroset", "hola")
    busyWait(delay)
    el.end("otroset", "hola")

    el.addEventSet("otrosetmas")
    el.start("otrosetmas", "hola")
    busyWait(delay)
    el.end("otrosetmas", "hola")

    el.start("set", "chao")
    el.end("set", "Oops")
  }

  @Test
  def testRetrieve():Unit = {

    val x = el.retrieve("set")
    assert(check(x("hola").get, holaInterval, precision), "Time is: " + x("hola").get)
    assert(x("chao").isEmpty) //doesn't end
    assert(x("Oops").isEmpty) //doesn't start
  }

  @Test
  def testRetrieveEvent():Unit = {
    el.retrieve("set", "hola") match {
      case Some(x: Duration) => assert(check(x, holaInterval, precision), "Time is: " + x)
      case _ => fail()
    }
  }

  @Test
  def testRetrieveEventEmpty():Unit = {
    el.retrieve("set", "chao") match {
      case None =>
      case _ => fail()
    }
  }

  @Test
  def testRetrieveEventAverage():Unit = {
    el.average("hola") match {
      case Some(x: Duration) => assert(check(x, delay, precision), "Time is: " + x)
      case _ => fail()
    }
  }

  @Test
  def testRetrieveEventAverageEmpty():Unit = {
    el.average("chao") match {
      case None =>
      case _ => fail()
    }
  }

  @Test
  def testCheck():Unit = {
    assert(el.check("set", "hola", 600))
  }

  @Test
  def testCheckEmpty():Unit = {
    assert(!el.check("set", "chao", 600))
  }

}