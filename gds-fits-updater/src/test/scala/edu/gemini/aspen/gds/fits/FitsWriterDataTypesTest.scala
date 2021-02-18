package edu.gemini.aspen.gds.fits

import org.junit.Assert._
import java.io.File
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import edu.gemini.aspen.gds.api.fits.{HeaderItem, Header}
import edu.gemini.aspen.gds.api.Conversions._
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import edu.gemini.aspen.gds.api.FitsType

@RunWith(classOf[JUnitRunner])
class FitsWriterDataTypesTest extends FunSuite with BeforeAndAfterEach {
  val destFile = File.createTempFile("test", ".fits")

  override def afterEach():Unit = {
    if (destFile.exists()) destFile.delete()
  }

  // Creates a new header with a single keyword
  def createHeadersWithKeyword[T](value: T)(implicit _type: FitsType[T]): Header = Header(0, List(HeaderItem("KEY", value, "comment", None)))
  def createHeadersWithKeywordAndFormat[T](value: T, format:String)(implicit _type: FitsType[T]): Header = Header(0, List(HeaderItem("KEY", value, "comment", Some(format))))

  // Verifies that a header contains all the original keywords plus new keywords
  def toMap(header: Header) = header.keywords.map {
    k => k.keywordName.key -> k.value
  }.toMap

  test("update keyword with string item") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeyword("value")

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("value", toMap(updatedHeader).get("KEY").get)
  }

  test("update keyword with string item and formatting") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeywordAndFormat("value", "Cool format: %s")

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("Cool format: value", toMap(updatedHeader).get("KEY").get)
  }

  test("update keyword with double item") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeyword(1.0)

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("1.0", toMap(updatedHeader).get("KEY").get)
  }

  test("update keyword with double item and formatting") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeywordAndFormat(16.014562745683578467946798467637, "%20.4G")

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("16.01", toMap(updatedHeader).get("KEY").get)
  }

  test("update keyword with infinite double value and no formatting") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeywordAndFormat(Double.PositiveInfinity, "")

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("INF", toMap(updatedHeader).get("KEY").get)
  }

  test("update keyword with infinite double value and formatting") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeywordAndFormat(Double.PositiveInfinity, "%.2f")

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("INF", toMap(updatedHeader).get("KEY").get)
  }

  test("update keyword with Nan double value and no formatting") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeywordAndFormat(Double.NaN, "")

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("NAN", toMap(updatedHeader).get("KEY").get)
  }

  test("update keyword with Nan double value and formatting") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeywordAndFormat(Double.NaN, "%.2f")

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("NAN", toMap(updatedHeader).get("KEY").get)
  }

  test("update keyword with int item") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeyword(1)

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("1", toMap(updatedHeader).get("KEY").get)
  }

  test("update keyword with int item and formatting") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeywordAndFormat(11,"0x%04X")

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("0x000B", toMap(updatedHeader).get("KEY").get)
  }

  test("update keyword with true boolean item") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeyword(true)

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("T", toMap(updatedHeader).get("KEY").get)
  }

  test("update keyword with false boolean item and format") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeywordAndFormat(false,"%B")

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("FALSE", toMap(updatedHeader).get("KEY").get)
  }

  test("update keyword with false boolean item") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeyword(false)

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("F", toMap(updatedHeader).get("KEY").get)
  }

  test("update keyword with wrong format") {
    val originalFile = new File(classOf[FitsWriterDataTypesTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithKeywordAndFormat(1.1, "Bla: %d")

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("KEY"))
    assertEquals("1.1", toMap(updatedHeader).get("KEY").get)
  }
}