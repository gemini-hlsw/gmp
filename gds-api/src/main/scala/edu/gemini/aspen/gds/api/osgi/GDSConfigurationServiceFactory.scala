package edu.gemini.aspen.gds.api.osgi

import java.util.{Dictionary, Hashtable}

import com.google.common.collect.Maps
import edu.gemini.aspen.gds.api.configuration.{GDSConfigurationService, GDSConfigurationServiceImpl}
import org.osgi.framework.{BundleContext, ServiceRegistration}
import org.osgi.service.cm.ManagedServiceFactory

class GDSConfigurationServiceFactory(context: BundleContext) extends ManagedServiceFactory {
  final private val existingServices = Maps.newHashMap[String, ServiceRegistration[_]]
  final private val existingComponents = Maps.newHashMap[String, GDSConfigurationService]

  override def getName = "GDS Configuration Service Factory"

  override def updated(pid: String, properties: Dictionary[String, _]): Unit = {
    if (existingServices.containsKey(pid)) {
      existingServices.remove(pid)
      updated(pid, properties)
    } else if (checkProperties(properties)) {
      val configurationFile = properties.get(GDSConfigurationService.KeywordsConfiguration).toString
      val component = new GDSConfigurationServiceImpl(configurationFile)
      val reference = context.registerService(classOf[GDSConfigurationService], component: GDSConfigurationService, new Hashtable[String, String]())
      existingComponents.put(pid, component)
      existingServices.put(pid, reference)
    }
    else GDSConfigurationService.LOG.warning("Cannot build " + classOf[GDSConfigurationService].getName + " without the required properties")
  }

  private def checkProperties(properties: Dictionary[String, _]): Boolean =
    properties.get(GDSConfigurationService.KeywordsConfiguration) != null

  override def deleted(pid: String): Unit = {
    if (existingServices.containsKey(pid)) {
      existingServices.remove(pid).unregister()
    }
  }

  def stopServices(): Unit = {
    import scala.jdk.CollectionConverters._
    for (pid <- existingServices.keySet) {
      deleted(pid)
    }
  }

}