package edu.gemini.giapi.clients.lib.status

import edu.gemini.aspen.giapi.status.StatusItem
import edu.gemini.aspen.giapi.util.jms.status.StatusGetter
import edu.gemini.jms.api.JmsProvider

import scala.jdk.CollectionConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ItemsStateProvider {

  def retrieveCurrentState(provider: JmsProvider):Future[Iterable[StatusItem[_]]] = {
    val getter = new StatusGetter("GPI Engineering Tool Initial Loader")
    Future.apply {
      getter.startJms(provider)
      val result = Option(getter.getAllStatusItems).map(_.asScala.toList).getOrElse(Nil)
      getter.stopJms()
      result
    }
  }

}
