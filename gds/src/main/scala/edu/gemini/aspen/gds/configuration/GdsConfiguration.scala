package edu.gemini.aspen.gds.configuration

import fs2.io.file.Path
import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import scala.concurrent.duration._

final case class ObservationConfig(
  cleanupRate:  FiniteDuration,
  lifespan:     FiniteDuration,
  eventRetries: RetryConfig
)

final case class SetOwnerConfig(owner: String, useSudo: Boolean)
final case class SetPermissionsConfig(permissions: String, useSudo: Boolean)
final case class FitsConfig(
  sourceDir:      Path,
  destDir:        Path,
  addSuffix:      Boolean,
  setOwner:       Option[SetOwnerConfig],
  setPermissions: Option[SetPermissionsConfig],
  deleteOriginal: Boolean
)

final case class RetryConfig(retries: Int, sleep: FiniteDuration)
final case class GdsConfiguration(
  keywords:       List[KeywordConfigurationItem],
  observation:    ObservationConfig,
  keywordRetries: RetryConfig,
  seqexecPort:    Port,
  seqexecHost:    Host,
  fitsConfig:     FitsConfig
)
