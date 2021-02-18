package edu.gemini.aspen.gds.postprocessingpolicy.osgi

import java.util.Dictionary

import com.google.common.collect.Maps
import edu.gemini.aspen.gds.api.PostProcessingPolicy
import edu.gemini.aspen.gds.postprocessingpolicy.SetPermissionsPolicy
import org.osgi.framework.{BundleContext, ServiceRegistration}
import org.osgi.service.cm.ManagedServiceFactory

class SetPermissionsPolicyFactory(context: BundleContext) extends ManagedServiceFactory {
  final private val existingServices = Maps.newHashMap[String, ServiceRegistration[_]]

  override def getName = "Set Permissions Policy Factory"

  override def updated(pid: String, properties: Dictionary[String, _]): Unit = {
    if (existingServices.containsKey(pid)) {
      existingServices.remove(pid)
      updated(pid, properties)
    } else if (checkProperties(properties)) {
      val permissions = properties.get(SetPermissionsPolicy.Permissions).toString
      val useSudo = properties.get(SetPermissionsPolicy.UseSudo).toString
      val component = new SetPermissionsPolicy(permissions, useSudo)
      val reference = context.registerService(classOf[PostProcessingPolicy], component, new java.util.Hashtable[String, String]())
      existingServices.put(pid, reference)
    }
    else SetPermissionsPolicy.Log.warning("Cannot build " + classOf[SetPermissionsPolicy].getName + " without the required properties")
  }

  private def checkProperties(properties: Dictionary[String, _]): Boolean =
    properties.get(SetPermissionsPolicy.Permissions) != null && properties.get(SetPermissionsPolicy.UseSudo) != null

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