package edu.gemini.aspen.gds.osgi

import cats.syntax.all._
import cats.effect.{ Deferred, FiberIO, IO, Ref, Resource }
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import edu.gemini.aspen.gds.Main
import edu.gemini.aspen.gds.configuration.{ GDSConfigurationServiceFactory, GdsConfiguration }
import edu.gemini.aspen.gds.observations.{ ObservationEventReceiver, ObservationStateEvent }
import edu.gemini.aspen.giapi.data.{ DataLabel, ObservationEvent, ObservationEventHandler }
import edu.gemini.aspen.giapi.status.StatusDatabaseService
import edu.gemini.aspen.gmp.services.PropertyHolder
import edu.gemini.epics.EpicsReader
import java.util
import java.util.logging.Logger
import edu.gemini.util.osgi.Tracker
import org.osgi.framework.{ BundleActivator, BundleContext, Constants, ServiceRegistration }
import org.osgi.util.tracker.ServiceTracker
import org.osgi.service.cm.ManagedServiceFactory
import scala.concurrent.duration._

// TODO: Do we need to implement a health service? If not, maybe remove GDS from the health monitor
// TODO: And the time measurement in KeywordSetComposer?
// TODO: Clean up logging.

class Activator extends BundleActivator {
  private val logger = Logger.getLogger(this.getClass.getName)

  private var fiber: Option[FiberIO[Unit]]                             = None
  private var observationStateEventQ: Queue[IO, ObservationStateEvent] = null
  private var epicsReaderRef: Ref[IO, Option[EpicsReader]]             = null
  private var statusDbRef: Ref[IO, Option[StatusDatabaseService]]      = null
  private var configDeferred: Deferred[IO, GdsConfiguration]           = null

  // The option bit for the trackers is odd...Does tracking fail at times?
  var epicsTracker: Option[ServiceTracker[EpicsReader, Unit]]                     = None
  var statusTracker: Option[ServiceTracker[StatusDatabaseService, Unit]]          = None
  var propTracker: Option[ServiceTracker[PropertyHolder, ServiceRegistration[_]]] = None
  var obsEventSvc: Option[ServiceRegistration[_]]                                 = None

  private def setIOVars(
    obsStateQ: Queue[IO, ObservationStateEvent],
    epicsRef:  Ref[IO, Option[EpicsReader]],
    statusRef: Ref[IO, Option[StatusDatabaseService]],
    configDef: Deferred[IO, GdsConfiguration]
  ): Unit = {
    observationStateEventQ = obsStateQ
    epicsReaderRef = epicsRef
    statusDbRef = statusRef
    configDeferred = configDef
  }

  override def start(context: BundleContext): Unit = {
    logger.info("GDS Starting")

    val makeIOVars = for {
      obsStateQ <- Queue.unbounded[IO, ObservationStateEvent]
      epicsRef  <- Ref.of[IO, Option[EpicsReader]](none[EpicsReader])
      statusRef <- Ref.of[IO, Option[StatusDatabaseService]](none)
      configDef <- Deferred[IO, GdsConfiguration]
      _          = setIOVars(obsStateQ, epicsRef, statusRef, configDef)
    } yield ()

    makeIOVars.unsafeRunSync()

    epicsTracker = Option(Tracker.track[EpicsReader, Unit](context) { eread =>
      epicsReaderRef.set(eread.some).unsafeRunSync()
    } { _ =>
      epicsReaderRef.set(none).unsafeRunSync()
    })
    epicsTracker.foreach(_.open(true))

    statusTracker = Option(Tracker.track[StatusDatabaseService, Unit](context) { sds =>
      statusDbRef.set(sds.some).unsafeRunSync()
    } { _ =>
      epicsReaderRef.set(none).unsafeRunSync()
    })
    statusTracker.foreach(_.open(true))

    // If the property holder goes away and comes back with new config, the GdsConfigurationFactory
    // will be restarted, but deferred will already have been fulfulled, so GDS will not get any
    // new properties for PropertyHolder.
    propTracker = Option(
      Tracker.track[PropertyHolder, ServiceRegistration[_]](context) { ph =>
        val configProps = new util.Hashtable[String, String]()
        configProps.put(Constants.SERVICE_PID, "edu.gemini.aspen.gds.GdsConfiguration")
        context
          .registerService(
            classOf[ManagedServiceFactory].getName,
            new GDSConfigurationServiceFactory(ph, handleConfigResult(context)),
            configProps
          )
      }(_.unregister())
    )
    propTracker.foreach(_.open(true))

    obsEventSvc = context
      .registerService(classOf[ObservationEventHandler].getName,
                       new ObservationEventReceiver(handleObsEvent),
                       new util.Hashtable[String, String]()
      )
      .some

    // wait for config and start running if we receive it. Otherwise timeout and stop GDS
    val run = configDeferred.get.timeout(5.seconds).attempt.flatMap {
      case Left(_)       =>
        IO {
          logger.severe("GDS timed out waiting for configuration. Shutting down.")
          // Trying to cancel the fiber in stop() will result in stop() never completing.
          // The fiber is stopping on it's own, anyway.
          fiber = none
          context.getBundle().stop()
        }.void
      case Right(config) =>
        Main.run(config, epicsReaderRef, statusDbRef, observationStateEventQ)
    }

    val resource = Resource.make(run.start)(_.cancel)
    val io       = resource.use { f =>
      fiber = f.some
      f.join.void
    }
    io.unsafeRunAndForget()
  }

  override def stop(context: BundleContext): Unit = {
    logger.info("GDS Stopping")
    fiber.foreach(_.cancel.unsafeRunSync())
    fiber = None
    epicsTracker.foreach(_.close())
    epicsTracker = None
    statusTracker.foreach(_.close())
    statusTracker = None
    propTracker.foreach(_.close())
    propTracker = None
    obsEventSvc.foreach(_.unregister())
    obsEventSvc = None
  }

  private def handleObsEvent(dataLabel: DataLabel, event: ObservationEvent): Unit =
    observationStateEventQ
      .offer(ObservationStateEvent.AddObservationEvent(dataLabel, event))
      .unsafeRunSync()

  private def handleConfigResult(context: BundleContext)(result: Option[GdsConfiguration]): Unit =
    result match {
      case Some(config) => configDeferred.complete(config).void.unsafeRunSync()
      case None         =>
        logger.severe("GDS stopping itself due to bad configuration.")
        context.getBundle().stop()
    }
}
