package edu.gemini.aspen.gds.api

import edu.gemini.aspen.giapi.data.DataLabel
import java.util.logging.Logger
import scala.collection._
import java.io.File

/**
 * Defines a post processing policy
 */
trait PostProcessingPolicy {
  /**
   * Lower priority policies are applied first (0 - 10)
   */
  val priority = 5

  /**
   * Applies the policy taking a set of proposed headers and returning the set of headers allowed
   *
   * @param dataLabel The data label being processed
   * @param headers Proposed set of headers
   * @return A list of headers to be written to the fits file
   */
  def applyPolicy(dataLabel: DataLabel, headers: immutable.List[CollectedValue[_]]): immutable.List[CollectedValue[_]]

  /**
   * Called after the file is ready for further processing
   *
   * @param originalFile
   * @param processedFile
   */
  def fileReady(originalFile: File, processedFile: File)
}

/**
 * The default error policy
 */
class DefaultPostProcessingPolicy extends PostProcessingPolicy {
  protected val LOG = Logger.getLogger(this.getClass.getName)

  // Let all the original headers to be applied
  override def applyPolicy(dataLabel: DataLabel, headers: immutable.List[CollectedValue[_]]): immutable.List[CollectedValue[_]] = headers

  override def fileReady(originalFile: File, processedFile: File):Unit = {}
}