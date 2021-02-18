package edu.gemini.aspen.gds.staticheaderreceiver

import java.util
import java.util.logging.{Level, Logger}

import edu.gemini.aspen.gds.api.Conversions._
import edu.gemini.aspen.gds.api.GDSObseventHandler
import edu.gemini.aspen.gds.keywords.database.{ProgramIdDatabase, StoreProgramId}
import edu.gemini.aspen.gds.staticheaderreceiver.TemporarySeqexecKeywordsDatabaseImpl.Store
import edu.gemini.aspen.giapi.data.{DataLabel, ObservationEvent}
import org.osgi.service.event.{Event, EventAdmin}

class XmlRpcReceiver(keywordsDatabase: TemporarySeqexecKeywordsDatabase, programIdDB: ProgramIdDatabase, publisher: EventAdmin) {
  protected val LOG: Logger = Logger.getLogger(this.getClass.getName)

  private def sendData(event: ObservationEvent, dataLabel: DataLabel): Unit = {
    val props = new util.HashMap[String, (ObservationEvent, DataLabel)]()
    props.put(GDSObseventHandler.ObsEventKey, (event, dataLabel))
    publisher.postEvent(new Event(GDSObseventHandler.ObsEventTopic, props))
  }
  /**
   * Opening an observation means associating it with a program ID, and telling GDS to wait for
   * someone to close it before writing the FITS file.
   *
   * @param programId The ID of the program in the ODB that specifies this observation
   * @param dataLabel The name of the FITS file to be written
   */
  def openObservation(programId: String, dataLabel: String):Unit = {
    LOG.info(s"Opened Observation, Program ID: $programId Data label: $dataLabel")
    programIdDB ! StoreProgramId(dataLabel, programId)
    sendData(ObservationEvent.EXT_START_OBS, new DataLabel(dataLabel))
  }

  /**
   * Convenience method to save on XMLRPC calls
   *
   * @param programId The ID of the program in the ODB that specifies this observation
   * @param dataLabel The name of the FITS file to be written, and to which keywords must be associated
   * @param keywords An array of Strings, each of which contains three parts, separated by a comma: the keyword name, the data type and the value
   */
  def openObservation(programId: String, dataLabel: String, keywords: Array[Object]):Unit = {
    openObservation(programId, dataLabel)
    storeKeywords(dataLabel, keywords)
  }

  /**
   * Closing an observation means all keywords have been sent, so the FITS file may be updated
   *
   * @param dataLabel The name of the FITS file to be written
   */
  def closeObservation(dataLabel: String):Unit = {
    LOG.info(s"Closed Observation, Data label: $dataLabel")
    sendData(ObservationEvent.EXT_END_OBS, new DataLabel(dataLabel))
  }

  /**
   * Convenience method to save on XMLRPC calls
   *
   * @param dataLabel The name of the FITS file to be written, and to which keywords must be associated
   * @param keywords An array of Strings, each of which contains three parts, separated by a comma: the keyword name, the data type and the value
   */
  def closeObservation(dataLabel: String, keywords: Array[Object]):Unit = {
    storeKeywords(dataLabel, keywords)
    closeObservation(dataLabel)
  }

  /**
   * Associate a keyword to a given data set
   *
   * @param dataLabel The name of the FITS file to be written, and to which keywords must be associated
   * @param keyword The FITS keyword name
   * @param value The FITS keyword value
   */
  def storeKeyword(dataLabel: String, keyword: String, value: String):Unit = {
    LOG.info(s"Data label: $dataLabel Keyword: $keyword Value: $value")
    keywordsDatabase ! Store(dataLabel, keyword, value)
  }

  /**
   * Associate a keyword to a given data set
   *
   * @param dataLabel The name of the FITS file to be written, and to which keywords must be associated
   * @param keyword The FITS keyword name
   * @param value The FITS keyword value
   */
  def storeKeyword(dataLabel: String, keyword: String, value: Double):Unit = {
    LOG.info(s"Data label: $dataLabel Keyword: $keyword Value: $value")
    keywordsDatabase ! Store(dataLabel, keyword, value.asInstanceOf[AnyRef])
  }

  /**
   * Associate a keyword to a given data set
   *
   * @param dataLabel The name of the FITS file to be written, and to which keywords must be associated
   * @param keyword The FITS keyword name
   * @param value The FITS keyword value
   */
  def storeKeyword(dataLabel: String, keyword: String, value: Int):Unit = {
    LOG.info(s"Data label: $dataLabel Keyword: $keyword Value: $value")
    keywordsDatabase ! Store(dataLabel, keyword, value.asInstanceOf[AnyRef])
  }

  /**
   * Associate a keyword to a given data set
   *
   * @param dataLabel The name of the FITS file to be written, and to which keywords must be associated
   * @param keyword The FITS keyword name
   * @param value The FITS keyword value
   */
  def storeKeyword(dataLabel: String, keyword: String, value: Boolean):Unit = {
    LOG.info(s"Data label: $dataLabel Keyword: $keyword Value: $value")
    keywordsDatabase ! Store(dataLabel, keyword, value.asInstanceOf[AnyRef])
  }

  /**
   * Associate keywords to a given data set
   *
   * @param dataLabel The name of the FITS file to be written, and to which keywords must be associated
   * @param keywords An array of Strings, each of which contains three parts, separated by a comma: the keyword name, the data type and the value
   */
  def storeKeywords(dataLabel: String, keywords: Array[Object]):Unit = {
    val regex = """(\w*),(\w*),(.*)""".r
    for {
      keyword <- keywords
      pieces = keyword match {
        case regex(k, t, v) => (k, t, v)
        case _ => ("", "", "")
      }
      if pieces._1.nonEmpty
    } {
      val key = pieces._1.trim().toUpperCase
      val dataType = pieces._2.trim()
      try {
        LOG.info("STORE")
        LOG.info(s"<|${pieces._1},${pieces._2},${pieces._3}|>")
        val value = pieces._3.trim()
        LOG.info(value)

        dataType match {
          case "INT"     => storeKeyword(dataLabel, key, value.toInt)
          case "DOUBLE"  => storeKeyword(dataLabel, key, value.toDouble)
          case "STRING"  => storeKeyword(dataLabel, key, value.toString)
          case "BOOLEAN" => storeKeyword(dataLabel, key, value.toBoolean)
          case x         => LOG.severe(s"Wrong data type: $x")
        }
      } catch {
        case ex: java.lang.NumberFormatException => LOG.log(Level.SEVERE, ex.getMessage, ex)
        case e: Exception                        => LOG.log(Level.SEVERE, e.getMessage, e)
      }
    }
  }
}

