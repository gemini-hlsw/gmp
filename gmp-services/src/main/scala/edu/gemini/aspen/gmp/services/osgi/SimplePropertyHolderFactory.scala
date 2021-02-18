package edu.gemini.aspen.gmp.services.osgi

import java.util
import java.util.Dictionary

import com.google.common.collect.Maps
import edu.gemini.aspen.gmp.services.PropertyHolder
import edu.gemini.aspen.gmp.services.properties.SimplePropertyHolder
import org.osgi.framework.{BundleContext, ServiceRegistration}
import org.osgi.service.cm.ManagedServiceFactory


class SimplePropertyHolderFactory(val context: BundleContext) extends ManagedServiceFactory {
  final private val existingServices = Maps.newHashMap[String, ServiceRegistration[_]]

  override def getName = "GMP Property holder factory"

  override def updated(pid: String, properties: Dictionary[String, _]): Unit = {
    if (existingServices.containsKey(pid)) {
      existingServices.remove(pid)
      updated(pid, properties)
    } else {
      val component = new SimplePropertyHolder(properties)
      val reference = context.registerService(classOf[PropertyHolder], component: PropertyHolder, new util.Hashtable[String, String]())
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