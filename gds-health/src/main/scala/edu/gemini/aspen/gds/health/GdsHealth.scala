package edu.gemini.aspen.gds.health

import java.util.logging.Logger

import edu.gemini.aspen.gds.api.{GDSObseventHandler, KeywordActorsFactory, KeywordSource}
import edu.gemini.aspen.giapi.status.Health
import edu.gemini.aspen.giapi.status.impl.{BasicStatus, HealthStatus}
import edu.gemini.aspen.giapi.status.setter.StatusSetter
import edu.gemini.gmp.top.Top
import edu.gemini.jms.api.{JmsArtifact, JmsProvider}

import scala.actors.Actor
import scala.actors.threadpool.TimeUnit
import scala.collection.mutable.ListBuffer

case object UpdateHealth
case object Connected

/**
 * OSGi component providing health information for the GDS
 */
class GdsHealth(top: Top, setter: StatusSetter) extends JmsArtifact {
  private val LOG: Logger = Logger.getLogger(this.getClass.getName)

  private val healthName = top.buildStatusItemName("gds:health")
  private val healthMessageName = top.buildStatusItemName("gds:health:message")

  private val healthState = new HealthState(LOG)
  private val stateActor = new StateActor()
  stateActor.start()

  private def updateHealth():Unit = {
    LOG.info(s"Updating Health to ${healthState.getHealth} on $healthName")
    stateActor ! UpdateHealth
  }

  def stopJms():Unit = {}

  def startJms(provider: JmsProvider):Unit = {
    LOG.info("Start GDS Health")
    stateActor ! Connected
  }

  def bindHeaderReceiver():Unit = {
    LOG.fine("Binding HeaderReceiver")
    healthState.registerHeaderReceiver()
    updateHealth()
  }

  def unbindHeaderReceiver():Unit = {
    LOG.fine("Unbinding HeaderReceiver")
    healthState.unregisterHeaderReceiver()
    updateHealth()
  }

  def bindGDSObseventHandler(evtHndlr: GDSObseventHandler):Unit = {
    LOG.fine("Binding GDSObseventHandlerImpl")
    healthState.registerGDSObseventHandler()
    updateHealth()
  }

  def unbindGDSObseventHandler(evtHndlr: GDSObseventHandler):Unit = {
    LOG.fine("Unbinding GDSObseventHandlerImpl")
    healthState.unregisterGDSObseventHandler()
    updateHealth()
  }

  def bindActorFactory(fact: KeywordActorsFactory):Unit = {
    LOG.fine("Binding ActorsFactory: " + fact.getClass.getName)
    healthState.registerActorFactory(fact.getSource)
    updateHealth()
  }

  def unbindActorFactory(fact: KeywordActorsFactory):Unit = {
    LOG.fine("Unbinding ActorsFactory: " + fact.getClass.getName)
    healthState.unregisterActorFactory(fact.getSource)
    updateHealth()
  }

  class StateActor extends Actor {
    var connected = false

    override def act():Unit = {
      loop {
        react {
          case Connected                 =>
              TimeUnit.SECONDS.sleep(1)
              LOG.info("GDS Health Connected")
              connected = true
              updateHealthValues()
          case UpdateHealth if connected =>
            updateHealthValues()
          case UpdateHealth              =>
        }
      }
    }

    def updateHealthValues():Unit = {
      setter.setStatusItem(new HealthStatus(healthName, healthState.getHealth))
      setter.setStatusItem(new BasicStatus[String](healthMessageName, healthState.getMessage))
      LOG.info("GDS health SET " + healthState)
    }
  }

  private class HealthState(LOG: Logger) {

    private val actors = ListBuffer[Boolean](false, false, true /* ODB is disabled*/, false, false, false) //Booleans are initialized to false

    private var obsEvtHndl = false
    private var headerRec = false

    def registerHeaderReceiver():Unit = {
      headerRec = true
    }

    def unregisterHeaderReceiver():Unit = {
      headerRec = false
    }

    def registerGDSObseventHandler():Unit = {
      obsEvtHndl = true
    }

    def unregisterGDSObseventHandler():Unit = {
      obsEvtHndl = false
    }

    def registerActorFactory(source: KeywordSource.Value):Unit = {
      source match {
        case KeywordSource.NONE => LOG.fine(s"Registered KeywordActorsFactory of unknown type: $source")
        case x => actors(x.id) = true
      }
    }

    def unregisterActorFactory(source: KeywordSource.Value):Unit = {
      source match {
        case KeywordSource.NONE => LOG.fine(s"Unregistered KeywordActorsFactory of unknown type: $source")
        case x => actors(x.id) = false
      }
    }

    def getHealth: Health = {
      if (obsEvtHndl) {
        if (actors.reduceLeft({
          _ && _
        }) && headerRec) {
          Health.GOOD
        } else {
          Health.WARNING
        }
      } else {
        Health.BAD
      }
    }

    def getMessage: String = {
      getHealth match {
        case Health.GOOD => ""
        case _ => "Missing components: " +
          (if (!headerRec) "HeaderReceiver, " else "") +
          (if (!obsEvtHndl) "ObservationEventHandler, " else "") +
          (actors.indices collect {
            case x if !actors(x) => KeywordSource(x).toString
          } reduceLeftOption {
            _ + ", " + _
          } map (x => "Actor factories: " + x) getOrElse "")
      }
    }
  }

}