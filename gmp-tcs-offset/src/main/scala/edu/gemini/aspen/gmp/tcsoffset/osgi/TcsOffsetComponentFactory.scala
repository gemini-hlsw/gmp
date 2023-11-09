package edu.gemini.aspen.gmp.tcsoffset.osgi

import java.util.Dictionary
import com.google.common.collect.Maps
import edu.gemini.aspen.gmp.tcsoffset.model.TcsOffsetComponent
import edu.gemini.jms.api.JmsArtifact
import org.osgi.framework.{BundleContext, ServiceRegistration}
import org.osgi.service.cm.ManagedServiceFactory
import edu.gemini.epics.{EpicsObserver, EpicsWriter}
import com.google.gson.JsonParser
import java.util.logging.Logger

class TcsOffsetComponentFactory(context: BundleContext, ew: EpicsWriter, eo: EpicsObserver) extends ManagedServiceFactory {
  final private val existingServices = Maps.newHashMap[String, ServiceRegistration[_]]
  final private val existingComponents = Maps.newHashMap[String, TcsOffsetComponent]
  private val logger = Logger.getLogger(this.getClass.getName)

  override def getName = "GMP Offset Component Factory"

  override def updated(pid: String, properties: Dictionary[String, _]): Unit = {

    logger.fine("Starting the gmp-tcs-offset service ")
    if (existingServices.containsKey(pid)) {
      existingServices.remove(pid)
      updated(pid, properties)
    } else if (checkProperties(properties)) {
      val simulation = java.lang.Boolean.parseBoolean(properties.get(TcsOffsetComponent.SIMULATION).toString)
      val offsetConfig = properties.get(TcsOffsetComponent.OFFSETCONFIG).toString
      val jsonOffsetConfig = JsonParser.parseString(offsetConfig.substring(1,offsetConfig.length()-1)).getAsJsonObject()
      val tcsChLoopsStr = properties.get("tcsChLoops").toString
      val jsonTcsChLoops = JsonParser.parseString(tcsChLoopsStr.substring(1,tcsChLoopsStr.length()-1)).getAsJsonObject()
      val component = new TcsOffsetComponent(ew, eo, simulation, jsonOffsetConfig, jsonTcsChLoops)
      component.start()
      val reference = context.registerService(classOf[JmsArtifact], component: JmsArtifact, new java.util.Hashtable[String, String]())
      existingComponents.put(pid, component)
      existingServices.put(pid, reference)
    }
    else { 
      TcsOffsetComponent.LOG.warning("Cannot build " + classOf[TcsOffsetComponent].getName + " without the required properties")
    }
  }

  private def checkProperties(properties: Dictionary[String, _]): Boolean =
          properties.get(TcsOffsetComponent.TCS_OFFSET_CONFIG) != null &&
          properties.get(TcsOffsetComponent.SIMULATION) != null &&
          properties.get(TcsOffsetComponent.TCS_ChannelLoops) != null


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
