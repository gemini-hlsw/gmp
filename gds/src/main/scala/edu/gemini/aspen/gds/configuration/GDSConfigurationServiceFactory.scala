package edu.gemini.aspen.gds.configuration

import cats.data.{ NonEmptyChain, ValidatedNec }
import cats.data.Validated.{ Invalid, Valid }
import cats.syntax.all._
import edu.gemini.aspen.gmp.services.PropertyHolder
import java.nio.file._
import java.util.{ Dictionary }
import java.util.logging.Logger
import org.osgi.service.cm.ManagedServiceFactory
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try }

// Probably an abuse of a ManagedServiceFactory, since it doesn't create any
// services. But, it makes it possible to use OSGi configuration.
class GDSConfigurationServiceFactory(
  propertyHolder: PropertyHolder,
  configHandler:  Option[GdsConfiguration] => Unit
) extends ManagedServiceFactory {
  private val logger         = Logger.getLogger(this.getClass.getName)
  private var receivedConfig = false

  // These values come from the PropertyHolder, not the config file.
  val appendFitsExtKey = "APPEND_FITS_EXTENSION"
  val fitsSrcPathKey   = "DHS_SCIENCE_DATA_PATH"
  val fitsDestPathKey  = "DHS_PERMANENT_SCIENCE_DATA_PATH"

  override def getName = "GDS Configuration Service Factory"

  override def updated(pid: String, properties: Dictionary[String, _]): Unit = {
    logger.info(s"GDS Config factory received configuration with pid: $pid")

    // This check is not completely thread safe, but
    // 1. It probably won't happen
    // 2. It would just result in a second config processing that wouldn't affect the first one.
    if (receivedConfig)
      logger.severe(
        "GDS Received a new configuration. This will have no effect on the running bundle."
      )
    else {
      receivedConfig = true
      Option(properties) match {
        case Some(props) => processProperties(props.asScala.toMap)
        case None        =>
          logger.severe("GdsConfigurationFactory received a null for properties.")
          configHandler(none)
      }
    }
  }

  def processProperties(props: Map[String, _]): Unit = {
    val keywordConfig: ValidatedNec[String, List[KeywordConfigurationItem]] =
      asString(props, "keywordsConfiguration").andThen(
        KeywordConfigurationFile.loadConfiguration(_)
      )

    val cleanupRate    = asDuration(props, "observation.cleanupRate")
    val lifespan       = asDuration(props, "observation.lifespan")
    val eventRetries   = asPosInt(props, "observation.event.retries")
    val eventSleep     = asDuration(props, "observation.event.sleep")
    val keywordRetries = asPosInt(props, "keyword.collection.retries")
    val keywordSleep   = asDuration(props, "keyword.collection.sleep")
    val seqexecPort    = asPosInt(props, "seqexec.server.port")

    val configValidated = (keywordConfig,
                           cleanupRate,
                           lifespan,
                           eventRetries,
                           eventSleep,
                           keywordRetries,
                           keywordSleep,
                           seqexecPort,
                           fitsSrcDir,
                           fitsDestDir,
                           fitsAddSuffix
    ).mapN { case (kc, cr, lf, er, es, kr, ks, sp, fsd, fdd, fas) =>
      GdsConfiguration(kc,
                       ObservationConfig(cr, lf, RetryConfig(er, es)),
                       RetryConfig(kr, ks),
                       sp,
                       FitsConfig(fsd, fdd, fas)
      )
    }

    configValidated match {
      case Invalid(e)    =>
        logErrors(e)
        configHandler(none)
      case Valid(config) => configHandler(config.some)
    }
  }

  override def deleted(pid: String): Unit = ()

  private def asString(props: Map[String, _], key: String): ValidatedNec[String, String] =
    props.get(key).map(_.toString).toValidNec(s"Config value missing for `$key`")

  private def asPosInt(props: Map[String, _], key: String): ValidatedNec[String, Int] =
    asString(props, key).andThen(s =>
      s.toIntOption
        .flatMap(i => if (i > 0) i.some else none)
        .toValidNec(s"Invalid positive integer `$s` for `$key`")
    )

  private def asDuration(props: Map[String, _], key: String): ValidatedNec[String, FiniteDuration] =
    asString(props, key).andThen(s =>
      durationFromString(s).toValidNec(s"Invalid duration `$s` for `$key`")
    )

  private def durationFromString(s: String): Option[FiniteDuration] =
    Try(Duration(s)) match {
      case Failure(_)        => none
      case Success(duration) =>
        Option.when(duration.isFinite)(duration.asInstanceOf[FiniteDuration])
    }

  private def fitsSrcDir: ValidatedNec[String, Path]  = dirFromPropHolder(fitsSrcPathKey)
  private def fitsDestDir: ValidatedNec[String, Path] = dirFromPropHolder(fitsDestPathKey)

  // PropertyHolder shouldn't return null, but...
  private def propHolderValue(key: String): ValidatedNec[String, String] =
    Option(propertyHolder.getProperty(key))
      .toValidNec(s"Value for PropertyHolder sevice key `$key` cannot be null.")

  private def dirFromPropHolder(key: String): ValidatedNec[String, Path] =
    propHolderValue(key).andThen { dir =>
      Try {
        val path = Paths.get(dir)
        if (path.toFile().isDirectory()) path.validNec
        else
          s"Value `$dir` for PropertyHolder service key `$key` is not a valid directory".invalidNec
      } match {
        case Failure(e)     =>
          s"Error creating Path for PropertyHolder service key `$key`, value `$dir`: ${e.getMessage}".invalidNec
        case Success(value) => value
      }
    }

  private def fitsAddSuffix: ValidatedNec[String, Boolean] =
    propHolderValue(appendFitsExtKey).andThen { s =>
      s.toBooleanOption
        .toValidNec(
          s"Invalid boolean `$s` for PropertyHolder service key `$appendFitsExtKey`"
        )
    }

  private def logErrors(errors: NonEmptyChain[String]): Unit = {
    val message =
      if (errors.length === 1)
        s"GDS configuration file error: ${errors.head}"
      else {
        val eString =
          errors.zipWithIndex.map { case (msg, idx) => s"${idx + 1}: $msg" }.mkString_("\n\t")
        s"GDS Configuration file has ${errors.length} errors:\n\t$eString"
      }
    logger.severe(message)
  }
}
