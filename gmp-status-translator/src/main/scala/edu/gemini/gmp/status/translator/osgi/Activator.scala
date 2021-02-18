package edu.gemini.gmp.status.translator.osgi

import edu.gemini.aspen.giapi.status.StatusHandler
import edu.gemini.aspen.giapi.statusservice.StatusHandlerAggregate
import edu.gemini.gmp.status.translator.LocalStatusItemTranslator
import edu.gemini.gmp.top.Top
import edu.gemini.util.osgi.Tracker._
import org.osgi.framework.{BundleActivator, BundleContext, ServiceRegistration}
import org.osgi.service.cm.ManagedServiceFactory
import org.osgi.util.tracker.ServiceTracker

import scala.jdk.CollectionConverters._

class Activator extends BundleActivator {
  var serviceRegistration: Option[ServiceRegistration[ManagedServiceFactory]] = None
  var topTracker: Option[ServiceTracker[Top, Top]] = None
  var aggregateTracker: Option[ServiceTracker[StatusHandlerAggregate, StatusHandlerAggregate]] = None

  override def start(context: BundleContext): Unit = {
    aggregateTracker = Some(track[StatusHandlerAggregate, StatusHandlerAggregate](context) { aggregate =>
      track[StatusHandler, StatusHandler](context) { s =>
        aggregate.bindStatusHandler(s)
        s
      } { s =>
        aggregate.unbindStatusHandler(s)
      }
      topTracker = Some(track[Top, Top](context) { top =>
        val f = LocalTranslatorFactory(top, aggregate, context)
        serviceRegistration = Some(
          context.registerService(
            classOf[ManagedServiceFactory],
            f,
            new java.util.Hashtable[String, Any](
              Map("service.pid" -> classOf[LocalStatusItemTranslator].getName).asJava
            )
          )
        )
        top
      } { _ =>
        serviceRegistration.foreach(_.unregister())
        serviceRegistration = None
      })
      topTracker.foreach(_.open(true))
      aggregate
    } { _ =>
      topTracker.foreach(_.close())
      topTracker = None
    })
    aggregateTracker.foreach(_.open(true))
  }

  override def stop(context: BundleContext): Unit = {
    aggregateTracker.foreach(_.close())
    aggregateTracker = None
  }
}
