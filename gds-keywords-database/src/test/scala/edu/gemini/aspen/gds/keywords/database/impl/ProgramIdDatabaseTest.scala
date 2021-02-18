package edu.gemini.aspen.gds.keywords.database.impl

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Test
import edu.gemini.aspen.gds.api.Conversions._
import scala.Some
import edu.gemini.aspen.gds.keywords.database.{RetrieveProgramId, StoreProgramId}
import java.util.concurrent.TimeUnit

class ProgramIdDatabaseTest extends AssertionsForJUnit {
  @Test
  def testBasic():Unit = {
    val db = new ProgramIdDatabaseImpl
    db ! StoreProgramId("label", "id")

    (db !? (1000, RetrieveProgramId("label"))) match {
      case Some(Some("id")) =>
      case _ => fail()
    }
  }

  @Test
  def testExpiration():Unit = {
    val db = new ProgramIdDatabaseImpl {
      override def expirationMillis = 5
    }

    db ! StoreProgramId("label", "id")

    // Sleep a bit
    TimeUnit.MILLISECONDS.sleep(50)

    (db !? (1000, RetrieveProgramId("label"))) match {
      case Some(None) => // we are ok
      case _ => fail()
    }
  }

}