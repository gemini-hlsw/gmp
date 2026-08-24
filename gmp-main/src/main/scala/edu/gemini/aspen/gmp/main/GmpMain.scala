package edu.gemini.aspen.gmp.main

import com.google.common.collect.ImmutableMap
import com.google.gson.JsonParser
import edu.gemini.aspen.epicsheartbeat.EpicsHeartbeat
import edu.gemini.aspen.giapi.data.obsevents.ObservationEventAction
import edu.gemini.aspen.giapi.data.obsevents.jms.JmsObservationEventListener
import edu.gemini.aspen.giapi.data.fileevents.FileEventActionRunner
import edu.gemini.aspen.giapi.data.fileevents.jms.JmsFileEventsListener
import edu.gemini.aspen.giapi.status.dispatcher.StatusDispatcher
import edu.gemini.aspen.giapi.status.setter.StatusSetterService
import edu.gemini.aspen.giapi.statusservice.{ StatusHandlerAggregate, StatusService }
import edu.gemini.aspen.giapi.util.jms.JmsKeys
import edu.gemini.aspen.gmp.commands.handlers.impl.CommandHandlersImpl
import edu.gemini.aspen.gmp.commands.jms.clientbridge.{
  CommandMessagesBridgeImpl,
  CommandMessagesConsumer
}
import edu.gemini.aspen.gmp.commands.jms.instrumentbridge.{
  ActionMessageActionSender,
  CompletionInfoListener,
  JmsActionMessageBuilder
}
import edu.gemini.aspen.gmp.commands.model.executors.SequenceCommandExecutorStrategy
import edu.gemini.aspen.gmp.commands.model.impl.{
  ActionManagerImpl,
  CommandSenderImpl,
  CommandUpdaterImpl
}
import edu.gemini.aspen.gmp.epics.impl.{
  ChannelListConfiguration,
  EpicsMonitor,
  EpicsRequestHandlerImpl,
  EpicsUpdaterThread
}
import edu.gemini.aspen.gmp.epics.simulator.EpicsSimulatorComponent
import edu.gemini.aspen.gmp.health.Health
import edu.gemini.aspen.gmp.heartbeat.Heartbeat
import edu.gemini.aspen.gmp.logging.LoggingMessageConsumer
import edu.gemini.aspen.gmp.pcs.model.PcsUpdaterComponent
import edu.gemini.aspen.gmp.services.jms.RequestConsumer
import edu.gemini.aspen.gmp.services.properties.{ PropertyService, SimplePropertyHolder }
import edu.gemini.aspen.gmp.status.simulator.StatusSimulator
import edu.gemini.aspen.gmp.statusdb.StatusDatabase
import edu.gemini.aspen.gmp.statusgw.StatusDatabaseServiceDecorator
import edu.gemini.aspen.gmp.statusgw.jms.{
  JmsStatusDispatcher,
  MultipleStatusItemsRequestListener,
  StatusItemRequestListener,
  StatusNamesRequestListener
}
import edu.gemini.aspen.gmp.statusservice.EpicsStatusService
import edu.gemini.aspen.gmp.tcs.model.TcsContextComponent
import edu.gemini.aspen.gmp.tcsoffset.model.TcsOffsetComponent
import edu.gemini.aspen.heartbeatdistributor.HeartbeatDistributor
import edu.gemini.cas.impl.ChannelAccessServerImpl
import edu.gemini.epics.EpicsService
import edu.gemini.epics.impl.{
  EpicsClientSubscriber,
  EpicsObserverImpl,
  EpicsReaderImpl,
  EpicsWriterImpl
}
import edu.gemini.gmp.status.translator.LocalStatusItemTranslator
import edu.gemini.gmp.top.TopImpl
import edu.gemini.jms.activemq.broker.{ ActiveMQBrokerComponent, ConfigDefaults }
import edu.gemini.jms.activemq.provider.ActiveMQJmsProvider
import edu.gemini.jms.api.{
  BaseMessageConsumer,
  DestinationData,
  DestinationType,
  JmsArtifact,
  JmsSimpleMessageSelector
}
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.logging.Logger
import scala.collection.mutable.ListBuffer

/**
 * Composition root for GMP as a plain Java application, replacing the OSGi
 * container. Components are constructed in dependency order; a component whose
 * configuration file is absent is skipped, mirroring the old ConfigAdmin
 * behavior where no config meant no factory instance. See ADR 0001.
 */
final class GmpApp(servicesDir: java.nio.file.Path) {
  private val logger = Logger.getLogger(classOf[GmpApp].getName)

  private val jmsArtifacts = ListBuffer.empty[JmsArtifact]
  private val stopActions  = ListBuffer.empty[(String, () => Unit)]

