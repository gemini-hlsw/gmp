package edu.gemini.aspen.gds.postprocessingpolicy

import java.io.File

import edu.gemini.aspen.gds.api.DefaultPostProcessingPolicy

/**
 * This policy removes ErrorCollectedValues. It should probably be the last policy applied.
 */
class DeleteOriginalPolicy extends DefaultPostProcessingPolicy {

  override val priority = 12

  override def toString = this.getClass.getSimpleName

  override def fileReady(originalFile: File, processedFile: File):Unit = {
    LOG.info(s"Delete original file $originalFile")
    if (!originalFile.delete()) {
      LOG.warning(s"Could not delete original file $originalFile")
    }
  }
}