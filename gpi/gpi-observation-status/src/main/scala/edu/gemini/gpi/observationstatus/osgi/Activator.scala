package edu.gemini.gpi.observationstatus.osgi

import edu.gemini.aspen.giapi.status.{StatusDatabaseService, StatusHandler}
import edu.gemini.aspen.giapi.status.setter.StatusSetter
import edu.gemini.epics.api.EpicsClient
import edu.gemini.epics.impl.EpicsObserverImpl
import edu.gemini.epics.{EpicsObserver, JCAContextController}
import edu.gemini.gmp.top.Top
import edu.gemini.gpi.observationstatus.{GpiItemTranslator, MassDimmTimetracker, ObservationEventsListener}
import edu.gemini.jms.api.JmsArtifact
import edu.gemini.util.osgi.Tracker._
import org.osgi.framework.{BundleActivator, BundleContext, ServiceRegistration}
import org.osgi.service.event.{EventConstants, EventHandler}
import org.osgi.util.tracker.ServiceTracker

import scala.jdk.CollectionConverters._
import scala.collection.mutable

class Activator extends BundleActivator {

  var translatorRegistration: Option[ServiceRegistration[_]] = None

  var obsEventsListenerTracker: Option[ServiceTracker[_, _]] = None

  var obsEventAdminRegistration: Option[ServiceRegistration[_]] = None

  var gmpTranslatorTracker: Option[ServiceTracker[_, _]] = None

  var jcaContextTracker: Option[ServiceTracker[_, _]] = None

  var epicsClient: Option[EpicsClient] = None

  override def start(context: BundleContext) = {
    gmpTranslatorTracker = Some(track[Top, StatusSetter, StatusHandler](context) { (top, ss) =>
        val translator = new GpiItemTranslator(top, ss)
        translatorRegistration = Some(context.registerService(Array[String](classOf[StatusHandler].getName, classOf[JmsArtifact].getName), translator, new java.util.Hashtable[String, String]()))
        translator
      } { _ =>

      })
    gmpTranslatorTracker.foreach(_.open())

    obsEventsListenerTracker = Some(track[Top, StatusSetter, StatusDatabaseService, ObservationEventsListener](context) { (top, ss, sdb) =>
        val listener = new ObservationEventsListener(top, ss, sdb)
        val props = mutable.Map(EventConstants.EVENT_TOPIC -> "edu/gemini/aspen/gds/gdsevent")
        obsEventAdminRegistration = Some(context.registerService(Array[String](classOf[EventHandler].getName), listener, props.asJavaDictionary))
        listener
      } { t =>
        t.invalidate()
      })
    obsEventsListenerTracker.foreach(_.open())

    jcaContextTracker = Some(track[Top, StatusSetter, JCAContextController, EpicsObserver](context) { (top, ss, jca) =>
        val observer = new EpicsObserverImpl(jca)

        val massDimmTracker = new MassDimmTimetracker(top, ss)
        epicsClient = Some(massDimmTracker)

        observer.registerEpicsClient(massDimmTracker, List(MassDimmTimetracker.massEpicsChannel, MassDimmTimetracker.dimmEpicsChannel).asJavaCollection)
        observer
      } { t =>
        epicsClient.foreach(t.unregisterEpicsClient)
        epicsClient = None
      })
    jcaContextTracker.foreach(_.open())
  }

  override def stop(context: BundleContext) = {
    translatorRegistration.foreach(_.unregister())
    translatorRegistration = None

    obsEventAdminRegistration.foreach(_.unregister())
    obsEventAdminRegistration = None

    gmpTranslatorTracker.foreach(_.close())
    gmpTranslatorTracker = None

    jcaContextTracker.foreach(_.close())
    jcaContextTracker = None

    epicsClient = None
  }
}
