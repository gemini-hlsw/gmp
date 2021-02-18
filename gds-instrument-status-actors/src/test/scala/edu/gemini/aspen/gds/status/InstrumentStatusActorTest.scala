package edu.gemini.aspen.gds.status

import org.junit.Test
import edu.gemini.aspen.giapi.data.DataLabel
import org.specs2.mock.Mockito
import edu.gemini.aspen.gds.api.Conversions._
import edu.gemini.aspen.giapi.status.StatusDatabaseService
import edu.gemini.aspen.giapi.status.impl.BasicStatus
import org.junit.Assert._
import edu.gemini.aspen.gds.api._
import fits.FitsKeyword

class InstrumentStatusActorTest extends Mockito {
  val defaultValue = "DEFAULT"
  val fitsKeyword = new FitsKeyword("GPISTATU")
  val dataLabel = new DataLabel("GS-2011")
  val statusDB = mock[StatusDatabaseService]
  val statusItemName = "gpi:status1"

  @Test
  def testReplyToCollect:Unit = {
    val configuration = buildConfiguration(statusItemName, false)

    val referenceValue = "ok"
    // mock return value
    val statusItem = new BasicStatus[String](statusItemName, referenceValue)
    statusDB.getStatusItem(statusItemName) returns statusItem

    val instrumentStatusActor = new InstrumentStatusActor(statusDB, configuration)

    // Send a Collect message and wait the response
    val result = instrumentStatusActor !! Collect


    result() match {
      case CollectedValue(keyword, value, comment, 0) :: Nil => {
        assertEquals(fitsKeyword, keyword)
        assertEquals(referenceValue, value)
        assertEquals("Current global status of the instrument", comment)
      }
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(statusDB).getStatusItem(statusItemName)
  }

  // should not return anything if the value cannot be read. The default will be added by an PostProcessingPolicy
  // it doesn't matter at this point if the item is mandatory or not
  @Test
  def testReplyToCollectIfStatusUnknown:Unit = {
    val configuration = buildConfiguration(statusItemName, false)

    // mock return value
    statusDB.getStatusItem(statusItemName) returns null

    val instrumentStatusActor = new InstrumentStatusActor(statusDB, configuration)

    // Send a Collect message and wait the response
    val result = instrumentStatusActor !! Collect

    result() match {
      case Nil =>
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(statusDB).getStatusItem(statusItemName)
  }

  // should not return anything if the value cannot be read. The default will be added by an PostProcessingPolicy
  // it doesn't matter at this point if the item is mandatory or not
  @Test
  def testErrorMandatoryItemAndStatusUnknown:Unit = {
    val configuration = buildConfiguration(statusItemName, true)

    // mock return value
    statusDB.getStatusItem(statusItemName) returns null

    val instrumentStatusActor = new InstrumentStatusActor(statusDB, configuration)

    // Send a Collect message and wait the response
    val result = instrumentStatusActor !! Collect

    result() match {
      case Nil =>
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(statusDB).getStatusItem(statusItemName)
  }

  @Test
  def testTypeMismatchError:Unit = {
    val configuration = GDSConfiguration("GPI",
      "OBS_START_ACQ",
      fitsKeyword,
      0,
      // note the type here
      "DOUBLE",
      true,
      defaultValue,
      "STATUS",
      statusItemName,
      0,
      "",
      "Current global status of the instrument")

    val referenceValue = "ok"
    // mock return value
    val statusItem = new BasicStatus[String](statusItemName, referenceValue)
    statusDB.getStatusItem(statusItemName) returns statusItem

    val instrumentStatusActor = new InstrumentStatusActor(statusDB, configuration)

    // Send a Collect message and wait the response
    val result = instrumentStatusActor !! Collect

    result() match {
      case ErrorCollectedValue(keyword, error, comment, 0) :: Nil => {
        assertEquals(fitsKeyword, keyword)
        assertEquals(CollectionError.TypeMismatch, error)
        assertEquals("Current global status of the instrument", comment)
      }
      case _ => fail("Should not reply other message ")
    }

    // verify mock
    there was one(statusDB).getStatusItem(statusItemName)
  }


  def buildConfiguration(statusItem: String, mandatory: Boolean): GDSConfiguration = {
    GDSConfiguration("GPI",
      "OBS_START_ACQ",
      fitsKeyword,
      0,
      "STRING",
      mandatory,
      defaultValue,
      "STATUS",
      statusItem,
      0,
      "",
      "Current global status of the instrument")
  }
}

