package edu.gemini.aspen.gmp.services.osgi

import java.util

import edu.gemini.aspen.gmp.services.PropertyHolder
import edu.gemini.aspen.gmp.services.core.Service
import edu.gemini.aspen.gmp.services.jms.{JmsService, RequestConsumer}
import edu.gemini.aspen.gmp.services.properties.{PropertyService, SimplePropertyHolder}
import edu.gemini.jms.api.JmsArtifact
import edu.gemini.util.osgi.Tracker
import org.osgi.framework.{BundleActivator, BundleContext, ServiceRegistration}
import org.osgi.service.cm.ManagedServiceFactory
import org.osgi.util.tracker.ServiceTracker

class Activator extends BundleActivator {
  var managedServiceFactory: Option[ServiceRegistration[_]] = None
  var trackerPH: Option[ServiceTracker[PropertyHolder, JmsService]] = None
  var trackerS: Option[ServiceTracker[Service, JmsArtifact]] = None
  var psService: Option[ServiceRegistration[_]] = None
  var rcService: Option[ServiceRegistration[_]] = None

  override def start(context: BundleContext): Unit = {
    val props: util.Hashtable[String, String] = new util.Hashtable()
    props.put("service.pid", classOf[SimplePropertyHolder].getName)
    managedServiceFactory = Option(context.registerService(classOf[ManagedServiceFactory].getName, new SimplePropertyHolderFactory(context), props))
    trackerPH = Option(Tracker.track[PropertyHolder, JmsService](context) { p =>
      val ps = new PropertyService(p)
      val props: util.Hashtable[String, String] = new util.Hashtable()
      psService = Option(context.registerService(Array[String](classOf[Service].getName, classOf[JmsService].getName, classOf[JmsArtifact].getName), ps, props))
      ps
    } { _ => psService.foreach(_.unregister())})
    trackerPH.foreach(_.open)
    trackerS = Option(Tracker.track[Service, JmsArtifact](context) { p =>
      val rq = new RequestConsumer(p)
      val props: util.Hashtable[String, String] = new util.Hashtable()
      rcService = Option(context.registerService(classOf[JmsArtifact].getName, rq, props))
      rq
    } { _ => ()})
    trackerS.foreach(_.open)
  }

  override def stop(context: BundleContext): Unit = {
    managedServiceFactory.foreach(s => s.unregister())
    managedServiceFactory = None
    trackerPH.foreach(_.close())
    trackerPH = None
    trackerS.foreach(_.close())
    trackerS = None
  }
}
