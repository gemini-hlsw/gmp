package edu.gemini.aspen.gds.syntax

import cats.effect.Sync
import java.util.logging.{ Level, Logger }

// TODO: At least in the pax console, the logging config is using the file and line number
//       instead of the logger name. This Ops class results in all of the log messages showing
//       they originate here. Which isn't useful. If the logging config in production uses the
//       logger name, this may not be a problem. Otherwise, I'll probably need to wrap the
//       logger calls in F everywhere logging is done instead of using extension methods.
class LoggerFOps(val logger: Logger) extends AnyVal {
  def finestF[F[_]](m:  String)(implicit sync: Sync[F]): F[Unit] = sync.delay(logger.finest(m))
  def finerF[F[_]](m:   String)(implicit sync: Sync[F]): F[Unit] = sync.delay(logger.finer(m))
  def fineF[F[_]](m:    String)(implicit sync: Sync[F]): F[Unit] = sync.delay(logger.fine(m))
  def infoF[F[_]](m:    String)(implicit sync: Sync[F]): F[Unit] = sync.delay(logger.info(m))
  def warningF[F[_]](m: String)(implicit sync: Sync[F]): F[Unit] = sync.delay(logger.warning(m))
  def severeF[F[_]](m:  String)(implicit sync: Sync[F]): F[Unit] = sync.delay(logger.severe(m))
  def severeF[F[_]](m:  String, e:             Throwable)(implicit sync: Sync[F]): F[Unit] =
    sync.delay(logger.log(Level.SEVERE, m, e))
}

trait ToLoggerFOps {
  implicit def ToLoggerFOps(l: Logger): LoggerFOps = new LoggerFOps(l)
}

object loggerF extends ToLoggerFOps
