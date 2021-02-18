package edu.gemini.aspen.gds.obsevent.handler

import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import edu.gemini.aspen.giapi.data.DataLabel
import edu.gemini.aspen.gds.api.Conversions._
import java.io.File
import edu.gemini.aspen.gmp.services.PropertyHolder
import org.junit.runner.RunWith
import org.junit.Assert._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatest.mock.MockitoSugar
import java.util.logging.Logger
import edu.gemini.aspen.gds.api.CollectedValue
import edu.gemini.aspen.gds.api.fits.{HeaderItem, Header}

@RunWith(classOf[JUnitRunner])
class FitsFileProcessorTest extends FunSuite with MockitoSugar with BeforeAndAfter {
  val propertyHolder: PropertyHolder = mock[PropertyHolder]
  val tempDir: String = System.getProperty("java.io.tmpdir")
  when(propertyHolder.getProperty(anyString())).thenReturn(tempDir)

  val dataLabel = new DataLabel("GS-2011.fits")
  val dummyFile = new File(tempDir, dataLabel.getName)

  before {
    dummyFile.createNewFile()
  }

  test("convertToHeaders empty") {
    implicit val LOG: Logger = mock[Logger]
    val fp = new FitsFileProcessor(propertyHolder)

    assertEquals(List(Header(0, Nil)), fp.convertToHeaders(Nil))
  }

  test("convertToHeaders with one header") {
    implicit val LOG: Logger = mock[Logger]
    val fp = new FitsFileProcessor(propertyHolder)

    val cv = List(CollectedValue("KEY", "1.0", "comment", 0, None))

    assertEquals(List(Header(0, HeaderItem("KEY", "1.0", "comment", None) :: Nil)), fp.convertToHeaders(cv))

    val cv2 = CollectedValue("KEY", "1.0", "comment", 0, None) :: CollectedValue("KEY2", "1.0", "comment", 0, None) :: Nil

    assertEquals(List(Header(0, HeaderItem("KEY", "1.0", "comment", None) :: HeaderItem("KEY2", "1.0", "comment", None) :: Nil)), fp.convertToHeaders(cv2))
  }

  test("convertToHeaders with format") {
    implicit val LOG: Logger = mock[Logger]
    val fp = new FitsFileProcessor(propertyHolder)

    val cv = List(CollectedValue("KEY", "1.0", "comment", 0, Some("%.2f")))

    assertEquals(List(Header(0, HeaderItem("KEY", "1.0", "comment", Some("%.2f")) :: Nil)), fp.convertToHeaders(cv))

    val cv2 = CollectedValue("KEY", "1.0", "comment", 0, None) :: CollectedValue("KEY2", "1.0", "comment", 0, Some("%.2f")) :: Nil

    assertEquals(List(Header(0, HeaderItem("KEY", "1.0", "comment", None) :: HeaderItem("KEY2", "1.0", "comment", Some("%.2f")) :: Nil)), fp.convertToHeaders(cv2))
  }

  test("convertToHeaders with two headers") {
    implicit val LOG: Logger = mock[Logger]
    val fp = new FitsFileProcessor(propertyHolder)

    val cv2 = CollectedValue("KEY", "1.0", "comment", 0, None) :: CollectedValue("KEY2", "1.0", "comment", 1, None) :: Nil

    assertEquals(Header(0, HeaderItem("KEY", "1.0", "comment", None) :: Nil) :: Header(1, HeaderItem("KEY2", "1.0", "comment", None) :: Nil) :: Nil, fp.convertToHeaders(cv2))
  }

  test("write to file") {
    implicit val LOG: Logger = mock[Logger]
    val fp = new FitsFileProcessor(propertyHolder)

    val cv = CollectedValue("KEY", "1.0", "comment", 0, None) :: CollectedValue("KEY2", "1.0", "comment", 1, None) :: Nil

    fp.updateFITSFile(dataLabel, cv) match {
      case Right(x) => // We expect the operation to succeed
      case Left(x) => fail()
    }
  }


  test("read an non-existing file") {
    implicit val LOG: Logger = mock[Logger]
    val fp = new FitsFileProcessor(propertyHolder)

    val cv = CollectedValue("KEY", "1.0", "comment", 0, None) :: CollectedValue("KEY2", "1.0", "comment", 1, None) :: Nil

    fp.updateFITSFile(dataLabel + ".nonvalid", cv) match {
      case Right(x) => fail()
      case Left(x) => // We expect an error in this case
    }
  }

  test("write to a non valid destination") {
    implicit val LOG: Logger = mock[Logger]
    val propertyHolder = mock[PropertyHolder]
    when(propertyHolder.getProperty("DHS_SCIENCE_DATA_PATH")).thenReturn(tempDir)
    when(propertyHolder.getProperty("DHS_PERMANENT_SCIENCE_DATA_PATH")).thenReturn("<")

    val fp = new FitsFileProcessor(propertyHolder)

    val cv = CollectedValue("KEY", "1.0", "comment", 0, None) :: CollectedValue("KEY2", "1.0", "comment", 1, None) :: Nil

    fp.updateFITSFile(dataLabel, cv) match {
      case Right(x) => fail()
      case Left(x) => // We expect an error in this case
    }
  }

  after {
    dummyFile.delete()
  }
}