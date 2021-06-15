package edu.gemini.aspen.gds.fits

import cats.effect.Sync
import cats.syntax.all._
import edu.gemini.aspen.gds.fits.FitsType._
import edu.gemini.aspen.gds.syntax.all._
import scala.util.{ Failure, Success, Try }

sealed trait FitsValue {
  def fitsString(format: Option[String]): String
}

object FitsValue {
  // smart constructors
  def intValue(value:     Int): FitsValue     = IntValue(value)
  def doubleValue(value:  Double): FitsValue  = DoubleValue(value)
  def stringValue(value:  String): FitsValue  = StringValue(value)
  def booleanValue(value: Boolean): FitsValue = BooleanValue(value)

  final case class IntValue(value: Int) extends FitsValue {
    def fitsString(format: Option[String]) =
      formatOrNot(value, format, padLeftTo(value.toString, nonCommentWidth))
  }

  object IntValue {
    def parse(s: String): Either[String, FitsValue] =
      s.toIntOption.map(IntValue(_)).toRight(s"Invalid FITS Integer Value: $s")
  }

  final case class DoubleValue(value: Double) extends FitsValue {
    // Infinity and NaN aren't part of the FITS standard. The old GDS appears to
    // use string keywords for them and skip formatting.
    def fitsString(format: Option[String]): String = value match {
      case d if d.isNaN      => StringValue("NAN").fitsString(None)
      case d if d.isInfinite => StringValue("INF").fitsString(None)
      case d                 => formatOrNot(d, format, padLeftTo(d.toString, nonCommentWidth))
    }
  }

  object DoubleValue {
    def parse(s: String): Either[String, FitsValue] =
      s.toDoubleOption.map(DoubleValue(_)).toRight(s"Invalid FITS Double Value: $s")
  }

  final case class StringValue(value: String) extends FitsValue {
    def fitsString(format: Option[String]) = formatOrNot(value, format, nonFormatted)

    private def nonFormatted: String = {
      val quoted = value.replace("'", "''")
      // final ' must be in or after column 20, but no later than 80,
      // and it starts in column 11. So the string itself must be between
      // 8 and 68 characters to account for the single quotes at each end.
      val inner  = quoted.padTo(8, ' ').take(68)
      s"'$inner'"
    }
  }

  object StringValue {
    // currently isn't any validation, but in case we add some.
    def parse(s: String): Either[String, FitsValue] = StringValue(s).asRight
  }

  final case class BooleanValue(value: Boolean) extends FitsValue {
    def fitsString(format: Option[String]) =
      formatOrNot(value, format, padLeftTo(if (value) "T" else "F", nonCommentWidth))
  }

  object BooleanValue {
    // Will we need to accept T and F strings, like is output in the file?
    def parse(s: String): Either[String, FitsValue] =
      s.toBooleanOption.map(BooleanValue(_)).toRight(s"Invalid FITS Boolean Value: $s")
  }

  def parse(valueType: FitsType, s: String): Either[String, FitsValue] = valueType match {
    case IntType     => IntValue.parse(s)
    case DoubleType  => DoubleValue.parse(s)
    case StringType  => StringValue.parse(s)
    case BooleanType => BooleanValue.parse(s)
  }

  /**
   * *********************
   * Helper methods
   * *********************
   */

  private val logger = java.util.logging.Logger.getLogger(this.getClass.getName)

  val nonCommentWidth = 20
  protected def padLeftTo(s: String, len: Int) = s"%${len}s".format(s)

  protected def formatOrNot[A](
    value:  A,
    format: Option[String],
    orNot:  => String
  ): String = format match {
    case Some(f) => tryFormat(value, f, orNot)
    case None    => orNot
  }

  // Note: In old GDS, use of a format string seems to have forced the keyword to be a
  //       string keyword type.
  protected def tryFormat[A](value: A, format: String, onFail: => String): String = Try(
    String.format(format, value)
  ) match {
    case Failure(exception) =>
      logger.warning(
        s"Could not properly format value '$value' with formatter '$format': ${exception.getMessage}"
      )
      onFail
    case Success(s)         => FitsValue.StringValue(s).fitsString(None)
  }

  /**
   * *********************
   * Methods adapted from the old OneItemKeywordValueActor for dealing with unknown values from Java land.
   * Ugliness ensues...
   * *********************
   */
  def fromAny[F[_]](fitsType: FitsType, value: Any)(implicit F: Sync[F]): F[FitsValue] =
    fitsType match {
      case IntType     => intFromAny(value)
      case DoubleType  => doubleFromAny(value)
      case StringType  => FitsValue.stringValue(value.toString).pure
      case BooleanType => boolFromAny(value)
    }

  def intFromAny[F[_]](value: Any)(implicit F: Sync[F]): F[FitsValue] = value match {
    case x: java.lang.Long    =>
      logger.warningF(s"Possible loss of precision converting $x to integer") >> intValue(
        x.intValue
      ).pure
    case x: java.lang.Integer => intValue(x.intValue).pure
    case x: java.lang.Short   => intValue(x.intValue).pure
    case x: java.lang.Byte    => intValue(x.intValue).pure
    case _                    => misMatchError(value)
  }

  def doubleFromAny[F[_]](value: Any)(implicit F: Sync[F]): F[FitsValue] = value match {
    case x: java.lang.Number => doubleValue(x.doubleValue).pure
    case _                   => misMatchError(value)
  }

  def boolFromAny[F[_]](value: Any)(implicit F: Sync[F]): F[FitsValue] = value match {
    case x: java.lang.Number => booleanValue(x.doubleValue != 0.0).pure
    case x: String
        if x.equalsIgnoreCase("false") || x
          .equalsIgnoreCase("f") || x.equalsIgnoreCase("0") || x.isEmpty =>
      booleanValue(false).pure
    case _: String           => booleanValue(true).pure
    case x: Boolean          => booleanValue(x).pure
    case _                   => misMatchError(value)
  }

  def misMatchError[F[_]](value: Any)(implicit F: Sync[F]): F[FitsValue] =
    F.raiseError(new Throwable(s"Unable to convert value from Epics: $value"))
}
