package edu.gemini.aspen.gds.fits

import cats.syntax.all._

sealed abstract case class FitsKeyword private (key: String)

object FitsKeyword {
  // Regular expression that FITS keyword must obey
  val KeyFormat = """[\p{Upper}\d-_]{1,8}"""

  def apply(key: String): Either[String, FitsKeyword] =
    if (key.matches(KeyFormat)) (new FitsKeyword(key) {}).asRight
    else s"Invalid FITS keyword $key".asLeft

  def unsafeFromString(key: String): FitsKeyword = apply(key).fold[FitsKeyword](sys.error, identity)
}
