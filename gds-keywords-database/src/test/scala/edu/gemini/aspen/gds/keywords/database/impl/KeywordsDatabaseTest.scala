package edu.gemini.aspen.gds.keywords.database.impl

import org.scalatest.junit.AssertionsForJUnit
import edu.gemini.aspen.gds.api.CollectedValue
import org.junit.{Before, Test}
import org.junit.Assert._
import edu.gemini.aspen.gds.api.Conversions._
import edu.gemini.aspen.giapi.data.DataLabel
import edu.gemini.aspen.gds.keywords.database._
import scala._
import java.util.concurrent.TimeUnit

class KeywordsDatabaseTest extends AssertionsForJUnit {
  var db: KeywordsDatabaseImpl = null
  val key = "KEYWORD"
  val key2 = "KEYWORD2"
  val value = 0.1
  val colVal = CollectedValue(key, value, "my comment", 0, None)
  val colVal2 = CollectedValue(key2, value, "my comment", 1, None)
  val dataLabel = "GS-2011B"
  val dataLabel2 = "GS-2011A"

  @Before
  def setup():Unit = {
    db = new KeywordsDatabaseImpl
  }

  def check(label: DataLabel, item: CollectedValue[_]):Unit = {
    val ret = db !?(1000, Retrieve(label))
    assertFalse(ret.isEmpty) //we didn't get a timeout
    ret foreach {
      _ match {
        case cv: List[_] => assertTrue(cv.contains(item))
        case a: Any => fail("Not valid value" + a)
      }
    }
  }

  @Test
  def testMethods():Unit = {
    db.store(dataLabel, colVal)
    val ret = db.retrieve(dataLabel)
    assertEquals(1, ret.size)
    assertEquals(colVal, ret(0))
  }

  @Test
  def testRetrieveAll():Unit = {
    db ! Store(dataLabel, colVal)
    check(dataLabel, colVal)
  }


  def checkEmpty(ret: Option[Any]):Unit = {
    assertFalse(ret.isEmpty)
    ret map {
      case c: List[_] => assertTrue(c.isEmpty)
      case a: Any => sys.error("Not a valid value " + a)
    }
  }

  @Test
  def retrieveEmpty():Unit = {
    val ret = db !?(1000, Retrieve(dataLabel))
    checkEmpty(ret)
  }

  @Test
  def retrieveWrongDataLabel():Unit = {
    db ! Store(dataLabel, colVal)
    val ret = db !?(1000, Retrieve("wrong"))
    checkEmpty(ret)
  }

  @Test
  def multipleDatasets():Unit = {
    db ! Store(dataLabel, colVal)
    db ! Store(dataLabel2, colVal2)

    check(dataLabel, colVal)

    check(dataLabel2, colVal2)
  }

  @Test
  def multipleDatasetsInList():Unit = {
    db ! StoreList(dataLabel, List(colVal, colVal2))

    check(dataLabel, colVal)

    check(dataLabel, colVal2)
  }

  @Test
  def testClean():Unit = {
    db !?(1000, Retrieve(dataLabel))
    db ! Clean(dataLabel)
    retrieveEmpty()
  }

  @Test
  def testExpiration():Unit = {
    val db = new KeywordsDatabaseImpl {
      override def expirationMillis = 5
    }

    db ! Store(dataLabel, colVal)

    // Sleep a bit
    TimeUnit.MILLISECONDS.sleep(10)

    val ret = db !?(1000, Retrieve(dataLabel))
    assertFalse(ret.isEmpty) //we didn't get a timeout
    ret match {
      case Some(x:List[_]) => // we are ok
      case x: Any => println(x); fail()
    }
  }
}