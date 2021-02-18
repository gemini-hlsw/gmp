package edu.gemini.gmp.status.translator

import edu.gemini.aspen.giapi.status.StatusItem
import edu.gemini.aspen.giapi.status.setter.StatusSetterImpl
import edu.gemini.gmp.top.Top
import edu.gemini.jms.api.JmsArtifact
import edu.gemini.jms.api.JmsProvider
import javax.jms.JMSException
import java.util.HashMap
import java.util.Map
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

/**
 * Class JmsStatusItemTranslatorImpl will publish status items translations over JMS
 *
 * @author Nicolas A. Barriga
 *         Date: 4/5/12
 */
case class JmsStatusItemTranslatorImpl(to: Top, xmlFileNam: String) extends AbstractStatusItemTranslator(to, xmlFileNam) with JmsArtifact with StatusItemTranslator {
  private final val LOG: Logger = Logger.getLogger(classOf[JmsStatusItemTranslatorImpl].getName)

   def start:Unit = {
    //super.start
    import scala.jdk.CollectionConverters._
    for (status <- config.statuses) {
      setters.put(to.buildStatusItemName(status.getOriginalName), new StatusSetterImpl(this.getName + status.getOriginalName, to.buildStatusItemName(status.getOriginalName)))
    }
    validateDone.set(true)
    //initItems
  }

  /**
   * Connect JMS on the StatusSetters
   */
  private def initSetters:Unit = {
    /*val executor: ScheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1)
    executor.execute(new Runnable {
      def run:Unit = {
        waitFor(validateDone)
        import scala.jdk.CollectionConverters._
        for (ss <- setters.values) {
          try {
            ss.startJms(provider)
          }
          catch {
            case e: JMSException => {
              LOG.log(Level.SEVERE, e.getMessage, e)
            }
          }
        }
        jmsStarted.set(true)
      }
    })*/
  }

  def stop:Unit = {
    validateDone.set(false)
    //super.stop
  }

  override def startJms(provider: JmsProvider):Unit = {
    getter.startJms(provider)
    this.provider = provider
    initSetters
  }

  override def stopJms:Unit = {
    //jmsStarted.set(false)
    getter.stopJms
    import scala.jdk.CollectionConverters._
    for (ss <- setters.values.asScala) {
      ss.stopJms
    }
  }

  def update[T](item: StatusItem[T]):Unit = {
    import scala.jdk.CollectionConverters._
    for (newItem <- translate(item)) {
      try {
        setters.get(item.getName).setStatusItem(newItem)
      }
      catch {
        case e: JMSException => {
          //LOG.log(Level.SEVERE, e.getMessage, e)
        }
      }
    }
  }

  private final val setters: Map[String, StatusSetterImpl] = new HashMap[String, StatusSetterImpl]
  private var provider: JmsProvider = null
  private final val validateDone: AtomicBoolean = new AtomicBoolean(false)
}