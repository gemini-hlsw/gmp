package edu.gemini.aspen.gmp.tcsoffset.osgi

import java.util
import edu.gemini.epics.{EpicsObserver, EpicsReader, EpicsWriter}
import edu.gemini.util.osgi.Tracker
import org.osgi.framework.{BundleActivator, BundleContext, ServiceRegistration}
import org.osgi.service.cm.ManagedServiceFactory
import org.osgi.util.tracker.ServiceTracker

import java.util.logging.Logger
import edu.gemini.aspen.gmp.tcsoffset.model.TcsOffsetComponent

/**
 * Activator class is the responsible for implementing the start and stop
 * of the gmp-tcs-offset bundle.
 */
class Activator extends BundleActivator {
  private val logger = Logger.getLogger(this.getClass.getName)
  var trackerTop: Option[ServiceTracker[EpicsWriter, _]] = None
  var psService: Option[ServiceRegistration[_]] = None

  override def start(context: BundleContext): Unit = {
    logger.fine("TCS Offset bundle Starting")
    trackerTop = Option(Tracker.track[EpicsWriter, EpicsObserver, TcsOffsetComponentFactory](context) { (ew, eo) =>
    val ps = new TcsOffsetComponentFactory(context, ew, eo)
    val props: util.Hashtable[String, String] = new util.Hashtable()
    props.put("service.pid", classOf[TcsOffsetComponent].getName)
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
