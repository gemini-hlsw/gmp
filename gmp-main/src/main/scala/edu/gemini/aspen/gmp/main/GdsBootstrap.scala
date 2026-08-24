package edu.gemini.aspen.gmp.main

import cats.effect.{ FiberIO, IO, Ref }
import cats.effect.std.Queue
import cats.effect.unsafe.implicits.global
import cats.syntax.all._
import edu.gemini.aspen.gds.{ Main => GdsMain }
import edu.gemini.aspen.gds.configuration.{ GDSConfigurationServiceFactory, GdsConfiguration }
import edu.gemini.aspen.gds.observations.ObservationStateEvent
import edu.gemini.aspen.giapi.data.{ DataLabel, ObservationEvent, ObservationEventHandler }
import edu.gemini.aspen.giapi.status.StatusDatabaseService
import edu.gemini.aspen.gmp.services.PropertyHolder
import edu.gemini.epics.EpicsReader
import java.util.logging.Logger

/** A running GDS pipeline: the observation-event entry point plus its fiber. */
final class GdsRuntime(
  val observationEventHandler: ObservationEventHandler,
  fiber:                       FiberIO[Unit]
) {
  def stop(): Unit = fiber.cancel.unsafeRunSync()
}

/**
 * Boots the GDS pipeline the way its OSGi Activator used to: validate the
 * configuration (reusing the existing validation in
 * GDSConfigurationServiceFactory) and launch gds Main.run in a fiber. Unlike
 * the Activator there is no 5s wait for ConfigAdmin — configuration is already
 * in hand — and on invalid config GDS just doesn't start (the Activator
 * stopped its own bundle, which amounted to the same thing).
 */
object GdsBootstrap {
  private val logger = Logger.getLogger(GdsBootstrap.getClass.getName)

  def start(
    configProps:    Map[String, String],
    propertyHolder: PropertyHolder,
    epicsReader:    Option[EpicsReader],
    statusDb:       Option[StatusDatabaseService]
  ): Option[GdsRuntime] = {
    var received: Option[GdsConfiguration] = None
    val factory                            =
      new GDSConfigurationServiceFactory(propertyHolder, result => received = result)
    factory.processProperties(configProps)

    received match {
      case None         =>
        logger.severe("GDS not started due to bad configuration.")
        None
      case Some(config) =>
        val runtime = for {
          obsStateQ <- Queue.unbounded[IO, ObservationStateEvent]
          epicsRef  <- Ref.of[IO, Option[EpicsReader]](epicsReader)
          statusRef <- Ref.of[IO, Option[StatusDatabaseService]](statusDb)
          fiber     <- GdsMain.run(config, epicsRef, statusRef, obsStateQ).start
          handler    = new ObservationEventHandler {
                         def onObservationEvent(event: ObservationEvent, label: DataLabel): Unit =
                           obsStateQ
                             .offer(ObservationStateEvent.AddObservationEvent(label, event))
                             .unsafeRunSync()
                       }
        } yield new GdsRuntime(handler, fiber)
        Some(runtime.unsafeRunSync())
    }
  }
}
