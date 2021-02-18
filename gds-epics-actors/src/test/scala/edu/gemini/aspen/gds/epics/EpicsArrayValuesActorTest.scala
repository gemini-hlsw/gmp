package edu.gemini.aspen.gds.epics

import org.junit.Assert._
import org.junit.Test
import edu.gemini.aspen.giapi.data.DataLabel
import org.specs2.mock.Mockito
import edu.gemini.aspen.gds.api.Conversions._
import edu.gemini.aspen.gds.api._
import edu.gemini.epics.api.ReadOnlyChannel
import fits.FitsKeyword
import scala.jdk.CollectionConverters._

class EpicsArrayValuesActorTest extends Mockito {
  val dataLabel = new DataLabel("GS-2011")

  val channelName = "ws:massAirmass"
  val referenceValue = "an epics string" :: "another epics string" :: Nil
  val fitsKeyword = new FitsKeyword("AIRMASS")
  val fitsKeyword2 = new FitsKeyword("AIRMASS2")


  @Test
  def testRight():Unit = {
    // mock return value
    val ch = mock[ReadOnlyChannel[String]]
    ch.getAll returns referenceValue

    val epicsValueActor = new EpicsArrayValuesActor(ch,
      GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS", 0, "STRING", true, "NONE", "EPICS", channelName, 0, "", "Mean airmass for the observation") ::
        GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS2", 0, "STRING", true, "NONE", "EPICS", channelName, 1, "", "Mean airmass for the observation") :: Nil)

    // Send an init message
    val result = epicsValueActor !! Collect

    result() match {
      case CollectedValue(keyword, value, comment, 0) :: CollectedValue(keyword2, value2, comment2, 0) :: Nil => {
        assertEquals(fitsKeyword, keyword)
        assertEquals(fitsKeyword2, keyword2)
        assertEquals(referenceValue(0), value)
        assertEquals(referenceValue(1), value2)
        assertEquals("Mean airmass for the observation", comment)
        assertEquals("Mean airmass for the observation", comment2)
      }
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(ch).getAll()
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testWrongDatatype():Unit = {
    // mock return value
    val ch = mock[ReadOnlyChannel[String]]

    val epicsValueActor = new EpicsArrayValuesActor(ch,
      GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS", 0, "STRING", true, "NONE", "EPICS", channelName, 0, "", "Mean airmass for the observation") ::
        GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS2", 0, "DOUBLE", true, "NONE", "EPICS", channelName, 1, "", "Mean airmass for the observation") :: Nil)


  }

  @Test(expected = classOf[IllegalArgumentException])
  def testWrongSource():Unit = {
    // mock return value
    val ch = mock[ReadOnlyChannel[String]]

    val epicsValueActor = new EpicsArrayValuesActor(ch,
      GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS", 0, "STRING", true, "NONE", "EPICS", channelName, 0, "", "Mean airmass for the observation") ::
        GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS2", 0, "STRING", true, "NONE", "EPICS", channelName + "bla", 1, "", "Mean airmass for the observation") :: Nil)


  }

  @Test
  def testExceptionOnCollect():Unit = {
    // mock return value cannot be read
    val ch = mock[ReadOnlyChannel[String]]
    ch.getAll throws new RuntimeException

    val epicsValueActor = new EpicsArrayValuesActor(ch,
      GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS", 0, "STRING", true, "NONE", "EPICS", channelName, 0, "", "Mean airmass for the observation") ::
        GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS2", 0, "STRING", true, "NONE", "EPICS", channelName, 2, "", "Mean airmass for the observation") :: Nil)


    // Send an init message
    val result = epicsValueActor !! Collect

    result() match {
      case ErrorCollectedValue(keyword, error, comment, 0) :: ErrorCollectedValue(keyword2, error2, comment2, 0) :: Nil => {
        assertEquals(fitsKeyword, keyword)
        assertEquals(fitsKeyword2, keyword2)
        assertEquals(CollectionError.GenericError, error)
        assertEquals(CollectionError.GenericError, error2)
        assertEquals("Mean airmass for the observation", comment)
        assertEquals("Mean airmass for the observation", comment2)
      }
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(ch).getAll()

  }

  @Test
  def testOneCollectError():Unit = {
    // mock return value
    val ch = mock[ReadOnlyChannel[String]]
    ch.getAll returns referenceValue

    val epicsValueActor = new EpicsArrayValuesActor(ch,
      GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS", 0, "STRING", true, "NONE", "EPICS", channelName, 0, "", "Mean airmass for the observation") ::
        GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS2", 0, "STRING", true, "NONE", "EPICS", channelName, 2, "", "Mean airmass for the observation") :: Nil)

    // Send an init message
    val result = epicsValueActor !! Collect

    result() match {
      case CollectedValue(keyword, value, comment, 0) :: ErrorCollectedValue(keyword2, CollectionError.ArrayIndexOutOfBounds, comment2, 0) :: Nil => {
        assertEquals(fitsKeyword, keyword)
        assertEquals(fitsKeyword2, keyword2)
        assertEquals(referenceValue(0), value)
        assertEquals("Mean airmass for the observation", comment)
        assertEquals("Mean airmass for the observation", comment2)
      }
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(ch).getAll()
  }

}

