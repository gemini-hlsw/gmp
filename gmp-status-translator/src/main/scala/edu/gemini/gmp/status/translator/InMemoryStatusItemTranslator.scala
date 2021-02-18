package edu.gemini.gmp.status.translator

import edu.gemini.aspen.giapi.status.StatusDatabaseService
import edu.gemini.aspen.giapi.status.StatusItem
import edu.gemini.aspen.giapi.statusservice.StatusHandlerAggregate
import edu.gemini.gmp.top.Top
import java.util.logging.Logger

/**
 * Class LocalStatusItemTranslatorImpl will publish status items translations directly back to the status handler aggregate
 *
 * @author Nicolas A. Barriga
 *         Date: 4/5/12
 */
class InMemoryStatusItemTranslator(top: Top, aggregate: StatusHandlerAggregate, statusDatabase: StatusDatabaseService, xmlFileName: String) extends AbstractStatusItemTranslator(top, xmlFileName) with StatusItemTranslator {
  private final val LOG: Logger = Logger.getLogger(classOf[InMemoryStatusItemTranslator].getName)

   def start:Unit = {
    //super.start()
    initItems
  }

  protected def initItems: Unit = {
    import scala.jdk.CollectionConverters._
    for (item <- statusDatabase.getAll.asScala) {
      update(item)
    }
  }

  def stop:Unit = {
    LOG.info("Start stop")
    //super.stop
    LOG.info("End stop")
  }

  def update[T](item: StatusItem[T]): Unit = {
    for (newItem <- translate(item)) {
      LOG.finer(s"Publishing translated status item: ${newItem.getName}")
      aggregate.update(newItem)
    }
  }

}