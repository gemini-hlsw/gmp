package edu.gemini.aspen.gds.osgi

import cats.syntax.all._
import cats.effect.{ FiberIO, IO, Ref, Resource }
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import edu.gemini.aspen.gds.Main
import edu.gemini.aspen.gds.configuration.{ KeywordConfiguration, KeywordConfigurationFile }
import edu.gemini.aspen.gds.observations.{ ObservationEventReceiver, ObservationStateEvent }
import edu.gemini.epics.EpicsReader
import edu.gemini.aspen.giapi.status.StatusDatabaseService
import java.util
import java.util.logging.Logger
import edu.gemini.util.osgi.Tracker
import org.osgi.framework.{ BundleActivator, BundleContext, ServiceRegistration }
import org.osgi.util.tracker.ServiceTracker
import org.osgi.service.event.{ EventConstants, EventHandler }

// TODO: I think I'll need a service for reporting health status.
// TODO: What about the ObservationEventLogger? And the time measurement in KeywordSetComposer?
// TODO: Clean up logging.

class Activator extends BundleActivator {
  private val logger = Logger.getLogger(this.getClass.getName)

  private var fiber: Option[FiberIO[Unit]]                             = None
  private var observationStateEventQ: Queue[IO, ObservationStateEvent] = null
  private var epicsReaderRef: Ref[IO, Option[EpicsReader]]             = null
  private var statusDbRef: Ref[IO, Option[StatusDatabaseService]]      = null

  // The option bit for the trackers is odd...Does tracking fail at times?
  var epicsTracker: Option[ServiceTracker[EpicsReader, Unit]]            = None
  var statusTracker: Option[ServiceTracker[StatusDatabaseService, Unit]] = None
  var obsEventSvc: Option[ServiceRegistration[_]]                        = None

  private def setIOVars(
    obsStateQ: Queue[IO, ObservationStateEvent],
    epicsRef:  Ref[IO, Option[EpicsReader]],
    statusRef: Ref[IO, Option[StatusDatabaseService]]
  ): Unit = {
    observationStateEventQ = obsStateQ
    epicsReaderRef = epicsRef
    statusDbRef = statusRef
  }

  override def start(context: BundleContext): Unit = {
    logger.info("Starting GDS")

    // TODO: Put these into a config file. Could also register an OSGI service to use the OSGI config mechanism
    val seqexecPort = context.getProperty("gds.seqexec.server.port").toInt
    val configFile  = context.getProperty("gds.keywordsConfiguration")

    // NOTE: This does a sys.error on errors
    val keywordConfig = loadKeywordConfig(configFile)

    val makeIOVars = for {
      obsStateQ <- Queue.unbounded[IO, ObservationStateEvent]
      epicsRef  <- Ref.of[IO, Option[EpicsReader]](none[EpicsReader])
      statusRef <- Ref.of[IO, Option[StatusDatabaseService]](none)
      _          = setIOVars(obsStateQ, epicsRef, statusRef)
    } yield ()

    makeIOVars.unsafeRunSync()

    epicsTracker = Option(Tracker.track[EpicsReader, Unit](context) { eread =>
      logger.info("EpicsReader service arriving")
      epicsReaderRef.set(eread.some).unsafeRunSync()
    } { _ =>
      logger.info("EpicsReader service going away")
      epicsReaderRef.set(none).unsafeRunSync()
    })
    epicsTracker.foreach(_.open(true))

    statusTracker = Option(Tracker.track[StatusDatabaseService, Unit](context) { sds =>
      logger.info("StatusDBService arriving")
      statusDbRef.set(sds.some).unsafeRunSync()
    } { _ =>
      logger.info("StatusDBService going away")
      epicsReaderRef.set(none).unsafeRunSync()
    })
    statusTracker.foreach(_.open(true))

    val eventAdminProps = new util.Hashtable[String, String]()
    eventAdminProps.put(EventConstants.EVENT_TOPIC, ObservationEventReceiver.ObsEventTopic)
    obsEventSvc = context
      .registerService(classOf[EventHandler].getName,
                       new ObservationEventReceiver(observationStateEventQ),
                       eventAdminProps
      )
      .some

    val resource =
      Resource.make(
        Main
          .run(keywordConfig, epicsReaderRef, statusDbRef, observationStateEventQ, seqexecPort)
          .start
      )(
        IO.println("Cleanup") >> _.cancel
      )
    val io       = resource.use { f =>
      fiber = f.some
      f.join.void
    }
    io.unsafeRunAndForget()
  }

  override def stop(context: BundleContext): Unit = {
    logger.info("Stopping GDS")
    fiber.foreach(_.cancel.unsafeRunSync())
    fiber = None
    epicsTracker.foreach(_.close())
    epicsTracker = None
    obsEventSvc.foreach(_.unregister())
    obsEventSvc = None
  }

  private def loadKeywordConfig(filename: String): KeywordConfiguration =
    KeywordConfigurationFile.loadConfiguration(filename) match {
      case Right(config) => config
      case Left(es)      =>
        val message =
          if (es.length === 1)
            s"GDS keyword configuration file error: ${es.head}"
          else {
            val eString =
              es.zipWithIndex.map { case (msg, idx) => s"${idx + 1}: $msg" }.mkString_("\n\t")
            s"GDS Keyword Configuration file has ${es.length} error(s):\n\t$eString"
          }
        logger.severe(message)
        sys.error(message)
    }
}
