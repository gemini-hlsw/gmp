package edu.gemini.aspen.gds.fits.checker

import java.io.File
import edu.gemini.aspen.gds.api.GDSConfiguration
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import edu.gemini.aspen.gds.observationstate.ObservationStateRegistrar
import edu.gemini.aspen.gds.api.Conversions._
import collection.immutable.Set
import edu.gemini.aspen.gmp.services.PropertyHolder
import edu.gemini.aspen.giapi.data.ObservationEvent
import edu.gemini.aspen.gds.api.configuration.GDSConfigurationService
import edu.gemini.aspen.gds.api.fits.FitsKeyword
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfter, FunSuite}
import java.util.concurrent.TimeUnit
import org.scalatest.mock.MockitoSugar
import com.google.common.io.Files

@RunWith(classOf[JUnitRunner])
class InstrumentKeywordCheckerTest extends FunSuite with BeforeAndAfter with MockitoSugar {
  val destinationFile = new File("/tmp/sample.fits")
  val destinationFileWithoutExtension = new File("/tmp/sample")
  val obsState = mock[ObservationStateRegistrar]
  val ph = mock[PropertyHolder]
  when(ph.getProperty(anyString)).thenReturn("/tmp")

  test("missing keywords") {
    val conf = mock[GDSConfigurationService]
    when(conf.getConfiguration).thenReturn(GDSConfiguration("GPI", "OBS_START_ACQ", "TELSCOP", 0, "DOUBLE", true, "NONE", "INSTRUMENT", "gpi:value", 0, "", "Mean airmass for the observation") :: Nil)

    val checker = new InstrumentKeywordsChecker(conf, obsState, ph)
    Files.copy(new File(classOf[InstrumentKeywordCheckerTest].getResource("sample.fits").toURI), destinationFile)

    checker.onObservationEvent(ObservationEvent.OBS_END_DSET_WRITE, "sample")
    TimeUnit.MILLISECONDS.sleep(200)
    verify(obsState, times(1)).registerMissingKeyword("sample", Set[FitsKeyword](new FitsKeyword("TELSCOP")))
  }

  test("check ok"){
    val conf = mock[GDSConfigurationService]
    when(conf.getConfiguration).thenReturn(GDSConfiguration("GPI", "OBS_START_ACQ", "TELESCOP", 0, "DOUBLE", true, "NONE", "INSTRUMENT", "gpi:value", 0, "", "Mean airmass for the observation") :: Nil)

    val checker = new InstrumentKeywordsChecker(conf, obsState, ph)
    Files.copy(new File(classOf[InstrumentKeywordCheckerTest].getResource("sample.fits").toURI), destinationFile)

    checker.onObservationEvent(ObservationEvent.OBS_END_DSET_WRITE, "sample.fits")
    TimeUnit.MILLISECONDS.sleep(200)
    verifyNoInteractions(obsState)
  }

  test("check without extension, bug GIAPI-932"){
    val conf = mock[GDSConfigurationService]
    when(conf.getConfiguration).thenReturn(GDSConfiguration("GPI", "OBS_START_ACQ", "TELESCOP", 0, "DOUBLE", true, "NONE", "INSTRUMENT", "gpi:value", 0, "", "Mean airmass for the observation") :: Nil)

    val checker = new InstrumentKeywordsChecker(conf, obsState, ph)
    Files.copy(new File(classOf[InstrumentKeywordCheckerTest].getResource("sample.fits").toURI), destinationFileWithoutExtension)

    checker.onObservationEvent(ObservationEvent.OBS_END_DSET_WRITE, "sample")
    TimeUnit.MILLISECONDS.sleep(200)
    verifyNoInteractions(obsState)
  }

  after {
    destinationFile.delete()
    destinationFileWithoutExtension.delete()
  }
}