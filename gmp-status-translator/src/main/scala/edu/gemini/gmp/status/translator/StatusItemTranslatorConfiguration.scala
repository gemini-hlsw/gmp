package edu.gemini.gmp.status.translator

import javax.xml.bind.JAXBContext
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import java.io.InputStream

import scala.jdk.CollectionConverters._
import edu.gemini.gmp.status.translator.generated.{StatusType, TranslateStatus}

/**
 * Class StatusItemTranslatorConfiguration
 *
 * @author Nicolas A. Barriga
 *         Date: 4/9/12
 */
class StatusItemTranslatorConfiguration(resourceAsStream: InputStream) {
  val jaxbContext = JAXBContext.newInstance(classOf[TranslateStatus])
  val u = jaxbContext.createUnmarshaller
  val factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema")
  val schema = factory.newSchema(this.getClass.getResource("status-translator.xsd"))
  u.setSchema(schema)
  val translatedStatuses = u.unmarshal(new StreamSource(resourceAsStream), classOf[TranslateStatus]).getValue

  def statuses: List[StatusType] =
    translatedStatuses.getStatus.asScala.toList

}