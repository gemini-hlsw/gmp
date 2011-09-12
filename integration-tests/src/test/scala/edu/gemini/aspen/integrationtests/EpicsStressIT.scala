package edu.gemini.aspen.integrationtests

import edu.gemini.cas.db.ChannelBuilder
import org.junit.Assert.{assertEquals, assertNotNull}
import gov.aps.jca.JCALibrary
import com.cosylab.epics.caj.CAJContext
import edu.gemini.cas.impl.ChannelAccessServerImpl
import edu.gemini.epics.impl.EpicsReaderImpl
import edu.gemini.epics.EpicsService
import org.junit.{Before, Test}
import org.junit.Assert._
import scala.actors.Actor._
import java.util.concurrent._

class EpicsStressIT {
  var jca: JCALibrary = _
  var context: CAJContext = _
  var reader: EpicsReaderImpl = _

  @Before
  def setup() {
    jca = JCALibrary.getInstance
    context = jca.createContext(JCALibrary.CHANNEL_ACCESS_JAVA).asInstanceOf[CAJContext]
    reader = new EpicsReaderImpl(new EpicsService(context))
  }

  @Test
  def testConcurrentRead() {
    val testFile = this.getClass.getResource("cas500channels.xml").toURI.toURL.getFile
    val channels = new ChannelBuilder(testFile).channels
    val Nactors = 10
    val Niter = 100
    val latch = new CountDownLatch(Nactors)

    assertNotNull(channels)
    assertEquals(501, channels.size)

    for (j <- 1 to Nactors) {
      actor {
        for (k <- 1 to Niter) {
          for (i <- 0 until 500) {
            assertEquals("gpi:E" + i, channels(i).getName)
            assertEquals(Double.box(i), channels(i).getFirst)
          }
        }
        latch.countDown()
      }
    }
    if (!latch.await(Nactors, TimeUnit.SECONDS)) {
      fail("Some actors didn't finish")
    }
  }

  @Test
  def testConcurrentReadWithEpicsReader() {
    val testFile = this.getClass.getResource("cas500channels.xml").toURI.toURL.getFile
    val channels = new ChannelBuilder(testFile).channels
    val Nactors = 5
    val Niter = 2

    assertNotNull(channels)
    assertEquals(501, channels.size)
    val latch = new CountDownLatch(Nactors)

    for (j <- 1 to Nactors) {
      actor {
        val context = jca.createContext(JCALibrary.CHANNEL_ACCESS_JAVA).asInstanceOf[CAJContext]
        val reader = new EpicsReaderImpl(new EpicsService(context))
        for (i <- 0 until 500) {
          reader.bindChannel(channels(i).getName)
        }
        for (k <- 1 to Niter) {
          for (i <- 0 until 500) {
            assertEquals(Double.box(i), (reader.getValue("gpi:E" + i).asInstanceOf[Array[Double]])(0))
          }
        }
        latch.countDown()
      }
    }
    if (!latch.await(10 * Nactors, TimeUnit.SECONDS)) {
      fail("Some actors didn't finish")
    }
  }
}