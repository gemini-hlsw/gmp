package edu.gemini.gmp.status.translator.osgi

import edu.gemini.aspen.giapi.statusservice.StatusHandlerAggregate
import edu.gemini.gmp.status.translator.{StatusItemTranslator, LocalStatusItemTranslator}
import edu.gemini.gmp.top.Top
import edu.gemini.jms.api.JmsArtifact
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration
import org.osgi.service.cm.ManagedServiceFactory
import java.util.Dictionary
import java.util.Hashtable
import java.util.logging.Logger

import scala.collection._
import edu.gemini.aspen.giapi.status.StatusHandler

case class LocalTranslatorFactory(top: Top, aggregate: StatusHandlerAggregate, context: BundleContext) extends ManagedServiceFactory {
  final val CONFIG_NAME: String = "xmlFileName"
  private final val LOG = Logger.getLogger(classOf[LocalTranslatorFactory].getName)
  private final val existingServices: mutable.Map[String, ServiceRegistration[_]] = mutable.Map.empty

  def getName = "LocalStatusItemTranslator factory"

  override def updated(pid: String, properties: Dictionary[String, _]) =
    for {
      configName <- Option(properties.get(CONFIG_NAME))
    } yield registerTranslator(pid, configName.toString)

  private def registerTranslator(pid: String, configName: String) = {
    deleted(pid)
    val provider = new LocalStatusItemTranslator(top, aggregate, configName)
    try {
      provider.start
      val serviceRegistration = context.registerService(Array[String](classOf[StatusHandler].getName, classOf[JmsArtifact].getName, classOf[StatusItemTranslator].getName), provider, new Hashtable[String, AnyRef])
      existingServices += pid -> serviceRegistration
    } catch {
      case e: Exception =>
        LOG.severe(e.getMessage)
    }
  }

  override def deleted(pid: String):Unit = {
    for {
      s <- existingServices.remove(pid)
    } yield s.unregister()
  }

}