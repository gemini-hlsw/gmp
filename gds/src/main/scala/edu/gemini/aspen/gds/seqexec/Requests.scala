package edu.gemini.aspen.gds.seqexec

import edu.gemini.aspen.gds.fits._
import edu.gemini.aspen.giapi.data.DataLabel
import io.circe.{ Decoder, HCursor }
import scala.util.Try

private[seqexec] final case class KeywordValue(keyword: FitsKeyword, value: FitsValue)

private[seqexec] final case class KeywordRequest(
  dataLabel: DataLabel,
  keywords:  List[KeywordValue]
)

private[seqexec] final case class OpenObservationRequest(
  programId: String,
  dataLabel: DataLabel,
  keywords:  List[KeywordValue]
)

private[seqexec] final case class DataLabelRequest(
  dataLabel: DataLabel
)

object Decoders {
  implicit val dataLabelDecoder: Decoder[DataLabel]     =
    Decoder.decodeString.emapTry(s => Try(new DataLabel(s)))
  implicit val fitsKeywordDecoder: Decoder[FitsKeyword] = Decoder.decodeString.emap(FitsKeyword(_))
  implicit val fitsTypeDecoder: Decoder[FitsType]       = Decoder.decodeString.emap(FitsType.fromString)

  // The incoming json has 3 fields
  implicit val keywordDecoder: Decoder[KeywordValue] = new Decoder[KeywordValue] {
    final def apply(c: HCursor): Decoder.Result[KeywordValue] =
      for {
        k <- c.downField("keyword").as[FitsKeyword]
        t <- c.downField("value_type").as[FitsType]
        v <- c.downField("value").as[FitsValue](fitsValueDecoder(t))
      } yield KeywordValue(k, v)
  }

  def fitsValueDecoder(fitsType: FitsType): Decoder[FitsValue] =
    Decoder.decodeString.emap(s => FitsValue.parse(fitsType, s))

  implicit val keywordRequestDecoder: Decoder[KeywordRequest] = new Decoder[KeywordRequest] {
    final def apply(c: HCursor): Decoder.Result[KeywordRequest] =
      for {
        dl  <- c.downField("data_label").as[DataLabel]
        kws <- c.downField("keywords").as[List[KeywordValue]]
      } yield KeywordRequest(dl, kws)
  }

  implicit val openObsRqstDecoder: Decoder[OpenObservationRequest] =
    new Decoder[OpenObservationRequest] {
      final def apply(c: HCursor): Decoder.Result[OpenObservationRequest] =
        for {
          pi  <- c.downField("program_id").as[String]
          dl  <- c.downField("data_label").as[DataLabel]
          kws <- c.downField("keywords").as[List[KeywordValue]]
        } yield OpenObservationRequest(pi, dl, kws)
    }

  implicit val dataLabelRqstDecoder: Decoder[DataLabelRequest] =
    new Decoder[DataLabelRequest] {
      final def apply(c: HCursor): Decoder.Result[DataLabelRequest] =
        for {
          dl <- c.downField("data_label").as[DataLabel]
        } yield DataLabelRequest(dl)
    }
}
