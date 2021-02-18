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

class EpicsValuesActorTest extends Mockito {
  val dataLabel = new DataLabel("GS-2011")

  val channelName = "ws:massAirmass"
  val referenceValue = "an epics string" :: "another epics string" :: Nil
  val fitsKeyword = new FitsKeyword("AIRMASS")
  val nullValue = DefaultValue("NONE")

  def buildConfiguration(mandatory: Boolean, arrayIndex: Int) =
    GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS", 0, "STRING", mandatory, "NONE", "EPICS", channelName, arrayIndex, "", "Mean airmass for the observation")

  @Test
  def testReplyToCollect():Unit = {
    val configuration = buildConfiguration(true, 0)
    // mock return value
    val ch = mock[ReadOnlyChannel[String]]
    ch.getFirst returns referenceValue.head

    val epicsValueActor = new EpicsValuesActor(ch, configuration)

    // Send an init message
    val result = epicsValueActor !! Collect

    result() match {
      case CollectedValue(keyword, value, comment, 0) :: Nil => {
        assertEquals(fitsKeyword, keyword)
        assertEquals(referenceValue(0), value)
        assertEquals("Mean airmass for the observation", comment)
      }
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(ch).getFirst()
  }

  @Test
  def testAccessArrayElement():Unit = {
    val configuration = buildConfiguration(true, 1)
    // mock return value
    val ch = mock[ReadOnlyChannel[String]]
    ch.getAll returns referenceValue

    val epicsValueActor = new EpicsValuesActor(ch, configuration)

    // Send an init message
    val result = epicsValueActor !! Collect

    result() match {
      case CollectedValue(keyword, value, comment, 0) :: Nil
      => {
        assertEquals(fitsKeyword, keyword)
        assertEquals(referenceValue(1), value)
        assertEquals("Mean airmass for the observation", comment)
      }
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(ch).getAll
  }

  // should not return anything if the value cannot be read. The default will be added by an PostProcessingPolicy
  // it doesn't matter at this point if the item is mandatory or not
  @Test
  def testCollectingADefaultValue:Unit = {
    val configuration = buildConfiguration(false, 0)
    // mock return value cannot be read
    val ch = mock[ReadOnlyChannel[String]]
    ch.getFirst returns null

    val epicsValueActor = new EpicsValuesActor(ch, configuration)

    // Send an init message
    val result = epicsValueActor !! Collect


    result() match {
      case Nil =>
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(ch).getFirst()

  }

  // should not return anything if the value cannot be read. The default will be added by an PostProcessingPolicy
  // it doesn't matter at this point if the item is mandatory or not
  @Test
  def testCollectError:Unit = {
    val configuration = buildConfiguration(true, 0)
    // mock return value cannot be read
    val ch = mock[ReadOnlyChannel[String]]
    ch.getFirst returns null

    val epicsValueActor = new EpicsValuesActor(ch, configuration)


    // Send an init message
    val result = epicsValueActor !! Collect

    result() match {
      case Nil =>
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(ch).getFirst()

  }

  @Test
  def testExceptionOnCollect:Unit = {
    val configuration = buildConfiguration(true, 0)
    // mock return value cannot be read
    val ch = mock[ReadOnlyChannel[String]]
    ch.getFirst throws new RuntimeException

    val epicsValueActor = new EpicsValuesActor(ch, configuration)

    // Send an init message
    val result = epicsValueActor !! Collect

    result() match {
      case ErrorCollectedValue(keyword, error, comment, 0) :: Nil => {
        assertEquals(fitsKeyword, keyword)
        assertEquals(CollectionError.GenericError, error)
        assertEquals("Mean airmass for the observation", comment)
      }
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(ch).getFirst()

  }

  @Test
  def testCollectTypeMismatch:Unit = {
    val configuration = GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS", 0, "DOUBLE", true, "NONE", "EPICS", channelName, 0, "", "Mean airmass for the observation")
    val ch = mock[ReadOnlyChannel[String]]
    ch.getFirst returns "a string"

    val epicsValueActor = new EpicsValuesActor(ch, configuration)


    // Send an init message
    val result = epicsValueActor !! Collect

    result() match {
      case ErrorCollectedValue(keyword, error, comment, 0) :: Nil => {
        assertEquals(fitsKeyword, keyword)
        assertEquals(CollectionError.TypeMismatch, error)
        assertEquals("Mean airmass for the observation", comment)
      }
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(ch).getFirst()

  }

  @Test
  def testCollectTypeMismatchFromDoubleToInt:Unit = {
    val configuration = GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS", 0, "INT", true, "NONE", "EPICS", channelName, 0, "", "Mean airmass for the observation")

    val ch = mock[ReadOnlyChannel[java.lang.Double]]
    // mock return value cannot be read
    ch.getFirst returns new java.lang.Double(1.1)

    val epicsValueActor = new EpicsValuesActor(ch, configuration)



    // Send an init message
    val result = epicsValueActor !! Collect

    result() match {
      case ErrorCollectedValue(keyword, error, comment, 0) :: Nil => {
        assertEquals(fitsKeyword, keyword)
        assertEquals(CollectionError.TypeMismatch, error)
        assertEquals("Mean airmass for the observation", comment)
      }
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(ch).getFirst()

  }

  @Test
  def testArrayIndexOutOfBounds:Unit = {
    val configuration = buildConfiguration(true, 2)

    val ch = mock[ReadOnlyChannel[String]]
    ch.getAll returns referenceValue

    val epicsValueActor = new EpicsValuesActor(ch, configuration)


    // Send an init message
    val result = epicsValueActor !! Collect

    result() match {
      case ErrorCollectedValue(keyword, error, comment, 0) :: Nil
      => {
        assertEquals(fitsKeyword, keyword)
        assertEquals(CollectionError.ArrayIndexOutOfBounds, error)
        assertEquals("Mean airmass for the observation", comment)
      }
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(ch).getAll()

  }

}