  private def onStop(name: String)(action: => Unit): Unit =
    stopActions += ((name, () => action))

  logger.info(s"GMP starting, services configuration from $servicesDir")

  private val config = ServicesConfig.load(servicesDir)

  // 1. Top
  val top = config.first("edu.gemini.gmp.top.Top").flatMap { c =>
    (c.get("epicsTop"), c.get("giapiTop")) match {
      case (Some(e), Some(g)) => Some(new TopImpl(e, g))
      case _                  => missing("Top", c); None
    }
  }

  // 2. PropertyHolder and its JMS services
  val propertyHolder =
    config.first("edu.gemini.aspen.gmp.services.properties.SimplePropertyHolder").map { c =>
      new SimplePropertyHolder(c.toDictionary)
    }
  val propertyService = propertyHolder.map(new PropertyService(_))
  propertyService.foreach { ps =>
    jmsArtifacts += ps
    jmsArtifacts += new RequestConsumer(ps)
  }

  // 3. Channel Access Server
  val cas = new ChannelAccessServerImpl()
  cas.start()
  onStop("ChannelAccessServer")(cas.stop())

  // 4. EPICS service and derived accessors
  val epicsService = config.first("edu.gemini.epics.EpicsService").flatMap { c =>
    (c.get("addressList"), c.get("ioTimeout")) match {
      case (Some(addressList), Some(_)) =>
        val timeout = c.doubleValue("ioTimeout").getOrElse(1.0)
        // readRetries keeps the historical parse-failure fallback to 0
        val retries = c.intValue("readRetries").getOrElse(0)
        val es      = new EpicsService(addressList, timeout, retries)
        es.startService()
        onStop("EpicsService")(es.stopService())
        Some(es)
      case _                            => missing("EpicsService", c); None
    }
  }
  val epicsWriter   = epicsService.map(new EpicsWriterImpl(_))
  val epicsReader   = epicsService.map(new EpicsReaderImpl(_))
  val epicsObserver = epicsService.map { es =>
    val eo = new EpicsObserverImpl(es)
    eo.startObserver()
    onStop("EpicsObserver")(eo.stopObserver())
    eo
  }
  val epicsClientSubscriber = epicsObserver.map(new EpicsClientSubscriber(_))

  // 5. Embedded JMS broker; must be up before the vm:// provider connects
  config.first("edu.gemini.jms.activemq.broker.ActiveMQBrokerComponent").foreach { c =>
    val required = List(
      ConfigDefaults.BROKER_NAME_PROPERTY,
      ConfigDefaults.BROKER_URL_PROPERTY,
      ConfigDefaults.BROKER_PERSISTENT_PROPERTY,
      ConfigDefaults.BROKER_DELETE_MESSAGES_ON_STARTUP_PROPERTY,
      ConfigDefaults.BROKER_USE_ADVISORY_MESSAGES_PROPERTY,
      ConfigDefaults.BROKER_USE_JMX_PROPERTY,
      ConfigDefaults.BROKER_JMX_RMI_PORT_PROPERTY,
      ConfigDefaults.BROKER_JMX_CONNECTOR_PORT_PROPERTY,
      ConfigDefaults.BROKER_MEMORY_PERCENTAGE_PROPERTY,
      ConfigDefaults.BROKER_MAX_STORAGE_MB_PROPERTY,
      ConfigDefaults.BROKER_MAX_MESSAGES_LIMIT_PROPERTY
    )
    if (required.forall(k => c.get(k).isDefined)) {
      val broker = new ActiveMQBrokerComponent(
        c.booleanValue(ConfigDefaults.BROKER_USE_JMX_PROPERTY).get,
        c.booleanValue(ConfigDefaults.BROKER_PERSISTENT_PROPERTY).get,
        c.get(ConfigDefaults.BROKER_NAME_PROPERTY).get,
        c.get(ConfigDefaults.BROKER_URL_PROPERTY).get,
        c.booleanValue(ConfigDefaults.BROKER_DELETE_MESSAGES_ON_STARTUP_PROPERTY).get,
        c.booleanValue(ConfigDefaults.BROKER_USE_ADVISORY_MESSAGES_PROPERTY).get,
        c.intValue(ConfigDefaults.BROKER_JMX_RMI_PORT_PROPERTY).get,
        c.intValue(ConfigDefaults.BROKER_JMX_CONNECTOR_PORT_PROPERTY).get,
        c.doubleValue(ConfigDefaults.BROKER_MEMORY_PERCENTAGE_PROPERTY).get,
        c.intValue(ConfigDefaults.BROKER_MAX_STORAGE_MB_PROPERTY).get,
        c.intValue(ConfigDefaults.BROKER_MAX_MESSAGES_LIMIT_PROPERTY).get
      )
      broker.startBroker()
      onStop("ActiveMQBroker")(broker.stopBroker())
    } else missing("ActiveMQBrokerComponent", c)
  }

