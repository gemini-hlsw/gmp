package edu.gemini.aspen.gmp.main

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import org.junit.Assert._
import org.junit.Test

class ServicesConfigTest {

  private def write(dir: java.nio.file.Path, name: String, content: String): Unit =
    Files.write(dir.resolve(name), content.getBytes(StandardCharsets.UTF_8))

  @Test
  def loadsFactoryStyleFileNames(): Unit = {
    val dir = Files.createTempDirectory("services")
    write(dir, "edu.gemini.gmp.top.Top-default.cfg", "epicsTop=gmp\ngiapiTop=gmp\n")

    val config = ServicesConfig.load(dir)
    val top    = config.first("edu.gemini.gmp.top.Top")
    assertTrue(top.isDefined)
    assertEquals(Some("default"), top.get.instance)
    assertEquals(Some("gmp"), top.get.get("epicsTop"))
  }

  @Test
  def loadsSingletonStyleFileNames(): Unit = {
    val dir = Files.createTempDirectory("services")
    write(dir, "some.pid.cfg", "key=value\n")

    val config = ServicesConfig.load(dir)
    assertEquals(Some("value"), config.first("some.pid").flatMap(_.get("key")))
  }

  @Test
  def expandsSystemProperties(): Unit = {
    System.setProperty("services.config.test", "/tmp/x")
    try {
      val dir = Files.createTempDirectory("services")
      write(dir, "a.b.C-default.cfg", "file=${services.config.test}/f.xml\nmissing=${no.such.prop}\n")

      val config = ServicesConfig.load(dir)
      val c      = config.first("a.b.C").get
      assertEquals(Some("/tmp/x/f.xml"), c.get("file"))
      // unresolvable references stay visible
      assertEquals(Some("${no.such.prop}"), c.get("missing"))
    } finally System.clearProperty("services.config.test")
  }

  @Test
  def typedAccessors(): Unit = {
    val dir = Files.createTempDirectory("services")
    write(dir, "t.T-default.cfg", "i=5\nd=0.25\nb=TRUE\nbad=1.0\n")

    val c = ServicesConfig.load(dir).first("t.T").get
    assertEquals(Some(5), c.intValue("i"))
    assertEquals(Some(0.25), c.doubleValue("d"))
    assertEquals(Some(true), c.booleanValue("b"))
    // historical behavior: a non-integer read as int falls back to None
    assertEquals(None, c.intValue("bad"))
  }

  @Test
  def missingDirectoryYieldsEmptyConfig(): Unit = {
    val config = ServicesConfig.load(java.nio.file.Paths.get("/nonexistent/services"))
    assertTrue(config.configs.isEmpty)
  }
}
