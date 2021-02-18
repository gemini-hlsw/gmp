package edu.gemini.aspen.gds.staticheaderreceiver


import org.scalatest.junit.AssertionsForJUnit
import edu.gemini.aspen.gds.api.Conversions._
import org.junit.Test
import edu.gemini.aspen.gds.keywords.database.impl.ProgramIdDatabaseImpl
import edu.gemini.aspen.gds.staticheaderreceiver.TemporarySeqexecKeywordsDatabaseImpl.{Clean, Retrieve, Store}

import edu.gemini.aspen.gds.keywords.database.RetrieveProgramId
import org.scalatest.mock.MockitoSugar
import org.osgi.service.event.EventAdmin

class XmlRpcReceiverTest extends AssertionsForJUnit with MockitoSugar {
  val db = new TemporarySeqexecKeywordsDatabaseImpl
  val pdb = new ProgramIdDatabaseImpl
  val publisher = mock[EventAdmin]
  val xmlRpcReceiver = new XmlRpcReceiver(db, pdb, publisher)


  @Test
  def testDB():Unit = {
    db ! Store("label", "KEY", 1.asInstanceOf[AnyRef])
    db !?(1000, Retrieve("label", "KEY")) match {
      case Some(Some(1)) =>
      case _ => fail()
    }
    db !?(1000, Retrieve("wronglabel", "KEY")) match {
      case Some(None) =>
      case _ => fail()
    }
    db !?(1000, Retrieve("label", "WRONGKEY")) match {
      case Some(None) =>
      case _ => fail()
    }
    db ! Clean("label")
    db !?(1000, Retrieve("label", "KEY")) match {
      case Some(None) =>
      case _ => fail()
    }
    db ! Clean("wronglabel")
  }

  @Test
  def testXmlRpcReceiver():Unit = {
    xmlRpcReceiver.openObservation("id", "label")
    xmlRpcReceiver.storeKeyword("label", "KEY", 1)
    xmlRpcReceiver.storeKeywords("label2", ("KEY,INT,1" :: "KEY2,DOUBLE,1.0" :: "KEY3,STRING,uno" :: Nil).toArray)
    xmlRpcReceiver.closeObservation("label")
    db !?(1000, Retrieve("label", "KEY")) match {
      case Some(Some(1)) =>
      case _ => fail()
    }
    db !?(1000, Retrieve("label2", "KEY")) match {
      case Some(Some(1)) =>
      case _ => fail()
    }
    db !?(1000, Retrieve("label2", "KEY2")) match {
      case Some(Some(1.0)) =>
      case _ => fail()
    }
    db !?(1000, Retrieve("label2", "KEY3")) match {
      case Some(Some("uno")) =>
      case _ => fail()
    }
    pdb !?(1000, RetrieveProgramId("label")) match {
      case Some(Some("id")) =>
      case _ => fail()
    }
  }

  @Test
  def ignoreEmptyKeywords(): Unit = {
    xmlRpcReceiver.openObservation("id", "label3")
    xmlRpcReceiver.storeKeywords("label3", ("SCIBAND,INT,1" :: "REQIQ,STRING," :: Nil).toArray)
    xmlRpcReceiver.closeObservation("label3")
    db !?(1000, Retrieve("label3", "SCIBAND")) match {
      case Some(Some(1)) =>
      case _             => fail()
    }
    db !?(1000, Retrieve("label3", "REQIQ")) match {
      case Some(Some("")) =>
      case _              => fail()
    }
  }

  @Test
  def handleNewLinesInKeywords(): Unit = {
    xmlRpcReceiver.openObservation("id", "label3")
    xmlRpcReceiver.storeKeywords("label3", ("SCIBAND,INT,1" :: "REQIQ,STRING,D. Krogsrud -  L. Magill -  F. Rantakyro" :: Nil).toArray)
    xmlRpcReceiver.closeObservation("label3")
    db !?(1000, Retrieve("label3", "SCIBAND")) match {
      case Some(Some(1)) =>
      case _             => fail()
    }
    db !?(1000, Retrieve("label3", "REQIQ")) match {
      case Some(Some("D. Krogsrud -  L. Magill -  F. Rantakyro")) =>
      case x              => println(x);fail()
    }
  }

}