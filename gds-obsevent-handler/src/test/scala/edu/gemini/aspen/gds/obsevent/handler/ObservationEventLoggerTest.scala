package edu.gemini.aspen.gds.obsevent.handler

import edu.gemini.aspen.giapi.data.ObservationEvent._
import edu.gemini.aspen.giapi.data.DataLabel
import org.junit.runner.RunWith
import org.junit.Assert._
import org.scalatest.junit.JUnitRunner
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import java.util.logging.Logger
import org.scalatest.mock.MockitoSugar
import org.scalatest.{OneInstancePerTest, FunSuite}
import java.util.concurrent.TimeUnit

@RunWith(classOf[JUnitRunner])
class ObservationEventLoggerTest extends FunSuite with MockitoSugar {
  val dataLabel = new DataLabel("GS-2011")

  test("check time exceeded") {
    val LOG = mock[Logger]
    val logger = new ObservationEventLogger(5000L)(LOG)

    logger.start(dataLabel, OBS_PREP)

    assertFalse(logger.check(dataLabel, OBS_PREP, 5000L))
    logger.checkTimeWithinLimits(OBS_PREP, dataLabel)

    verify(LOG).severe(anyString)
  }

  test("check time ok") {
    val LOG = mock[Logger]
    val logger = new ObservationEventLogger(5000L)(LOG)

    logger.start(dataLabel, OBS_PREP)

    TimeUnit.MILLISECONDS.sleep(100)

    logger.end(dataLabel, OBS_PREP)
    assertTrue(logger.check(dataLabel, OBS_PREP, 5000L))

    logger.checkTimeWithinLimits(OBS_PREP, dataLabel)

    verifyNoInteractions(LOG)
  }

  test("log timing") {
    val LOG = mock[Logger]
    val logger = new ObservationEventLogger(5000L)(LOG)

    logger.logTiming(OBS_PREP, dataLabel)

    // Boring test
    verify(LOG, times(2)).info(anyString)
  }

  test("enforce time constraints") {
    val LOG = mock[Logger]
    val logger = new ObservationEventLogger(5000L)(LOG)

    logger.enforceTimeConstraints(OBS_END_ACQ, dataLabel)

    // Boring test
    verify(LOG).severe(anyString)
  }
}