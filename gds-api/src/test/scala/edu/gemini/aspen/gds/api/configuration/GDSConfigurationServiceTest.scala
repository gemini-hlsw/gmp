package edu.gemini.aspen.gds.api.configuration

import edu.gemini.aspen.gds.api._
import org.junit.Assert._
import edu.gemini.aspen.gds.api.Conversions._
import java.io.File
import edu.gemini.aspen.gds.api.Predef._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import com.google.common.io.Files
import com.google.common.base.Charsets
import util.parsing.input.OffsetPosition

/**
 * Specify how the GDSConfiguration parser should behave
 */
@RunWith(classOf[JUnitRunner])
class GDSConfigurationServiceTest extends FunSuite {
  val ORIGINAL_CONFIG = "gds-keywords.conf"
  val ORIGINAL_BAD_CONFIG = "gds-keywords-with-error.conf"
  val NEW_CONFIG = "gds-keywords.conf.test"
  val TEST_DIR = "/tmp/"

  protected def copyFile(fromResource: String, toAbsolute: String):Unit = {
    copy(new File(this.getClass.getResource(fromResource).toURI), new File(toAbsolute))
  }

  protected def checkOriginalContent(config: List[GDSConfiguration]):Unit = {
    assertTrue(config.contains(GDSConfiguration("GPI", "OBS_END_ACQ", "AIRMASS", 0, "DOUBLE", false, "NONE", "EPICS", "ws:massAirmass", 0, "", "Mean airmass for the observation")))
    //todo: check for the rest of the items
  }

  test("read config") {
    copyFile(ORIGINAL_CONFIG, TEST_DIR + ORIGINAL_CONFIG)
    val serviceOrig = new GDSConfigurationServiceImpl(TEST_DIR + ORIGINAL_CONFIG)

    val readConfig = serviceOrig.getConfiguration

    assertEquals(9, readConfig.length)
    assertFalse(serviceOrig.hasError)
    checkOriginalContent(readConfig)
  }

  test("get for update") {
    copyFile(ORIGINAL_CONFIG, TEST_DIR + ORIGINAL_CONFIG)
    val serviceOrig = new GDSConfigurationServiceImpl(TEST_DIR + ORIGINAL_CONFIG)

    val readConfig = serviceOrig.getFullConfiguration
    assertEquals(15, readConfig.size)
    assertEquals(9, GDSConfigurationFile.getConfiguration(readConfig).length)
    checkOriginalContent(GDSConfigurationFile.getConfiguration(readConfig))
  }

  test("new file") {
    copyFile(ORIGINAL_CONFIG, TEST_DIR + NEW_CONFIG)
    val serviceNew = new GDSConfigurationServiceImpl(TEST_DIR + NEW_CONFIG)

    val config = GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS", 0, "DOUBLE", true, "NONE", "EPICS", "gpi:value", 0, "", "Mean airmass for the observation")
    serviceNew.replaceConfiguration(config :: Nil)
    val readConfig = serviceNew.getConfiguration
    assertEquals(1, readConfig.length)
    assertTrue(readConfig.contains(config))
  }

  test("add item") {
    copyFile(ORIGINAL_CONFIG, TEST_DIR + NEW_CONFIG)
    val serviceNew = new GDSConfigurationServiceImpl(TEST_DIR + NEW_CONFIG)

    val config = GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS", 0, "DOUBLE", true, "NONE", "EPICS", "gpi:value", 0, "", "Mean airmass for the observation")
    serviceNew.addConfiguration(config :: Nil)
    val readConfig = serviceNew.getConfiguration

    assertEquals(10, readConfig.length)
    checkOriginalContent(readConfig)
    assertTrue(readConfig.contains(config))
  }

  test("update item") {
    copyFile(ORIGINAL_CONFIG, TEST_DIR + NEW_CONFIG)
    val serviceNew = new GDSConfigurationServiceImpl(TEST_DIR + NEW_CONFIG)

    val newConfig = GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS", 0, "DOUBLE", true, "NONE", "EPICS", "gpi:value", 0, "", "Mean airmass for the observation")
    val modConfig = GDSConfiguration("MOD", "OBS_START_ACQ", "AIRMASS", 0, "DOUBLE", true, "NONE", "EPICS", "gpi:value", 0, "", "Mean airmass for the observation")
    val readConfig: List[ConfigItem[_]] = serviceNew.getFullConfiguration
    val newElement: ConfigItem[_] = new ConfigItem(newConfig)
    val removedConfig = GDSConfigurationFile.getConfiguration(readConfig).tail.head //2nd element
    val newConfigList: List[ConfigItem[_]] = readConfig.updated(7, new ConfigItem(modConfig)) :+ newElement
    serviceNew.updateConfiguration(newConfigList)
    val readAgainConfig: List[ConfigItem[_]] = serviceNew.getFullConfiguration

    assertEquals(16, readAgainConfig.size) //5 comments,9 old keywords, 1 new keyword, 1 blank line
    checkOriginalContent(GDSConfigurationFile.getConfiguration(readAgainConfig))
    assertTrue(GDSConfigurationFile.getConfiguration(readAgainConfig).contains(newConfig))
    assertTrue(GDSConfigurationFile.getConfiguration(readAgainConfig).contains(modConfig))
    assertFalse(GDSConfigurationFile.getConfiguration(readAgainConfig).contains(removedConfig))
  }

  test("read bad config") {
    copyFile(ORIGINAL_BAD_CONFIG, TEST_DIR + ORIGINAL_BAD_CONFIG)
    val serviceOrig = new GDSConfigurationServiceImpl(TEST_DIR + ORIGINAL_BAD_CONFIG)

    val readConfig = serviceOrig.getConfiguration

    assertEquals(0, readConfig.length)
    assertTrue(serviceOrig.hasError)
  }

  test("read textContent") {
    copyFile(ORIGINAL_CONFIG, TEST_DIR + NEW_CONFIG)
    val serviceOrig = new GDSConfigurationServiceImpl(TEST_DIR + NEW_CONFIG)

    val fileContent = serviceOrig.textContent

    val originalContent = Files.asCharSource(new File(TEST_DIR + NEW_CONFIG), Charsets.UTF_8).read()
    assertEquals(originalContent, fileContent)
    assertTrue(serviceOrig.errors.isEmpty)
  }

  test("get errors") {
    copyFile(ORIGINAL_BAD_CONFIG, TEST_DIR + ORIGINAL_BAD_CONFIG)
    val serviceOrig = new GDSConfigurationServiceImpl(TEST_DIR + ORIGINAL_BAD_CONFIG)

    assertEquals("""string matching regex `\d+' expected but `?' found""", serviceOrig.errors.get._1)
    assertEquals(616, serviceOrig.errors.get._2)
    assertEquals(OffsetPosition(serviceOrig.textContent, 616).column, serviceOrig.errors.get._3.column)
    assertEquals(OffsetPosition(serviceOrig.textContent, 616).line, serviceOrig.errors.get._3.line)
  }

}