package edu.gemini.aspen.gds.fits

import cats.syntax.all._
import edu.gemini.aspen.gds.util.Enumerated

sealed abstract class FitsType(val repr: String) extends Product with Serializable

object FitsType {
  case object IntType     extends FitsType("INT")
  case object DoubleType  extends FitsType("DOUBLE")
  case object StringType  extends FitsType("STRING")
  case object BooleanType extends FitsType("BOOLEAN")

  val all: List[FitsType] = List(IntType, DoubleType, StringType, BooleanType)

  val validationRegex: String = all.map(_.repr).mkString("|")

  def fromString(s: String): Either[String, FitsType] =
    Enumerated[FitsType].fromTag(s).fold(s"Invalid FITS Type: $s".asLeft[FitsType])(_.asRight)

  implicit val FitsTypeEnumerated: Enumerated[FitsType] = new Enumerated[FitsType] {
    def all = FitsType.all
    def tag(a:                    FitsType)         = a.repr
    override def unsafeFromTag(s: String): FitsType =
      fromTag(s).getOrElse(sys.error(s"Invalid FitsType: $s"))
  }
}
