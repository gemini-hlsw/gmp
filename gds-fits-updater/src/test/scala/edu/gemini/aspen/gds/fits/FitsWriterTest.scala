package edu.gemini.aspen.gds.fits

import org.junit.Assert._
import java.io.File
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import com.google.common.io.Files
import edu.gemini.aspen.gds.api.fits.{HeaderItem, Header}
import edu.gemini.aspen.gds.api.Conversions._
import org.scalatest.{BeforeAndAfterEach, BeforeAndAfter, FunSuite}

@RunWith(classOf[JUnitRunner])
class FitsWriterTest extends FunSuite with FitsSamplesDownloader with FitsSamplesVerificationData with BeforeAndAfter with BeforeAndAfterEach {
  before {
    downloadFile("FITS_WITH_2_EXTENSIONS.fits", "d0702f110e0888c002d958438bfe747e")
    downloadFile("test0029.fits", "091ab82301e6017f735602c1af598ba4")
    downloadFile("test1546.fits", "527b8899b9eb65fec92350626d2232ee")
  }

  val destFile = File.createTempFile("test", ".fits")

  override def afterEach():Unit = {
    if (destFile.exists()) destFile.delete()
  }

  // Creates a new header with a single keyword
  def createHeadersWithAirMass(headerIndex: Int): Header = Header(headerIndex, List(HeaderItem("AIRMASS", 1.0, "Mass of airmass", None)))

  // Verifies that a header contains all the original keywords plus new keywords
  def verifyKeysInHeader(header: Header, names: List[String], newKeywords: List[String]):Unit = {
    val keyNames = header.keywords map {
      _.keywordName.key
    }
    assertEquals(names.size + newKeywords.size, keyNames.size)
    // Verify all expected keys are there
    (names ++ newKeywords) foreach {
      k => assertTrue(keyNames.contains(k))
    }
  }

