package edu.gemini.aspen.gmp.main

import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }
import java.util.Properties
import java.util.logging.Logger
import scala.jdk.CollectionConverters._
import scala.util.matching.Regex

/**
 * One configuration file from the services directory.
 *
 * Follows the Felix FileInstall naming convention this project used under OSGi:
 * `<pid>.cfg` for singleton configurations and `<factoryPid>-<instance>.cfg`
 * for factory configurations (e.g. `edu.gemini.gmp.top.Top-default.cfg`).
 */
final case class ServiceConfig(
  pid:        String,
  instance:   Option[String],
  properties: Map[String, String],
  file:       Path
) {

  def get(key: String): Option[String] = properties.get(key)

  def stringValue(key: String): Option[String] = get(key)

  def intValue(key: String): Option[Int] = get(key).flatMap(_.trim.toIntOption)

  def doubleValue(key: String): Option[Double] = get(key).flatMap(_.trim.toDoubleOption)

  def booleanValue(key: String): Option[Boolean] = get(key).map(v => "true".equalsIgnoreCase(v.trim))

  def toDictionary: java.util.Dictionary[String, String] = {
    val table = new java.util.Hashtable[String, String]()
    properties.foreach { case (k, v) => table.put(k, v) }
    table
  }
}

/**
 * Loads the `conf/services/@*.cfg` files that used to feed Felix ConfigAdmin.
 * Read once at startup; changing a file requires a restart (see ADR 0001).
 */
final class ServicesConfig(val configs: List[ServiceConfig]) {

  /** All instances of a (factory) pid, e.g. the `-default` one plus any extras. */
  def instances(pid: String): List[ServiceConfig] = configs.filter(_.pid == pid)

  /** The single instance of a pid, when the component only ever has one. */
  def first(pid: String): Option[ServiceConfig] = instances(pid).headOption
}

object ServicesConfig {
  private val logger = Logger.getLogger(classOf[ServicesConfig].getName)

  private val Substitution: Regex = """\$\{([^}]+)\}""".r

  def load(servicesDir: Path): ServicesConfig =
    if (!Files.isDirectory(servicesDir)) {
      logger.severe(s"Services configuration directory not found: $servicesDir")
      new ServicesConfig(Nil)
    } else {
      val files = Files.list(servicesDir).iterator().asScala.toList
        .filter(_.getFileName.toString.endsWith(".cfg"))
        .sortBy(_.getFileName.toString)
      new ServicesConfig(files.map(loadFile))
    }

  private def loadFile(file: Path): ServiceConfig = {
    val props = new Properties()
    val in    = Files.newInputStream(file)
    try props.load(new InputStreamReader(in, StandardCharsets.UTF_8))
    finally in.close()

    val base              = file.getFileName.toString.stripSuffix(".cfg")
    val (pid, instance)   = base.lastIndexOf('-') match {
      case -1 => (base, None)
      case i  => (base.substring(0, i), Some(base.substring(i + 1)))
    }
    val expanded          = props.asScala.toMap.map { case (k, v) => (k, expand(v)) }
    ServiceConfig(pid, instance, expanded, file)
  }

  /**
   * `${prop}` expansion against system properties, matching what FileInstall
   * did at runtime in development. Production files arrive pre-expanded by
   * Maven filtering; unresolvable references are left untouched so they stay
   * visible in logs.
   */
  private def expand(value: String): String =
    Substitution.replaceAllIn(value,
                              m =>
                                Option(System.getProperty(m.group(1))) match {
                                  case Some(v) => Regex.quoteReplacement(v)
                                  case None    => Regex.quoteReplacement(m.matched)
                                }
    )
}
