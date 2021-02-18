package edu.gemini.aspen.gmp.epicstostatus.osgi

import com.google.common.collect.Maps
import edu.gemini.jms.api.{JmsArtifact, JmsProvider}
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration
import org.osgi.service.cm.ManagedServiceFactory
import java.util.Dictionary
import java.util.logging.Logger

import edu.gemini.aspen.gmp.epicstostatus.EpicsToStatusComponent
import edu.gemini.epics.EpicsReader

object EpicsToStatusFactory {
  private val LOG = Logger.getLogger(classOf[EpicsToStatusFactory].getName)
}

class EpicsToStatusFactory(val context: BundleContext, reader: EpicsReader, provider: JmsProvider) extends ManagedServiceFactory {
  final private val existingServices = Maps.newHashMap[String, EpicsToStatusComponent]

  private class ServiceRef private(val registration: ServiceRegistration[JmsArtifact], val heartbeat: Nothing) {
  }

  override def getName = "GMP Epics to Status factory"

  override def updated(pid: String, properties: Dictionary[String, _]): Unit = {
    if (existingServices.containsKey(pid)) {
      existingServices.get(pid).shutdown()
      existingServices.remove(pid)
      updated(pid, properties)
    }
    else if (checkProperties(properties)) {
      val component = new EpicsToStatusComponent(reader, provider, properties.get(EpicsToStatusComponent.PROP).toString)
      component.initialize()
      existingServices.put(pid, component)
    }
    else EpicsToStatusFactory.LOG.warning("Cannot build " + classOf[EpicsToStatusFactory].getName + " without the required properties")
  }

  private def checkProperties(properties: Dictionary[String, _]): Boolean = properties.get(EpicsToStatusComponent.PROP) != null

  override def deleted(pid: String): Unit = {
    if (existingServices.containsKey(pid)) {
      existingServices.get(pid).shutdown()
    }
  }

  def stopServices(): Unit = {
    import scala.jdk.CollectionConverters._
    for (pid <- existingServices.keySet.asScala) {
      deleted(pid)
    }
  }

}