package edu.gemini.aspen.gds.staticheaderreceiver

import edu.gemini.aspen.gds.keywords.database.ProgramIdDatabase
import org.apache.xmlrpc.XmlRpcRequest
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory.RequestProcessorFactory
import org.apache.xmlrpc.server.{PropertyHandlerMapping, RequestProcessorFactoryFactory, XmlRpcHandlerMapping}
import org.apache.xmlrpc.webserver.XmlRpcServlet
import org.ops4j.pax.web.service.WebContainer
import org.osgi.service.event.EventAdmin

import scala.jdk.CollectionConverters._
import scala.collection._

trait HeaderReceiver

class SeqexecHeaderServlet(keywordsDatabase: TemporarySeqexecKeywordsDatabase, programIdDB: ProgramIdDatabase, webContainer: WebContainer, publisher: EventAdmin) extends XmlRpcServlet with HeaderReceiver {
  val initParams = mutable.Map("enabledForExtensions" -> "true")
  // Register XMLRPC Handler
  webContainer.registerServlet(this, Array("/xmlrpc/*"), initParams, null)

  override def newXmlRpcHandlerMapping(): XmlRpcHandlerMapping = {
    val phm = new PropertyHandlerMapping()
    phm.setVoidMethodEnabled(true)
    phm.setRequestProcessorFactoryFactory(new XmlRpcReceiverProcessFactoryFactory())
    phm.addHandler("HeaderReceiver", classOf[XmlRpcReceiver])
    phm
  }

  /**
   * XMLRPC Process factories */
  class XmlRpcReceiverProcessFactoryFactory extends RequestProcessorFactoryFactory {
    def getRequestProcessorFactory(c: Class[_]) = new XmlRpcReceiverProcessFactory
  }

  class XmlRpcReceiverProcessFactory extends RequestProcessorFactory {
    def getRequestProcessor(r: XmlRpcRequest) = new XmlRpcReceiver(keywordsDatabase, programIdDB, publisher)
  }

}