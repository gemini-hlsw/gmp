package edu.gemini.aspen.gds.fits

import java.io.File
import org.junit.Assert._
import edu.gemini.aspen.giapi.data.DataLabel
import edu.gemini.aspen.gds.api.fits.{HeaderItem, Header}
import edu.gemini.aspen.gds.api.Conversions._
import org.scalatest.{BeforeAndAfterEach, FunSuite}

trait FitsBaseTest extends FunSuite with BeforeAndAfterEach {
  val originalFile: File
  val destinationFile: File
  val dataLabel: DataLabel

  def createHeadersWithAirMass(headerIndex: Int): List[Header] =  List(Header(headerIndex, List(HeaderItem("AIRMASS", 1.0, "Mass of airmass", None))))

  def updateFitsFile(headers: List[Header]) = {
    val fitsUpdater = new FitsUpdater(originalFile.getParentFile, destinationFile.getParentFile, dataLabel, headers)
    fitsUpdater.updateFitsHeaders(outputNamingFunction = label => destinationFile.getName)
  }

  def verifyKeywordInHeader(header: Header, keyword: String):Unit = {
    assertTrue(header.containsKey(keyword))
  }

  def verifyKeywordNotInHeader(header: Header, keyword: String):Unit = {
    assertFalse(header.containsKey(keyword))
  }

  override def afterEach(): Unit = if (destinationFile.exists):Unit = {
    destinationFile.delete
  }
}
