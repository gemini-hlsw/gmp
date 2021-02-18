package edu.gemini.aspen.gmp.status.simulator.osgi

import java.util.Dictionary

import com.google.common.collect.Maps
import edu.gemini.aspen.gmp.status.simulator.StatusSimulator
import edu.gemini.gmp.top.Top
import edu.gemini.jms.api.JmsArtifact
import org.osgi.framework.{BundleContext, ServiceRegistration}
import org.osgi.service.cm.ManagedServiceFactory

class StatusSimulatorFactory(context: BundleContext, top: Top) extends ManagedServiceFactory {
  final private val existingServices = Maps.newHashMap[String, ServiceRegistration[_]]

  override def getName = "GMP Status simulator"

  override def updated(pid: String, properties: Dictionary[String, _]): Unit = {
    if (existingServices.containsKey(pid)) {
      existingServices.remove(pid)
      updated(pid, properties)
    } else {
      val component = new StatusSimulator(properties.get(StatusSimulator.configurationFile).toString, top)
      val reference = context.registerService(classOf[JmsArtifact], component: JmsArtifact, new java.util.Hashtable[String, String]())
      existingServices.put(pid, reference)
    }
  }

  override def deleted(pid: String): Unit = {
    if (existingServices.containsKey(pid)) {
      existingServices.remove(pid).unregister()
    }
  }

  def stopServices(): Unit = {
    import scala.jdk.CollectionConverters._
    for (pid <- existingServices.keySet.asScala) {
      deleted(pid)
    }
  }

}