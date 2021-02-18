package edu.gemini.aspen.gds.actors

import org.junit.Assert._
import edu.gemini.aspen.gds.keywords.database.impl.KeywordsDatabaseImpl
import org.mockito.Mockito._
import edu.gemini.aspen.gds.api.{CollectedValue, KeywordValueActor, KeywordActorsFactory}
import edu.gemini.aspen.giapi.data.{ObservationEvent, DataLabel}
import actors.Futures
import org.junit.Test

class KeywordSetComposerTest {
  @Test
  def startAcqMessage():Unit = {
    val (dataLabel, composer) = createFixture

    // Send an init message
    val result = composer !! AcquisitionRequest(ObservationEvent.OBS_START_ACQ, dataLabel)

    result() match {
      case AcquisitionRequestReply(obsEvent, replyDataSet) => {
        assertEquals(replyDataSet, dataLabel)
        assertEquals(obsEvent, ObservationEvent.OBS_START_ACQ)
      }
      case _ => fail("Should not reply other message")
    }
  }

  @Test
  def endAcqMessage():Unit = {
    val (dataLabel, composer) = createFixture

    // Send an init message
    val result = composer !! AcquisitionRequest(ObservationEvent.OBS_END_ACQ, dataLabel)

    result() match {
      case AcquisitionRequestReply(obsEvent, replyDataSet) => {
        assertEquals(replyDataSet, dataLabel)
        assertEquals(obsEvent, ObservationEvent.OBS_END_ACQ)
      }
      case _ => fail("Should not reply other message")
    }
  }

  @Test
  def testNoActors():Unit = {
    // Generate datalabel
    val dataLabel = new DataLabel("GS-2011")

    val noActorsFactory = mock(classOf[KeywordActorsFactory])
    when(noActorsFactory.buildActors(ObservationEvent.OBS_END_READOUT, dataLabel)).thenReturn(Nil)

    val keywordsDatabase = new KeywordsDatabaseImpl()
    // Create composer
    val composer = new KeywordSetComposer(noActorsFactory, keywordsDatabase)

    // Send an init message
    val result = composer !! AcquisitionRequest(ObservationEvent.OBS_END_READOUT, dataLabel)

    result() match {
      case AcquisitionRequestReply(obsEvent, replyDataSet) => {
        assertEquals(replyDataSet, dataLabel)
        assertEquals(obsEvent, ObservationEvent.OBS_END_READOUT)
      }
      case _ => fail("Should not reply other message")
    }
  }

  @Test
  def testExceptionActor():Unit = {
    // Generate dataset
    val dataLabel = new DataLabel("GS-2011")

    val actorsFactory = mock(classOf[KeywordActorsFactory])
    when(actorsFactory.buildActors(ObservationEvent.OBS_END_READOUT, dataLabel)).thenReturn({
      new KeywordValueActor {
        override def collectValues(): List[CollectedValue[_]] = {
          throw new IllegalStateException
        }
      } :: Nil
    })

    val keywordsDatabase = new KeywordsDatabaseImpl()
    // Create composer
    val composer = new KeywordSetComposer(actorsFactory, keywordsDatabase)

    // Send an init message
    val result = composer !! AcquisitionRequest(ObservationEvent.OBS_END_READOUT, dataLabel)

    result() match {
      case AcquisitionRequestReply(obsEvent, replyDataSet) => {
        assertEquals(replyDataSet, dataLabel)
        assertEquals(obsEvent, ObservationEvent.OBS_END_READOUT)
      }
      case _ => fail("Should not reply other message")
    }
  }

  @Test
  def testExceptionActorFactory():Unit = {
    // Generate dataset
    val dataLabel = new DataLabel("GS-2011")

    val actorsFactory = mock(classOf[KeywordActorsFactory])
    when(actorsFactory.buildActors(ObservationEvent.OBS_END_READOUT, dataLabel)).thenThrow(new IllegalStateException())

    val keywordsDatabase = new KeywordsDatabaseImpl()
    // Create composer
    val composer = new KeywordSetComposer(actorsFactory, keywordsDatabase)

    // Send an init message
    val result = composer !! AcquisitionRequest(ObservationEvent.OBS_END_READOUT, dataLabel)
    val v = Futures awaitAll(500, result)
    assertEquals(1, v.size)
    v(0) match {
      case Some(AcquisitionRequestReply(obsEvent, replyDataSet)) => {
        assertEquals(replyDataSet, dataLabel)
        assertEquals(obsEvent, ObservationEvent.OBS_END_READOUT)
      }
      case Some(_) => fail("Should not reply other message")
      case None => fail("Should not timeout")
    }
  }

  private def createFixture = {
    // Generate dataset
    val dataLabel = new DataLabel("GS-2011")

    val dummyActorsFactory = new DummyActorsFactory()
    val keywordsDatabase = new KeywordsDatabaseImpl()
    // Create composer
    val composer = new KeywordSetComposer(dummyActorsFactory, keywordsDatabase)
    (dataLabel, composer)
  }
}

