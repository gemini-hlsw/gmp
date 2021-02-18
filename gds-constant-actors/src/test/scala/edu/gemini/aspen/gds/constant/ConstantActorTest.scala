package edu.gemini.aspen.gds.constant

import edu.gemini.aspen.gds.api.Conversions._
import org.junit.Test
import org.junit.Assert._
import edu.gemini.aspen.giapi.data.ObservationEvent
import edu.gemini.aspen.gds.api.{ErrorCollectedValue, CollectionError, CollectedValue, GDSConfiguration}
import scala.collection._

class ConstantActorTest {

  @Test
  def testActor():Unit = {
    val constActor = new ConstantActor(buildConfiguration("KEY1", "val1") :: buildConfiguration("KEY2", "val2") :: Nil)
    assertEquals(CollectedValue("KEY1", "val1", "COMMENT", 0, None) :: CollectedValue("KEY2", "val2", "COMMENT", 0, None) :: Nil, constActor.collectValues())
  }

  @Test
  def testQuoteRemoval():Unit = {
    val constActor = new ConstantActor(GDSConfiguration("GPI",
        "OBS_START_ACQ",
        "KEY1",
        0,
        "STRING",
        false,
        "\"val1\"",
        "CONSTANT",
        "NONE",
        0,
        "",
      "COMMENT") :: GDSConfiguration("GPI",
        "OBS_START_ACQ",
        "KEY2",
        0,
        "STRING",
        false,
        """'val2'""",
        "CONSTANT",
        "NONE",
        0,
        "",
        "COMMENT")
      :: Nil)
    assertEquals(CollectedValue("KEY1", "val1", "COMMENT", 0, None) :: CollectedValue("KEY2", "val2", "COMMENT", 0, None) :: Nil, constActor.collectValues())
  }

  @Test
  def testActorWrongType():Unit = {
    val constActor = new ConstantActor(GDSConfiguration("GPI",
      "OBS_START_ACQ",
      "KEY1",
      0,
      "INT",
      false,
      "val1",
      "CONSTANT",
      "NONE",
      0,
      "",
      "COMMENT") :: Nil)
    assertEquals(ErrorCollectedValue("KEY1", CollectionError.TypeMismatch, "COMMENT", 0) :: Nil, constActor.collectValues())
  }

  @Test
  def testActorOfBooleanType():Unit = {
    val constActor = new ConstantActor(GDSConfiguration("GPI",
      "OBS_START_ACQ",
      "KEY1",
      0,
      "BOOLEAN",
      false,
      "true",
      "CONSTANT",
      "NONE",
      0,
      "",
      "COMMENT") :: Nil)
    assertEquals(CollectedValue("KEY1", true, "COMMENT", 0, None) :: Nil, constActor.collectValues())
  }

  @Test
  def testActorFactory():Unit = {
    val factory = new ConstantActorsFactory
    factory.configure(immutable.List(GDSConfiguration("GPI", "OBS_PREP", "TEST", 0, "DOUBLE", false, "1.0", "CONSTANT", "ws:massAirmass", 0, "", "my comment")))

    assertEquals(1, factory.buildActors(ObservationEvent.OBS_START_ACQ, "label").length)

    val actors = factory.buildActors(ObservationEvent.OBS_PREP, "label")
    assertEquals(1, actors.length)

    val values = actors.head.collectValues()
    values.head match {
      case CollectedValue(fits, value, comment, 0) => {
        assertEquals(stringToFitsKeyword("TEST"), fits)
        assertEquals(1.0, value)
        assertEquals("my comment", comment)
      }
      case _ => fail("Wrong answer")
    }
  }

  private def buildConfiguration(keyword: String, value: String): GDSConfiguration = {
    GDSConfiguration("GPI",
      "OBS_START_ACQ",
      keyword,
      0,
      "STRING",
      false,
      value,
      "CONSTANT",
      "NONE",
      0,
      "",
      "COMMENT")
  }
}