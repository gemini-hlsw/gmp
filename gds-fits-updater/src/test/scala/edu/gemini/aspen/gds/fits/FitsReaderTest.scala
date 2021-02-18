package edu.gemini.aspen.gds.fits

import org.junit.Assert._
import java.io.File
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import edu.gemini.aspen.gds.api.fits.{FitsKeyword, Header}

@RunWith(classOf[JUnitRunner])
class FitsReaderTest extends FunSuite with FitsSamplesDownloader with FitsSamplesVerificationData with BeforeAndAfter {
  before {
    downloadFile("FITS_WITH_2_EXTENSIONS.fits", "d0702f110e0888c002d958438bfe747e")
    downloadFile("test0029.fits", "091ab82301e6017f735602c1af598ba4")
    downloadFile("test1546.fits", "527b8899b9eb65fec92350626d2232ee")
  }

  def verifyKeysInHeader(header: Header, names: List[String]):Unit = {
    val keyNames = header.keywords map {
      _.keywordName.key
    }
    assertEquals(names.size, keyNames.size)
    // Verify all expected keys are there
    names foreach {
      k => assertTrue(keyNames.contains(k))
    }
  }

  test("keywords in sample without extensions") {
    val fr = new FitsReader(new File(classOf[FitsReaderTest].getResource("sample1.fits").toURI))
    val pdu = fr.header(0).get
    assertEquals(0, pdu.index)
    verifyKeysInHeader(pdu, keysSample1PDU)

    // Request an unknown header
    assertEquals(None, fr.header(1))
  }

  test("keywords in extension") {
    val fr = new FitsReader(new File(classOf[FitsReaderTest].getResource("sampleWithExt.fits").toURI))

    // Check reading the primary
    val pdu = fr.header(0).get
    assertEquals(0, pdu.index)
    verifyKeysInHeader(pdu, keysSampleWithExtensionPDU)

    // Check reading the extension 1
    val extension1 = fr.header(1).get
    assertEquals(1, extension1.index)
    verifyKeysInHeader(extension1, keysSampleWithExtensionExt1)
  }

  test("keywords in 2nd extension") {
    val fr = new FitsReader(new File("FITS_WITH_2_EXTENSIONS.fits"))

    // Check reading the primary
    val pdu = fr.header(0).get
    assertEquals(0, pdu.index)
    verifyKeysInHeader(pdu, keysSampleWith2ExtPDU)

    // Check reading the extension 1
    val extension1 = fr.header(1).get
    assertEquals(1, extension1.index)
    verifyKeysInHeader(extension1, keysSampleWith2ExtExt1)

    // Check reading the extension 2
    val extension2 = fr.header(2).get
    assertEquals(2, extension2.index)
    verifyKeysInHeader(extension2, keysSampleWith2ExtExt2)
  }

  test("keywords in GPI sample without extensions") {
    val fr = new FitsReader(new File("test0029.fits"))

    // Check reading the primary
    val pdu = fr.header(0).get
    assertEquals(0, pdu.index)
    verifyKeysInHeader(pdu, keysGPISample1PDU)
  }

  test("keywords in GPI sample with extensions") {
    val fr = new FitsReader(new File("test1546.fits"))

    // Check reading the primary
    val pdu = fr.header(0).get
    assertEquals(0, pdu.index)
    verifyKeysInHeader(pdu, keysGPISample2PDU)

    // Check reading the extension 1
    val extension1 = fr.header(1).get
    assertEquals(1, extension1.index)
    verifyKeysInHeader(extension1, keysGPISample2Ext1)
  }

  test("sample1 checksum") {
    val fr = new FitsReader(new File(classOf[FitsReaderTest].getResource("sample1.fits").toURI))
    assertEquals(sample1PDUHash, fr.checksumData(0))
    assertEquals("", fr.checksumData(1))
  }

  test("sample1 dimensions") {
    val fr = new FitsReader(new File(classOf[FitsReaderTest].getResource("sample1.fits").toURI))
    val pdu = fr.header(0).get
    assertEquals(0, pdu.fileOffset)
    assertEquals(23040, pdu.size)
    assertEquals(40320, pdu.dataSize)
  }

  test("sampleWithExt checksum") {
    val fr = new FitsReader(new File(classOf[FitsReaderTest].getResource("sampleWithExt.fits").toURI))
    assertEquals(sampleExtPDUHash, fr.checksumData(0))
    assertEquals(sampleExtExt1Hash, fr.checksumData(1))
  }

  test("samplWithExt dimensions") {
    val fr = new FitsReader(new File(classOf[FitsReaderTest].getResource("sampleWithExt.fits").toURI))
    val pdu = fr.header(0).get
    assertEquals(0, pdu.fileOffset)
    assertEquals(20160, pdu.size)
    assertEquals(34560, pdu.dataSize)

    val ext1 = fr.header(1).get
    assertEquals(34560+20160, ext1.fileOffset)
    assertEquals(11520, ext1.size)
    assertEquals(2880, ext1.dataSize)
  }

  test("GPI sample1 checksum") {
    val fr = new FitsReader(new File("test0029.fits"))
    assertEquals(gpiNoExtSamplePDUHash, fr.checksumData(0))
  }

  test("GPI sample2 checksum") {
    val fr = new FitsReader(new File("test1546.fits"))
    assertEquals(gpiExtSamplePDUHash, fr.checksumData(0))
    assertEquals(gpiExtSampleExt1, fr.checksumData(1))
  }

  test("read keys only") {
    val fr = new FitsReader(new File(classOf[FitsReaderTest].getResource("sampleWithExt.fits").toURI))
    assertEquals(keysSampleWithExtensionPDU map FitsKeyword.apply, fr.keys(0))
    assertEquals(keysSampleWithExtensionExt1 map FitsKeyword.apply, fr.keys(1))
  }

}