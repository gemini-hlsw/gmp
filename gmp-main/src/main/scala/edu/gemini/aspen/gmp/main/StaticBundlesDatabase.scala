package edu.gemini.aspen.gmp.main

import com.google.common.util.concurrent.AtomicDouble
import edu.gemini.aspen.gmp.health.BundlesDatabase

/**
 * Replacement for the OSGi BundlesDatabaseImpl, which reported the fraction of
 * ACTIVE bundles. Without a bundle lifecycle every component either started or
 * the process failed to boot, so health is constant 1.0.
 */
object StaticBundlesDatabase extends BundlesDatabase {
  private val percentage = new AtomicDouble(1.0)

  override def getPercentageActive: AtomicDouble = percentage
}
