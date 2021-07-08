package edu.gemini.aspen.gds.model

case class GdsError(msg: String) extends Throwable(msg, null, true, false)
