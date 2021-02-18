package edu.gemini.aspen.gds.postprocessingpolicy

import java.io.File
import java.util.logging.Logger

import edu.gemini.aspen.gds.api.DefaultPostProcessingPolicy

import scala.sys.process._

/**
 * Post Processing Policy to set the owner on the file to fit archiving requirements
 */
class SetOwnershipPolicy(owner: String, sudo: String) extends DefaultPostProcessingPolicy {
  val useSudo: Boolean = sudo.equalsIgnoreCase("true")

  override val priority = 13

  override def fileReady(originalFile: File, processedFile: File):Unit = {
    LOG.info(s"Set file $processedFile ownership to $owner")

    val cmd = s"${if (useSudo) "sudo " else ""}chown $owner $processedFile"
    LOG.info(cmd)

    val result = cmd.!
    if (result != 0) {
      LOG.severe(s"Failed command $cmd")
    }
  }

  override def toString: String = this.getClass.getSimpleName
}

object SetOwnershipPolicy {
  val Log: Logger = Logger.getLogger(this.getClass.getName)
  val Owner: String = "owner"
  val UseSudo: String = "useSudo"
}
