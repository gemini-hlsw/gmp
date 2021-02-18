package edu.gemini.aspen.gds.staticheaderreceiver

import org.junit.Test
import org.junit.Assert._
import edu.gemini.aspen.gds.staticheaderreceiver.TemporarySeqexecKeywordsDatabaseImpl.{RetrieveAll, Retrieve, Store}
import java.util.concurrent.TimeUnit
import edu.gemini.aspen.giapi.data.DataLabel
import edu.gemini.aspen.gds.api.fits.FitsKeyword

class TemporarySeqexecKeywordsDatabaseImplTest {
  val dataLabel = new DataLabel("GS2010-1")
  val keyword = new FitsKeyword("KEY")
  val originalValue = "VAL"

  @Test
  def testStoreAndRetrieve():Unit = {
    // Verify we can store and retrieve
    val db = new TemporarySeqexecKeywordsDatabaseImpl
    db ! Store(dataLabel, keyword, originalValue)

    val value = db !? (100, Retrieve(dataLabel, keyword))
    value match {
      case Some(Some(x)) => assertEquals(originalValue, x)
      case _ => fail()
    }

    val noValue = db !? (100, Retrieve(new DataLabel("label"), keyword))
    noValue match {
      case Some(None) =>
      case Some(x) => fail()
      case None => fail()
    }
  }

  @Test
  def testStoreAndRetrieveUnknown():Unit = {
    // Verify we can store and retrieve
    val db = new TemporarySeqexecKeywordsDatabaseImpl
    db ! Store(dataLabel, keyword, originalValue)

    val value = db !? (100, Retrieve(new DataLabel("label"), keyword))
    value match {
      case Some(None) => // we are ok
      case _ => fail()
    }
  }

  @Test
  def testStoreAndRetrieveAll():Unit = {
    val db = new TemporarySeqexecKeywordsDatabaseImpl
    db ! Store(dataLabel, keyword, originalValue)

    val value = db !? (100, RetrieveAll(dataLabel))
    value match {
      case Some(x: Map[FitsKeyword, AnyRef]) =>
        assertEquals(1, x.size)
        assertEquals(originalValue, x(keyword))
      case _ => fail()
    }
  }

  @Test
  def testExpiration():Unit = {
    val db = new TemporarySeqexecKeywordsDatabaseImpl {
      override def expirationMillis = 1
    }
    db ! Store(dataLabel, keyword, originalValue)

    TimeUnit.MILLISECONDS.sleep(5)

    val value = db !? (100, Retrieve(dataLabel, keyword))
    value match {
      case Some(None) => // we are ok
      case _ => fail()
    }
  }
}