  // 6. Status database (StatusHandler + StatusDatabaseService)
  val statusDatabase = new StatusDatabase()

  // 7. Status aggregation, dispatch and setting
  val aggregate = new StatusHandlerAggregate()
  aggregate.bindStatusHandler(statusDatabase)
  val statusDispatcher = new StatusDispatcher()
  aggregate.bindStatusHandler(statusDispatcher)
  val statusSetter     = new StatusSetterService()
  jmsArtifacts += statusSetter
  config.first("edu.gemini.aspen.giapi.statusservice.StatusService").foreach { c =>
    (c.get("serviceName"), c.get("statusName")) match {
      case (Some(serviceName), Some(statusName)) =>
        jmsArtifacts += new StatusService(aggregate, serviceName, statusName)
      case _                                     => missing("StatusService", c)
    }
  }

  // 8. GMP EPICS access: registrar, request handler and channel monitor
  val epicsRegistrar = new EpicsUpdaterThread()
  epicsReader.foreach(r => jmsArtifacts += new EpicsRequestHandlerImpl(r))
  config.first("edu.gemini.aspen.gmp.epics.impl.ChannelListConfiguration").foreach { c =>
    c.get("configurationFile") match {
      case Some(file) =>
        val channelConfig = new ChannelListConfiguration(file)
        val channels      = channelConfig.getValidChannelsNames.toArray(new Array[String](0))
        val monitor       = new EpicsMonitor(epicsRegistrar, channelConfig, channels)
        jmsArtifacts += monitor
        epicsClientSubscriber.foreach { subscriber =>
          subscriber.bindEpicsClient(
            monitor,
            ImmutableMap.of("edu.gemini.epics.api.EpicsClient.EPICS_CHANNELS",
                            channels.asInstanceOf[Object]
            )
          )
        }
        onStop("EpicsMonitor")(monitor.stopChannels())
      case None       => missing("ChannelListConfiguration", c)
    }
  }

  // 9. Status translation to the local aggregate and to EPICS
  for {
    t <- top
    c <- config.first("edu.gemini.gmp.status.translator.LocalStatusItemTranslator")
    x <- c.get("xmlFileName").orElse { missing("LocalStatusItemTranslator", c); None }
  } {
    val translator = new LocalStatusItemTranslator(t, aggregate, x)
    translator.start
    aggregate.bindStatusHandler(translator)
    jmsArtifacts += translator
  }
  for {
    t <- top
    c <- config.first("edu.gemini.aspen.gmp.statusservice.EpicsStatusService")
    x <- c.get("xmlFileName").orElse { missing("EpicsStatusService", c); None }
  } {
    val ess = new EpicsStatusService(cas, t, x)
    ess.initialize()
    aggregate.bindStatusHandler(ess)
    onStop("EpicsStatusService")(ess.shutdown())
  }

  // 10. Heartbeats and health
  val heartbeatDistributor = new HeartbeatDistributor()
  jmsArtifacts += heartbeatDistributor
  for {
    t <- top
    c <- config.first("edu.gemini.aspen.epicsheartbeat.EpicsHeartbeat")
    n <- c.get("channelName").orElse { missing("EpicsHeartbeat", c); None }
  } {
    val ehb = new EpicsHeartbeat(cas, t, n)
    ehb.initialize()
    heartbeatDistributor.bindHeartbeatConsumer(ehb)
    onStop("EpicsHeartbeat")(ehb.shutdown())
  }
  for {
    t <- top
    c <- config.first("edu.gemini.aspen.gmp.heartbeat.Heartbeat")
    n <- c.get("heartbeatName").orElse { missing("Heartbeat", c); None }
    s <- c.booleanValue("sendJms")
  } {
    val heartbeat = new Heartbeat(n, s, t, statusSetter)
    jmsArtifacts += heartbeat
    onStop("Heartbeat")(heartbeat.stopService())
  }
  for {
    t <- top
    c <- config.first("edu.gemini.aspen.gmp.health.Health")
    n <- c.get("healthName").orElse { missing("Health", c); None }
  } {
    val health = new Health(n, t, statusSetter, StaticBundlesDatabase)
    jmsArtifacts += health
    onStop("Health")(health.stopService())
  }

