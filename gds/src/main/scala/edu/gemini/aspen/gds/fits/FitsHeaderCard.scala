package edu.gemini.aspen.gds.fits

import cats.data.Chain

final case class FitsHeaderCard(
  keyword: FitsKeyword,
  value:   FitsValue,
  comment: Option[String],
  format:  Option[String]
) {

  def bytes: Chain[Byte] = {
    val kw          = keyword.key.padTo(8, ' ').take(8)
    val valueString = value.fitsString(format)
    val base        = s"$kw= $valueString"
    val withComment = comment.fold(base)(cmnt => s"$base / $cmnt")
    val fullStr     = withComment.padTo(80, ' ').take(80)

    Chain.fromSeq(fullStr.toList.map(_.toByte))
  }
}
