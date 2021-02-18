package edu.gemini.gmp.status.translator

import edu.gemini.aspen.giapi.status.Health
import edu.gemini.aspen.giapi.status.StatusItem
import edu.gemini.aspen.giapi.status.impl.BasicStatus
import edu.gemini.aspen.giapi.status.impl.HealthStatus
import edu.gemini.aspen.giapi.statusservice.StatusHandlerAggregate
import edu.gemini.aspen.gmp.statusdb.StatusDatabase
import edu.gemini.gmp.top.TopImpl
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._

/**
 * Class InMemoryStatusItemTranslatorTest
 */
class InMemoryStatusItemTranslatorTest {
  @Test def testSimpleConfiguration():Unit = {
    val top = new TopImpl("gpi", "gpi")
    val file = getClass.getResource("status-translator.xml").getFile
    val db = new StatusDatabase
    val aggregate = mock(classOf[StatusHandlerAggregate])
    val translator = new InMemoryStatusItemTranslator(top, aggregate, db, file)
    translator.start
    verifyNoInteractions(aggregate)
  }

  @Test def testTranslations():Unit = {
    val top = new TopImpl("gpi", "gpi")
    val file = getClass.getResource("status-translator.xml").getFile
    val db = new StatusDatabase
    val aggregate = mock(classOf[StatusHandlerAggregate])
    val translator = new InMemoryStatusItemTranslator(top, aggregate, db, file)
    translator.start
    val translated = translator.translate(new BasicStatus[Int](top.buildStatusItemName("old"), 0))
    assertFalse(translated.isEmpty)
    val good = new HealthStatus(top.buildStatusItemName("new"), Health.GOOD)
    assertEquals(good.getName, translated(0).getName)
    assertEquals(good.getValue, translated(0).getValue)
  }

  @Test def testMultipleTranslationsSameOrigin():Unit = {
    val top = new TopImpl("gpi", "gpi")
    val file = getClass.getResource("status-translator.xml").getFile
    val db = new StatusDatabase
    val aggregate = mock(classOf[StatusHandlerAggregate])
    val translator = new InMemoryStatusItemTranslator(top, aggregate, db, file)
    translator.start
    val translated = translator.translate(new BasicStatus[Int](top.buildStatusItemName("twice"), 0))
    assertEquals(2, translated.size)
    val first = new HealthStatus(top.buildStatusItemName("second"), Health.GOOD)
    assertEquals(first.getName, translated(1).getName)
    assertEquals(first.getValue, translated(1).getValue)
    val second = new HealthStatus(top.buildStatusItemName("first"), Health.GOOD)
    assertEquals(second.getName, translated(0).getName)
    assertEquals(second.getValue, translated(0).getValue)
  }

  @Test def testMultipleOriginsSameTranslation():Unit = {
    val top = new TopImpl("gpi", "gpi")
    val file = getClass.getResource("status-translator.xml").getFile
    val db = new StatusDatabase
    val aggregate = mock(classOf[StatusHandlerAggregate])
    val translator = new InMemoryStatusItemTranslator(top, aggregate, db, file)
    translator.start
    val translated = (translator.translate(new BasicStatus[Int](top.buildStatusItemName("original1"), 0)) :: translator.translate(new BasicStatus[Int](top.buildStatusItemName("original2"), 0)) :: Nil).flatten
    assertEquals(2, translated.size)
    val first = new HealthStatus(top.buildStatusItemName("target"), Health.GOOD)
    assertEquals(first.getName, translated(0).getName)
    assertEquals(first.getValue, translated(0).getValue)
    val second = new HealthStatus(top.buildStatusItemName("target"), Health.BAD)
    assertEquals(second.getName, translated(1).getName)
    assertEquals(second.getValue, translated(1).getValue)
  }

  @Test def testTranslationUpdate():Unit = {
    val top = new TopImpl("gpi", "gpi")
    val file = getClass.getResource("status-translator.xml").getFile
    val db = new StatusDatabase
    val aggregate = mock(classOf[StatusHandlerAggregate])
    val translator = new InMemoryStatusItemTranslator(top, aggregate, db, file)
    translator.start
    val good = new HealthStatus(top.buildStatusItemName("new"), Health.GOOD)
    translator.update(new BasicStatus[Int](top.buildStatusItemName("old"), 0))
    verify(aggregate).update(any(classOf[StatusItem[_]]))
  }

  @Test def testTranslationTwiceUpdate():Unit = {
    val top = new TopImpl("gpi", "gpi")
    val file = getClass.getResource("status-translator.xml").getFile
    val db = new StatusDatabase
    val aggregate = mock(classOf[StatusHandlerAggregate])
    val translator = new InMemoryStatusItemTranslator(top, aggregate, db, file)
    translator.start
    translator.update(new BasicStatus[Int](top.buildStatusItemName("twice"), 0))
    verify(aggregate, times(2)).update(any(classOf[StatusItem[_]]))
  }

  @Test def testTranslateOnStart():Unit = {
    val top = new TopImpl("gpi", "gpi")
    val file = getClass.getResource("status-translator.xml").getFile
    val db = new StatusDatabase
    val aggregate = mock(classOf[StatusHandlerAggregate])
    val original = new BasicStatus[Int](top.buildStatusItemName("old"), 3)
    val original2 = new BasicStatus[Int](top.buildStatusItemName("oldone"), 3)
    db.update(original)
    db.update(original2)
    val translator = new InMemoryStatusItemTranslator(top, aggregate, db, file)
    translator.start
    verify(aggregate, times(1)).update(any(classOf[StatusItem[_]]))
  }

  @Test def testTranslateString():Unit = {
    val top = new TopImpl("gpi", "gpi")
    val file = getClass.getResource("status-translator.xml").getFile
    val db = new StatusDatabase
    val aggregate = mock(classOf[StatusHandlerAggregate])
    val original = new BasicStatus[String](top.buildStatusItemName("str"), "A")
    val translator = new InMemoryStatusItemTranslator(top, aggregate, db, file)
    translator.start
    assertEquals("gpi:target", translator.translate(original).head.getName)
    assertEquals("B", translator.translate(original).head.getValue)

    val another = new BasicStatus[String](top.buildStatusItemName("str"), "C")
    assertEquals("gpi:target", translator.translate(another).head.getName)
    assertEquals("D", translator.translate(another).head.getValue)

    translator.update(original)
    verify(aggregate, times(1)).update(any(classOf[StatusItem[_]]))
  }

  @Test def testTranslateStringDefault():Unit = {
    val top = new TopImpl("gpi", "gpi")
    val file = getClass.getResource("status-translator.xml").getFile
    val db = new StatusDatabase
    val aggregate = mock(classOf[StatusHandlerAggregate])
    val original = new BasicStatus[String](top.buildStatusItemName("str"), "ABC")
    val translator = new InMemoryStatusItemTranslator(top, aggregate, db, file)
    translator.start
    assertEquals("gpi:target", translator.translate(original).head.getName)
    assertEquals("UNKNOWN", translator.translate(original).head.getValue)
    translator.update(original)
    verify(aggregate, times(1)).update(any(classOf[StatusItem[_]]))
  }
}