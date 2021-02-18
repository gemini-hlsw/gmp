package edu.gemini.aspen.gds.postprocessingpolicy

import java.io.File
import java.util.logging.Logger

import edu.gemini.aspen.gds.api.DefaultPostProcessingPolicy

import scala.sys.process._

/**
 * Post Processing Policy to set the permissions on the file to fit archiving requirements
 */
class SetPermissionsPolicy(permissions: String, sudo: String) extends DefaultPostProcessingPolicy {
  val useSudo: Boolean = sudo.equalsIgnoreCase("true")

  override val priority = 12

  override def fileReady(originalFile: File, processedFile: File):Unit = {
    LOG.info(s"Set file $processedFile permissions to $permissions")

    val cmd = s"${if (useSudo) "sudo " else ""}chmod $permissions $processedFile"
    val result = cmd.!
    if (result != 0) {
      LOG.severe(s"Failed command $cmd")
    }
  }

  override def toString: String = this.getClass.getSimpleName
}

object SetPermissionsPolicy {
  val Log: Logger = Logger.getLogger(this.getClass.getName)
  val Permissions: String = "owner"
  val UseSudo: String = "useSudo"
}
