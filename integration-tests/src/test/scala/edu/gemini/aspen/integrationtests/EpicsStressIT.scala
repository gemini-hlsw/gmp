package edu.gemini.aspen.integrationtests

import edu.gemini.cas.db.ChannelBuilder
import org.junit.Assert.{assertEquals, assertNotNull}
import gov.aps.jca.JCALibrary
import com.cosylab.epics.caj.CAJContext
import org.junit.Assert._
import scala.actors.Actor._
import java.util.concurrent._
import org.junit.{Before, Test}
import edu.gemini.epics.impl.EpicsReaderImpl
import edu.gemini.epics.{ReadOnlyClientEpicsChannel, EpicsReader, EpicsService}

class EpicsStressIT {
  var jca: JCALibrary = _
  var context: CAJContext = _
  var reader: EpicsReader = _

  @Before
  def setup():Unit = {
    System.setProperty("com.cosylab.epics.caj.CAJContext.addr_list", "127.0.0.1")
    System.setProperty("com.cosylab.epics.caj.CAJContext.auto_addr_list", "false")

    jca = JCALibrary.getInstance
    context = jca.createContext(JCALibrary.CHANNEL_ACCESS_JAVA).asInstanceOf[CAJContext]
    reader = new EpicsReaderImpl(new EpicsService(context))
  }

  @Test
  def testConcurrentRead():Unit = {
    val testFile = this.getClass.getResource("cas501channels.xml").toURI.toURL.getFile
    val channels = new ChannelBuilder(testFile).channels
    val Nactors = 10
    val Niter = 100
    val latch = new CountDownLatch(Nactors)

    assertNotNull(channels)
    assertEquals(501, channels.size)

    for (j <- 1 to Nactors) {
      actor {
        for (k <- 1 to Niter) {
          for (i <- 0 to 500) {
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
  def testConcurrentReadWithEpicsReader():Unit = {
    val testFile = this.getClass.getResource("cas501channels.xml").toURI.toURL.getFile
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
        var epicsChannels:List[(Int,ReadOnlyClientEpicsChannel[Double])] = Nil
        for (i <- 0 to 500) {
          epicsChannels :+ (i,reader.getDoubleChannel(channels(i).getName))
        }
        for (k <- 1 to Niter) {
          epicsChannels foreach { case (i:Int,ch:ReadOnlyClientEpicsChannel[Double]) => assertEquals(Double.box(i),ch.getFirst)}
        }
        latch.countDown()
      }
    }
    if (!latch.await(10 * Nactors, TimeUnit.SECONDS)) {
      fail("Some actors didn't finish. Missing: " + latch.getCount)
    }
  }
}