  test("update in unknown extensions are ignored") {
    val originalFile = new File(classOf[FitsWriterTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val headerUpdate = createHeadersWithAirMass(1)
    fitsFile.updateHeader(headerUpdate, destFile)

    Files.equal(originalFile, destFile)
  }

  test("update keywords in sample without extensions") {
    val originalFile = new File(classOf[FitsWriterTest].getResource("sample1.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)

    val pduHash = hashDataSection(originalFile, fitsFile.header(0).get)
    val headerUpdate = createHeadersWithAirMass(0)

    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)
    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("AIRMASS"))

    verifyKeysInHeader(updatedHeader, keysSample1PDU, List("AIRMASS"))
    // Verify the data section is intact
    assertEquals(sample1PDUHash, updatedFitsFile.checksumData(0))
    assertEquals(pduHash, hashDataSection(destFile, updatedHeader))
  }

  test("update keywords in PDU in sample with extension") {
    val originalFile = new File(classOf[FitsWriterTest].getResource("sampleWithExt.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)
    val pduHash = hashDataSection(originalFile, fitsFile.header(0).get)
    val extHash = hashDataSection(originalFile, fitsFile.header(1).get)

    val headerUpdate = createHeadersWithAirMass(0)

    fitsFile.updateHeader(headerUpdate, destFile)
    val updatedFitsFile = new FitsReader(destFile)

    val updatedHeader = updatedFitsFile.header(0).get

    assertTrue(updatedHeader.containsKey("AIRMASS"))
    verifyKeysInHeader(updatedHeader, keysSampleWithExtensionPDU, List("AIRMASS"))
    verifyKeysInHeader(updatedFitsFile.header(1).get, keysSampleWithExtensionExt1, Nil)

    // Verify the data section is intact
    assertEquals(sampleExtPDUHash, fitsFile.checksumData(0))
    // Verify the data section is intact
    assertEquals(sampleExtExt1Hash, fitsFile.checksumData(1))
    // data section checksum
    assertEquals(pduHash, hashDataSection(destFile, updatedFitsFile.header(0).get))
    assertEquals(extHash, hashDataSection(destFile, updatedFitsFile.header(1).get))
  }

  test("update keywords in ext1 in sample with extension") {
    val originalFile = new File(classOf[FitsWriterTest].getResource("sampleWithExt.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)
    val pduHash = hashDataSection(originalFile, fitsFile.header(0).get)
    val extHash = hashDataSection(originalFile, fitsFile.header(1).get)

    val headerUpdate = createHeadersWithAirMass(1)

    fitsFile.updateHeader(headerUpdate, destFile)
    val updatedFitsFile = new FitsReader(destFile)

    val updatedHeader = updatedFitsFile.header(1).get

    assertTrue(updatedHeader.containsKey("AIRMASS"))
    verifyKeysInHeader(updatedHeader, keysSampleWithExtensionExt1, List("AIRMASS"))
    verifyKeysInHeader(updatedFitsFile.header(0).get, keysSampleWithExtensionPDU, Nil)

    // Verify the data section is intact
    assertEquals(sampleExtPDUHash, updatedFitsFile.checksumData(0))
    // Verify the data section is intact
    assertEquals(sampleExtExt1Hash, updatedFitsFile.checksumData(1))
    // data section checksum
    assertEquals(pduHash, hashDataSection(destFile, updatedFitsFile.header(0).get))
    assertEquals(extHash, hashDataSection(destFile, updatedFitsFile.header(1).get))
  }

  test("update keywords in both extensions in sample with extension") {
    val originalFile = new File(classOf[FitsWriterTest].getResource("sampleWithExt.fits").toURI)
    val fitsFile = new FitsWriter(originalFile)
    val pduHash = hashDataSection(originalFile, fitsFile.header(0).get)
    val extHash = hashDataSection(originalFile, fitsFile.header(1).get)

    val headerUpdate0 = createHeadersWithAirMass(0)
    val headerUpdate1 = createHeadersWithAirMass(1)

    fitsFile.updateHeaders(headerUpdate0 :: headerUpdate1 :: Nil, destFile)
    val updatedFitsFile = new FitsReader(destFile)

    val updatedHeader0 = updatedFitsFile.header(0).get
    val updatedHeader1 = updatedFitsFile.header(1).get

    assertTrue(updatedHeader0.containsKey("AIRMASS"))
    assertTrue(updatedHeader1.containsKey("AIRMASS"))
    verifyKeysInHeader(updatedHeader0, keysSampleWithExtensionPDU, List("AIRMASS"))
    verifyKeysInHeader(updatedHeader1, keysSampleWithExtensionExt1, List("AIRMASS"))

    // Verify the data section is intact
    assertEquals(sampleExtPDUHash, updatedFitsFile.checksumData(0))
    // Verify the data section is intact
    assertEquals(sampleExtExt1Hash, updatedFitsFile.checksumData(1))
    // data section checksum
    assertEquals(pduHash, hashDataSection(destFile, updatedFitsFile.header(0).get))
    assertEquals(extHash, hashDataSection(destFile, updatedFitsFile.header(1).get))
  }

  test("writing many keywords in GPI sample without extensions") {
    val originalFile = new File("test0029.fits")
    val fitsFile = new FitsWriter(originalFile)

    val pduHash = hashDataSection(originalFile, fitsFile.header(0).get)

    val originalHeader = fitsFile.header(0).get
    val originalHeaderKeywordCount = originalHeader.keywords.size
    val headerDim = (originalHeader.fileOffset, originalHeader.size, originalHeader.dataSize)
    assertEquals(gpiNoExtSamplePDUHash, fitsFile.checksumData(0))

    // Approximately 300 keywords are added to GPI file in the primary
    val headers = for (i <- 0 to 300) yield HeaderItem("GPIK" + i, "value" + i, "comment " + i, None)
    val headerUpdate = Header(0, headers)
    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)

    val updatedHeader = updatedFitsFile.header(0).get
    val updatedHeaderDim = (updatedHeader.fileOffset, updatedHeader.size, updatedHeader.dataSize)
    assertEquals(originalHeaderKeywordCount + headers.size, updatedHeader.keywords.size)

    // Ensure all original keywords are in place
    originalHeader.keywords foreach {
      k => assertTrue(updatedHeader.containsKey(k.keywordName.key))
    }

    // Ensure all new keywords are in place
    headers foreach {
      k => assertTrue(updatedHeader.containsKey(k.keywordName.key))
    }

    // Verify data checksum
    assertEquals(gpiNoExtSamplePDUHash, updatedFitsFile.checksumData(0))
    assertEquals("", updatedFitsFile.checksumData(1))

    // Check data size have not changed
    assertEquals(headerDim._3, updatedHeaderDim._3)

    // Check header alignment
    assertEquals(destFile.length(), updatedHeaderDim._1 + updatedHeaderDim._2 + updatedHeaderDim._3)

    // data section checksum
    assertEquals(pduHash, hashDataSection(destFile, updatedFitsFile.header(0).get))
  }

  test("writing many keywords in GPI sample with extensions") {
    val originalFile = new File("test1546.fits")
    val fitsFile = new FitsWriter(originalFile)

    val originalHeader = fitsFile.header(0).get
    val headerDim = (originalHeader.fileOffset, originalHeader.size, originalHeader.dataSize)
    val originalHeaderKeywordCount = originalHeader.keywords.size
    val originalExtHeader = fitsFile.header(1).get
    val extDim = (originalExtHeader.fileOffset, originalExtHeader.size, originalExtHeader.dataSize)
    val pduDataHash = hashDataSection(originalFile, fitsFile.header(0).get)
    val ext1DataHash = hashDataSection(originalFile, fitsFile.header(1).get)
    assertEquals(gpiExtSamplePDUHash, new FitsReader(originalFile).checksumData(0))

    // Approximately 300 keywords are added to GPI file in the primary
    val headers = for (i <- 0 to 300) yield HeaderItem("GPIK" + i, "value" + i, "comment " + i, None)
    val headerUpdate = Header(0, headers)
    fitsFile.updateHeader(headerUpdate, destFile)

    val updatedFitsFile = new FitsReader(destFile)

    val updatedHeader = updatedFitsFile.header(0).get
    val updatedHeaderDim = (updatedHeader.fileOffset, updatedHeader.size, updatedHeader.dataSize)
    val updatedExtHeader = updatedFitsFile.header(1).get
    val updatedExtDim = (updatedExtHeader.fileOffset, updatedExtHeader.size, updatedExtHeader.dataSize)
    assertEquals(originalHeaderKeywordCount + headers.size, updatedHeader.keywords.size)

    // Ensure all original keywords are in place
    originalHeader.keywords foreach {
      k => assertTrue(updatedHeader.containsKey(k.keywordName.key))
    }

    // Ensure all new keywords are in place
    headers foreach {
      k => assertTrue(updatedHeader.containsKey(k.keywordName.key))
    }

    // Verify data checksum
    assertEquals(gpiExtSamplePDUHash, updatedFitsFile.checksumData(0))
    assertEquals(gpiExtSampleExt1, updatedFitsFile.checksumData(1))
    assertEquals("", updatedFitsFile.checksumData(2))

    // Check data size have not changed
    assertEquals(headerDim._3, updatedHeaderDim._3)
    assertEquals(extDim._3, updatedExtDim._3)

    // Check extension header size has not changed
    assertEquals(extDim._2, updatedExtDim._2)

    // Check header alignment
    assertEquals(updatedExtDim._1, updatedHeaderDim._1 + updatedHeaderDim._2 + updatedHeaderDim._3)

    assertEquals(pduDataHash, hashDataSection(destFile, updatedFitsFile.header(0).get))
    assertEquals(ext1DataHash, hashDataSection(destFile, updatedFitsFile.header(1).get))

  }

  test("writing many keywords in GPI sample with extensions to PDU and EXT1") {
    val originalFile = new File("test1546.fits")
    val fitsFile = new FitsWriter(originalFile)

    assertEquals(gpiExtSamplePDUHash, new FitsReader(originalFile).checksumData(0))
    val originalHeader = fitsFile.header(0).get
    val headerDim = (originalHeader.fileOffset, originalHeader.size, originalHeader.dataSize)
    val originalHeaderKeywordCount = originalHeader.keywords.size
    val originalExtHeader = fitsFile.header(1).get
    val extDim = (originalExtHeader.fileOffset, originalExtHeader.size, originalExtHeader.dataSize)
    val extHeaderKeywordCount = originalExtHeader.keywords.size
    val pduDataHash = hashDataSection(originalFile, fitsFile.header(0).get)
    val ext1DataHash = hashDataSection(originalFile, fitsFile.header(1).get)

    // Approximately 300 keywords are added to GPI file in the primary
    val headersPDU = for (i <- 0 to 300) yield HeaderItem("GPIK" + i, "value" + i, "comment " + i, None)
    val headersEXT1 = for (i <- 0 to 300) yield HeaderItem("GPIK" + i, "value" + i, "comment " + i, None)
    fitsFile.updateHeaders(Header(0, headersPDU) :: Header(1, headersEXT1) :: Nil, destFile)

    val updatedFitsFile = new FitsReader(destFile)

    val updatedHeader = updatedFitsFile.header(0).get
    val updatedHeaderDim = (updatedHeader.fileOffset, updatedHeader.size, updatedHeader.dataSize)
    val updatedExtHeader = updatedFitsFile.header(1).get
    assertEquals(None, updatedFitsFile.header(2))
    val updatedExtDim = (updatedExtHeader.fileOffset, updatedExtHeader.size, updatedExtHeader.dataSize)
    assertEquals(originalHeaderKeywordCount + headersPDU.size, updatedHeader.keywords.size)
    assertEquals(extHeaderKeywordCount + headersEXT1.size, updatedExtHeader.keywords.size)

    // Ensure all original keywords are in place
    originalHeader.keywords foreach {
      k => assertTrue(updatedHeader.containsKey(k.keywordName.key))
    }
    originalExtHeader.keywords foreach {
      k => assertTrue(updatedExtHeader.containsKey(k.keywordName.key))
    }

    // Ensure all new keywords are in place
    headersPDU foreach {
      k => assertTrue(updatedHeader.containsKey(k.keywordName.key))
    }
    headersEXT1 foreach {
      k => assertTrue(updatedExtHeader.containsKey(k.keywordName.key))
    }

    // Verify data checksum
    assertEquals(gpiExtSamplePDUHash, updatedFitsFile.checksumData(0))
    assertEquals(gpiExtSampleExt1, updatedFitsFile.checksumData(1))
    assertEquals("", updatedFitsFile.checksumData(2))

    // Check data size have not changed
    assertEquals(headerDim._3, updatedHeaderDim._3)
    assertEquals(extDim._3, updatedExtDim._3)

    // Check header alignment
    assertEquals(updatedExtDim._1, updatedHeaderDim._1 + updatedHeaderDim._2 + updatedHeaderDim._3)

    assertEquals(pduDataHash, hashDataSection(destFile, updatedFitsFile.header(0).get))
    assertEquals(ext1DataHash, hashDataSection(destFile, updatedFitsFile.header(1).get))

  }

}