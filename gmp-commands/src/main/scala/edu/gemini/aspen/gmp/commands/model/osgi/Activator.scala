package edu.gemini.aspen.gmp.commands.model.osgi

import java.util.Dictionary
import java.util.logging.Logger

import edu.gemini.aspen.giapi.commands.{CommandSender, CommandUpdater}
import edu.gemini.aspen.giapi.status.setter.StatusSetter
import edu.gemini.aspen.gmp.commands.handlers.CommandHandlers
import edu.gemini.aspen.gmp.commands.model.executors.{SequenceCommandExecutor, SequenceCommandExecutorStrategy}
import edu.gemini.aspen.gmp.commands.model.impl.{ActionManager, ActionManagerImpl, CommandSenderImpl, CommandUpdaterImpl}
import edu.gemini.aspen.gmp.commands.model.{ActionMessageBuilder, ActionSender}
import edu.gemini.gmp.top.Top
import edu.gemini.util.osgi.Tracker._
import org.osgi.framework.{BundleActivator, BundleContext, ServiceRegistration}
import org.osgi.service.cm.ManagedServiceFactory
import org.osgi.util.tracker.ServiceTracker

import scala.collection.mutable

class Activator extends BundleActivator {

  var amRegistration: Option[ServiceRegistration[ActionManager]] = None
  var cuRegistration: Option[ServiceRegistration[CommandUpdater]] = None
  var csRegistration: Option[ServiceRegistration[CommandSender]] = None
  var factoryTracker: Option[ServiceTracker[ActionMessageBuilder, _]] = None
  var senderTracker: Option[ServiceTracker[ActionSender, _]] = None
  var actionManager: Option[ActionManagerImpl] = None

  def start(context: BundleContext):Unit = {
    val am = new ActionManagerImpl
    am.start()
    actionManager = Some(am)
    amRegistration = Some(context.registerService(classOf[ActionManager], am, new java.util.Hashtable[String, String]))
    val commandUpdater = new CommandUpdaterImpl(am)
    cuRegistration = Some(context.registerService(classOf[CommandUpdater], commandUpdater, new java.util.Hashtable[String, String]))

    factoryTracker = Some(track[ActionMessageBuilder, CommandHandlers, StatusSetter, Top, SequenceCommandExecutorFactory](context) { (b, ch, ss, t) =>
        val f = SequenceCommandExecutorFactory(b, am, ch, ss, t, context)
        val props = new java.util.Hashtable[String, String]()
        props.put("service.pid", classOf[SequenceCommandExecutorStrategy].getName)
        context.registerService(classOf[ManagedServiceFactory], f, props)
        f
      } { s =>
      })
    factoryTracker.foreach(_.open())

    senderTracker = Some(track[ActionSender, SequenceCommandExecutor, CommandSender](context) { (s, e) =>
        val f = new CommandSenderImpl(am, s, e)
        csRegistration = Some(context.registerService(classOf[CommandSender], f, new java.util.Hashtable[String, String]))
        f
      } { s =>
        csRegistration.foreach(_.unregister())
        csRegistration = None
      })
    senderTracker.foreach(_.open())
  }

  def stop(context: BundleContext):Unit = {
    (cuRegistration :: amRegistration :: csRegistration :: Nil).foreach(_.foreach(_.unregister))
    cuRegistration = None
    amRegistration = None
    csRegistration = None
    (factoryTracker :: senderTracker :: Nil).foreach(_.foreach(_.close()))
    factoryTracker = None
    senderTracker = None
    actionManager.foreach(_.stop)
    actionManager = None
  }

}

case class SequenceCommandExecutorFactory(builder: ActionMessageBuilder, manager: ActionManager, handler: CommandHandlers, setter:StatusSetter, top: Top, context: BundleContext) extends ManagedServiceFactory {
  final val STARTUP_SCRIPT: String = "instrumentStartupScript"
  private final val LOG = Logger.getLogger(classOf[SequenceCommandExecutorFactory].getName)
  private final val existingServices: mutable.Map[String, ServiceRegistration[_]] = mutable.Map.empty

  def getName = "SequenceCommandExecutorFactory factory"

  override def updated(pid: String, properties: Dictionary[String, _]): Unit =
    for {
      configName <- Option(properties.get(STARTUP_SCRIPT))
    } yield registerExecutor(pid, configName.toString)

  private def registerExecutor(pid: String, startup: String) = {
    deleted(pid)
    val executor = new SequenceCommandExecutorStrategy(builder, manager, handler, setter, top, startup)
    try {
      val serviceRegistration = context.registerService(classOf[SequenceCommandExecutor], executor, new java.util.Hashtable[String, AnyRef])
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