  // 11. TCS / PCS / simulators
  for {
    r <- epicsReader
    c <- config.first("edu.gemini.aspen.gmp.tcs.model.TcsContextComponent")
  } {
    (c.get(TcsContextComponent.TCSCHANNEL),
     c.get(TcsContextComponent.SIMULATION),
     c.get(TcsContextComponent.SIMULATION_DATA)
    ) match {
      case (Some(channel), Some(sim), Some(simData)) =>
        val component =
          new TcsContextComponent(r, channel, java.lang.Boolean.parseBoolean(sim), simData)
        component.start()
        jmsArtifacts += component
        onStop("TcsContextComponent")(component.stop())
      case _                                         => missing("TcsContextComponent", c)
    }
  }
  for {
    w <- epicsWriter
    o <- epicsObserver
    c <- config.first("edu.gemini.aspen.gmp.tcsoffset.model.TcsOffsetComponent")
  } {
    (c.get(TcsOffsetComponent.SIMULATION),
     c.get(TcsOffsetComponent.OFFSETCONFIG),
     c.get("tcsChLoops"),
     c.get("caListDef")
    ) match {
      case (Some(sim), Some(offsets), Some(loops), Some(caDefs)) =>
        val component = new TcsOffsetComponent(
          w,
          o,
          java.lang.Boolean.parseBoolean(sim),
          parseQuotedJson(offsets),
          parseQuotedJson(loops),
          parseQuotedJson(caDefs)
        )
        component.start()
        jmsArtifacts += component
        onStop("TcsOffsetComponent")(component.stop())
      case _                                                     =>
        missing("TcsOffsetComponent", c)
    }
  }
  config.first("edu.gemini.aspen.gmp.pcs.model.PcsUpdaterComponent").foreach { c =>
    (c.get("simulation"), c.get("epicsChannel"), c.get("gains"), c.intValue("taiDiff")) match {
      case (Some(sim), Some(channel), Some(gains), Some(taiDiff)) =>
        val updater = new PcsUpdaterComponent(cas,
                                              "true".equalsIgnoreCase(sim.trim),
                                              channel,
                                              gains,
                                              taiDiff
        )
        jmsArtifacts += updater
        updater.startComponent()
        onStop("PcsUpdaterComponent")(updater.stopComponent())
      case _                                                      =>
        missing("PcsUpdaterComponent", c)
    }
  }
  config.first("edu.gemini.aspen.gmp.epics.simulator.EpicsSimulatorComponent").foreach { c =>
    c.get("simulationConfiguration") match {
      case Some(file) =>
        val simulator = new EpicsSimulatorComponent(epicsRegistrar, file)
        simulator.startSimulation()
        onStop("EpicsSimulator")(simulator.stopSimulation())
      case None       => missing("EpicsSimulatorComponent", c)
    }
  }
  for {
    t <- top
    c <- config.first("edu.gemini.aspen.gmp.status.simulator.StatusSimulator")
    f <- c.get(StatusSimulator.configurationFile).orElse { missing("StatusSimulator", c); None }
  } jmsArtifacts += new StatusSimulator(f, t)

  // 12-14. Command handling
  val actionSender   = new ActionMessageActionSender()
  jmsArtifacts += actionSender
  val messageBuilder = new JmsActionMessageBuilder()
  val handlers       = new CommandHandlersImpl()
  val actionManager  = new ActionManagerImpl()
  actionManager.start()
  onStop("ActionManager")(actionManager.stop())
  val commandUpdater = new CommandUpdaterImpl(actionManager)
  jmsArtifacts += new CompletionInfoListener(commandUpdater)
  val commandSender  = for {
    t <- top
    c <- config
           .first("edu.gemini.aspen.gmp.commands.model.executors.SequenceCommandExecutorStrategy")
    s <- c.get("instrumentStartupScript").orElse {
           missing("SequenceCommandExecutorStrategy", c); None
         }
  } yield {
    val executor =
      new SequenceCommandExecutorStrategy(messageBuilder, actionManager, handlers, statusSetter, t, s)
    new CommandSenderImpl(actionManager, actionSender, executor)
  }

