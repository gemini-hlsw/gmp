package edu.gemini.aspen.gmp.tcs.osgi

import java.util
import edu.gemini.epics.EpicsReader
import edu.gemini.jms.api.JmsArtifact
import edu.gemini.util.osgi.Tracker
import org.osgi.framework.{BundleActivator, BundleContext, ServiceRegistration}
import org.osgi.service.cm.ManagedServiceFactory
import org.osgi.util.tracker.ServiceTracker
import edu.gemini.aspen.gmp.tcs.model.TcsContextComponent

class Activator extends BundleActivator {
  var trackerTop: Option[ServiceTracker[EpicsReader, TcsContextComponentFactory]] = None
  var psService: Option[ServiceRegistration[_]] = None

  override def start(context: BundleContext): Unit = {
    trackerTop = Option(Tracker.track[EpicsReader, TcsContextComponentFactory](context) { p =>
      val ps = new TcsContextComponentFactory(context, p)
      val props: util.Hashtable[String, String] = new util.Hashtable()
      props.put("service.pid", classOf[TcsContextComponent].getName)
      psService = Option(context.registerService(classOf[ManagedServiceFactory].getName, ps, props))
      ps
    } { p =>
        p.stopServices()
        psService.foreach(_.unregister())
        psService = None
    })
    trackerTop.foreach(_.open)
  }

  override def stop(context: BundleContext): Unit = {
    trackerTop.foreach(_.close())
    trackerTop = None
  }
}
