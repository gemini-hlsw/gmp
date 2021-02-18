package edu.gemini.aspen.gds.api

import edu.gemini.aspen.giapi.data.DataLabel
import scala.collection._
import java.io.File

/**
 * Interface for an PostProcessingPolicy that uses OSGi services implementing error policies
 */
trait CompositePostProcessingPolicy extends PostProcessingPolicy

/**
 * OSGi service implementing the CompositePostProcessingPolicy that can use delegates
 */
class CompositePostProcessingPolicyImpl extends DefaultPostProcessingPolicy with CompositePostProcessingPolicy {
  @volatile var policies = immutable.List[PostProcessingPolicy]()

  // Apply all the original headers
  override def applyPolicy(dataLabel: DataLabel, headers: immutable.List[CollectedValue[_]]): immutable.List[CollectedValue[_]] = {
    LOG.info("Applying policies: " + policies.sortWith((a, b) => a.priority < b.priority).foldLeft("")((B, A) => s"$B ${A.getClass.getSimpleName}"))

    //recursive
    applyPolicies(dataLabel, policies.sortWith((a, b) => a.priority < b.priority), headers)
  }

  private def applyPolicies(dataLabel: DataLabel, l: immutable.List[PostProcessingPolicy], headers: immutable.List[CollectedValue[_]]): immutable.List[CollectedValue[_]] = {
    l match {
      case Nil => headers
      case _ => l.last.applyPolicy(dataLabel, applyPolicies(dataLabel, l.init, headers))
    }
  }

  override def fileReady(originalFile: File, processedFile: File):Unit = {
    val sortedPolicies = policies.sortWith((a, b) => a.priority < b.priority)
    for {
      p <- sortedPolicies
    } yield p.fileReady(originalFile, processedFile)
  }

  def addPolicy(ep: PostProcessingPolicy):Unit = {
    policies = ep :: policies
  }

  def removePolicy(ep: PostProcessingPolicy):Unit = {
    policies = policies.filter(_ != ep)
  }

}