  // 15. Status gateway, observation events, file events
  val gwDecorator  = new StatusDatabaseServiceDecorator()
  gwDecorator.setDatabaseService(statusDatabase)
  val gwDispatcher = new JmsStatusDispatcher("Gateway Status Dispatcher")
  jmsArtifacts += gwDispatcher
  jmsArtifacts += new BaseMessageConsumer(
    "Gateway Status Consumer",
    new DestinationData(JmsKeys.GW_STATUS_REQUEST_DESTINATION, DestinationType.TOPIC),
    new StatusItemRequestListener(gwDecorator, gwDispatcher),
    new JmsSimpleMessageSelector(
      JmsKeys.GW_STATUS_REQUEST_TYPE_PROPERTY + " = '" + JmsKeys.GW_STATUS_REQUEST_TYPE_ITEM + "'"
    )
  )
  jmsArtifacts += new BaseMessageConsumer(
    "Gateway Status Names Consumer",
    new DestinationData(JmsKeys.GW_STATUS_REQUEST_DESTINATION, DestinationType.TOPIC),
    new StatusNamesRequestListener(gwDecorator, gwDispatcher),
    new JmsSimpleMessageSelector(
      JmsKeys.GW_STATUS_REQUEST_TYPE_PROPERTY + " = '" + JmsKeys.GW_STATUS_REQUEST_TYPE_NAMES + "'"
    )
  )
  jmsArtifacts += new BaseMessageConsumer(
    "Gateway Multiple Status Consumer",
    new DestinationData(JmsKeys.GW_STATUS_REQUEST_DESTINATION, DestinationType.TOPIC),
    new MultipleStatusItemsRequestListener(gwDecorator, gwDispatcher),
    new JmsSimpleMessageSelector(
      JmsKeys.GW_STATUS_REQUEST_TYPE_PROPERTY + " = '" + JmsKeys.GW_STATUS_REQUEST_TYPE_ALL + "'"
    )
  )

  val obsEventComposite = new ObservationEventAction()
  jmsArtifacts += new BaseMessageConsumer(
    "JMS Observation Event Monitor",
    new DestinationData(JmsObservationEventListener.TOPIC_NAME, DestinationType.TOPIC),
    new JmsObservationEventListener(obsEventComposite)
  )

  val fileEventAction = new FileEventActionRunner()
  jmsArtifacts += new BaseMessageConsumer(
    "JMS File Event Monitor",
    new DestinationData(JmsFileEventsListener.TOPIC_NAME, DestinationType.TOPIC),
    new JmsFileEventsListener(fileEventAction)
  )
  onStop("FileEventAction")(fileEventAction.shutdown())

  // JMS logging consumer
  jmsArtifacts += new LoggingMessageConsumer()

  // 16. GDS pipeline
  for {
    ph <- propertyHolder
    c  <- config.first("edu.gemini.aspen.gds.GdsConfiguration")
  } {
    GdsBootstrap.start(c.properties, ph, epicsReader, Some(statusDatabase)).foreach { runtime =>
      obsEventComposite.registerHandler(runtime.observationEventHandler)
      onStop("GDS")(runtime.stop())
    }
  }

  // 17. JMS provider last: connect, then hand every artifact its connection.
  val provider = config.first("edu.gemini.jms.activemq.provider.ActiveMQJmsProvider").flatMap {
    c =>
      c.get("brokerUrl") match {
        case Some(url) =>
          val p = c.intValue("closeTimeout") match {
            case Some(t) => new ActiveMQJmsProvider(url, t)
            case None    => new ActiveMQJmsProvider(url)
          }
          // The client bridge existed only once CommandSender and JmsProvider met
          commandSender.foreach { cs =>
            val bridge = new CommandMessagesBridgeImpl(p, cs)
            jmsArtifacts += new CommandMessagesConsumer(bridge)
          }
          p.startConnection()
          jmsArtifacts.foreach(p.bindJmsArtifact)
          onStop("JmsProvider")(jmsArtifacts.foreach(p.unbindJmsArtifact))
          Some(p)
        case None      => missing("ActiveMQJmsProvider", c); None
      }
  }
  if (provider.isEmpty)
    logger.severe("No JMS provider configured; JMS-dependent components stay idle")

  logger.info("GMP started")

  def stop(): Unit = {
    logger.info("GMP shutting down")
    stopActions.reverseIterator.foreach { case (name, action) =>
      try action()
      catch { case e: Exception => logger.warning(s"Error stopping $name: ${e.getMessage}") }
    }
  }

  /** TcsOffset config values arrive wrapped in quotes; strip them before parsing. */
  private def parseQuotedJson(value: String) =
    JsonParser.parseString(value.substring(1, value.length - 1)).getAsJsonObject

  private def missing(component: String, c: ServiceConfig): Unit =
    logger.warning(s"Cannot build $component without its required properties (${c.file})")
}

object GmpMain {

  def main(args: Array[String]): Unit = {
    val confBase = sys.props.getOrElse("conf.base", "conf")
    val app      = new GmpApp(Paths.get(confBase, "services"))

    val latch = new CountDownLatch(1)
    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      app.stop()
      latch.countDown()
    }, "gmp-shutdown"))
    latch.await()
  }
}
