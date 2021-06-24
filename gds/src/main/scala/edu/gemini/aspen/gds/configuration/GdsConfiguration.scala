package edu.gemini.aspen.gds.configuration

import scala.concurrent.duration._

final case class ObservationConfig(
  cleanupRate:  FiniteDuration,
  lifespan:     FiniteDuration,
  eventRetries: RetryConfig
)

final case class FitsConfig(sourceDir: String, destDir: String, addSuffix: Boolean)
final case class RetryConfig(retries: Int, sleep: FiniteDuration)

final case class GdsConfiguration(
  keywords:       List[KeywordConfigurationItem],
  observation:    ObservationConfig,
  keywordRetries: RetryConfig,
  seqexecPort:    Int,
  fitsConfig:     FitsConfig
)
