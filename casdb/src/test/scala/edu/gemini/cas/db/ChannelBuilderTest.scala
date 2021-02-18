package edu.gemini.cas.db

import org.junit.Test
import org.junit.Assert._

class ChannelBuilderTest {
  @Test
  def testParser():Unit = {
    val testFile = classOf[ChannelBuilder].getResource("channels.xml").toURI.toURL.getFile
    val channels = new ChannelBuilder(testFile).channels

    assertNotNull(channels)
    assertEquals(5, channels.size)
    assertEquals("gpi:E1", channels(0).getName)
    assertEquals(Double.box(10), channels(0).getFirst)
    assertEquals("gpi:E2", channels(1).getName)
    assertEquals(Int.box(20), channels(1).getFirst)
  }
}