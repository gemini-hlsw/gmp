package edu.gemini.gpi.observationstatus

import java.util.logging.Logger

import edu.gemini.aspen.giapi.status.impl.BasicStatus
import edu.gemini.aspen.giapi.status.setter.StatusSetter
import edu.gemini.aspen.giapi.status.{StatusItem, StatusHandler}
import edu.gemini.aspen.giapi.util.jms.status.StatusGetter
import edu.gemini.gmp.top.Top
import edu.gemini.jms.api.{JmsProvider, JmsArtifact}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Class that listens for gpi specific variables and produce new ones. It does calculations that the status translator is unable to do
 */
class GpiItemTranslator(top: Top, statusSetter: StatusSetter) extends StatusHandler with JmsArtifact {

  val LOG = Logger.getLogger(classOf[GpiItemTranslator].getName)

  val getter: StatusGetter = new StatusGetter("GPI Special Status Translator")
  val irStatus = top.buildStatusItemName("artificialSourceIR")
  val visibleStatus = top.buildStatusItemName("artificialSourceVIS")
  val scStatus = top.buildStatusItemName("artificialSourceSCstate")
  val scPower = top.buildStatusItemName("artificialSourceSCpower")
  val lightOnStatus = top.buildStatusItemName("artificialLight")

  case class LightOn(ir: Boolean = false, visible: Boolean = false, scPower: Float = 0.0f, scState: Boolean = false) {
    def isOn = if (ir || visible || (scState && scPower > 0f)) 1 else 0
  }

  var current = LightOn()

  override def getName = "GPI Item translator"

  override def update[T](item: StatusItem[T]) = {
    if (item.getName == irStatus) {
      current = current.copy(ir = item.getValue.toString == "1")
      updateStatus()
    }
    if (item.getName == visibleStatus) {
      current = current.copy(visible = item.getValue.toString == "1")
      updateStatus()
    }
    if (item.getName == scStatus) {
      current = current.copy(scState = item.getValue.toString == "1")
      updateStatus()
    }
    if (item.getName == scPower) {
      current = current.copy(scPower = item.getValue.asInstanceOf[Float])
      updateStatus()
    }
  }

  private def updateStatus():Unit = {
    statusSetter.setStatusItem(new BasicStatus[Int](lightOnStatus, current.isOn))
  }

  override def startJms(provider: JmsProvider):Unit = {
    getter.startJms(provider)
    import scala.jdk.CollectionConverters._
    Future.apply {
      for {
        si <- getter.getAllStatusItems
      } update(si)
    }
  }


  override def stopJms():Unit = {}
}
