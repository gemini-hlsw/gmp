package edu.gemini.aspen.gmp.tcs.osgi

import java.util.Dictionary

import com.google.common.collect.Maps
import edu.gemini.aspen.gmp.tcs.model.TcsContextComponent
import edu.gemini.epics.EpicsReader
import edu.gemini.jms.api.JmsArtifact
import org.osgi.framework.{BundleContext, ServiceRegistration}
import org.osgi.service.cm.ManagedServiceFactory

class TcsContextComponentFactory(context: BundleContext, reader: EpicsReader) extends ManagedServiceFactory {
  final private val existingServices = Maps.newHashMap[String, ServiceRegistration[_]]
  final private val existingComponents = Maps.newHashMap[String, TcsContextComponent]

  override def getName = "GMP Context Component Factory"

  override def updated(pid: String, properties: Dictionary[String, _]): Unit = {
    if (existingServices.containsKey(pid)) {
      existingServices.remove(pid)
      updated(pid, properties)
    } else if (checkProperties(properties)) {
      val tcsChannel = properties.get(TcsContextComponent.TCSCHANNEL).toString
      val simulation = java.lang.Boolean.parseBoolean(properties.get(TcsContextComponent.SIMULATION).toString)
      val simulationData = properties.get(TcsContextComponent.SIMULATION_DATA).toString
      val component = new TcsContextComponent(reader, tcsChannel, simulation, simulationData)
      component.start()
      val reference = context.registerService(classOf[JmsArtifact], component: JmsArtifact, new java.util.Hashtable[String, String]())
      existingComponents.put(pid, component)
      existingServices.put(pid, reference)
    }
    else TcsContextComponent.LOG.warning("Cannot build " + classOf[TcsContextComponent].getName + " without the required properties")
  }

  private def checkProperties(properties: Dictionary[String, _]): Boolean =
    properties.get(TcsContextComponent.TCSCHANNEL) != null && properties.get(TcsContextComponent.SIMULATION) != null && properties.get(TcsContextComponent.SIMULATION_DATA) != null

  override def deleted(pid: String): Unit = {
    if (existingServices.containsKey(pid)) {
      existingComponents.remove(pid).stop()
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