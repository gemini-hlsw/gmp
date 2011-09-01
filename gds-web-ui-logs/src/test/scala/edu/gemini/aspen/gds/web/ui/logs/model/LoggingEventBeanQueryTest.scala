package edu.gemini.aspen.gds.web.ui.logs.model

import org.junit.Test
import org.junit.Assert._
import org.joda.time.{DateTimeZone, DateTime}
import org.joda.time.format.ISODateTimeFormat

class LoggingEventBeanQueryTest {
  @Test
  def testFormatTimeStamp {
    val dt = new DateTime()
    val formatted = LoggingEventBeanQuery.formatTimeStamp(dt.getMillis)
    val formatter = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC)
    assertEquals(formatter.print(dt), formatted)
  }
}