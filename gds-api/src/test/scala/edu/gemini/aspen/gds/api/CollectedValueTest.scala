package scala.edu.gemini.aspen.gds.api

import org.junit.Test
import org.junit.Assert._
import edu.gemini.aspen.gds.api.Conversions._
import edu.gemini.aspen.gds.api.{CollectionError, ErrorCollectedValue, CollectedValue}
import scala.collection._
import edu.gemini.aspen.gds.api.fits.FitsKeyword

class CollectedValueTest {

  @Test
  def testBuilding():Unit = {
    // Build with string
    assertNotNull(CollectedValue("KEYWORD", "strValue", "comment", 0, None))
    // Build with int
    assertNotNull(CollectedValue("KEYWORD", 1, "comment", 0, None))
    // Build with double
    assertNotNull(CollectedValue("KEYWORD", 1.1, "comment", 0, None))
    // Note this cannot compile
    //assertNotNull(CollectedValue("KEYWORD", new Date(), "comment", 0))
  }

  @Test
  def testPatternMatchingString():Unit = {
    // Build with string
    val cv = CollectedValue("KEYWORD", "strValue", "comment", 0, None)
    cv match {
      case CollectedValue(keyword, value, comment, index) => {
        assertEquals(new FitsKeyword("KEYWORD"), keyword)
        assertEquals("strValue", value)
        assertEquals("comment", comment)
        assertEquals(0, index)
      }
      case _ => fail("Should match")
    }
  }

  @Test
  def testPatternMatchingInteger():Unit = {
    // Build with string
    val cv = CollectedValue("KEYWORD", 99, "comment", 0, None)
    cv match {
      case CollectedValue(keyword, value, comment, index) => {
        assertEquals(new FitsKeyword("KEYWORD"), keyword)
        assertEquals(99, value)
        assertEquals("comment", comment)
        assertEquals(0, index)
      }
      case _ => fail("Should match")
    }
  }

  @Test
  def testPatternMatchingDouble():Unit = {
    // Build with string
    val cv = CollectedValue("KEYWORD", 1.1, "comment", 0, None)
    cv match {
      case CollectedValue(keyword, value, comment, index) => {
        assertEquals(new FitsKeyword("KEYWORD"), keyword)
        assertEquals(1.1, value, 0)
        assertEquals("comment", comment)
        assertEquals(0, index)
      }
      case _ => fail("Should match")
    }
  }

  @Test
  def testPatternMatchingBoolean():Unit = {
    // Build with string
    val cv = CollectedValue("KEYWORD", false, "comment", 0, None)
    cv match {
      case CollectedValue(keyword, value, comment, index) => {
        assertEquals(new FitsKeyword("KEYWORD"), keyword)
        assertEquals(false, value)
        assertEquals("comment", comment)
        assertEquals(0, index)
      }
      case _ => fail("Should match")
    }
  }

  @Test
  def testPatternMatchingInList():Unit = {
    // Build with string
    val cv = CollectedValue("KEYWORD", 1.1, "comment", 0, None)
    val list = immutable.List(cv)
    list match {
      case CollectedValue(keyword, value, comment, index) :: Nil => {
        assertEquals(new FitsKeyword("KEYWORD"), keyword)
        assertEquals(1.1, value, 0)
        assertEquals("comment", comment)
        assertEquals(0, index)
      }
      case _ => fail("Should match")
    }
  }

  @Test
  def testPatternMatchingError():Unit = {
    // Build with string
    val ev = ErrorCollectedValue("KEYWORD", CollectionError.MandatoryRequired, "comment", 0)
    assertTrue(ev.isError)
    ev match {
      case ErrorCollectedValue(keyword, CollectionError.MandatoryRequired, comment, index) => {
        assertEquals(new FitsKeyword("KEYWORD"), keyword)
        assertEquals("comment", comment)
        assertEquals(0, index)
      }
      case _ => fail("Should match")
    }

    ev match {
      case ErrorCollectedValue(keyword, error, comment, index) => {
        assertEquals(new FitsKeyword("KEYWORD"), keyword)
        assertEquals(CollectionError.MandatoryRequired, error)
        assertEquals("comment", comment)
        assertEquals(0, index)
      }
      case _ => fail("Should match")
    }
  }

}