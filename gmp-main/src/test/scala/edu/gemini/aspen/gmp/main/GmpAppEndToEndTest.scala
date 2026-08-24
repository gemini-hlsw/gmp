package edu.gemini.aspen.gmp.main

import edu.gemini.aspen.giapi.status.impl.BasicStatus
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }
import org.junit.Assert._
import org.junit.Test

/**
 * Boots the composition root against a minimal configuration and pushes a
 * status item through the real JMS pipeline: StatusSetter -> broker ->
 * StatusService -> StatusHandlerAggregate -> StatusDatabase. This covers the
 * ground the old Pax Exam integration tests covered for the status path,
 * without a Felix container.
 */
class GmpAppEndToEndTest {

  private def write(dir: Path, name: String, content: String): Unit =
    Files.write(dir.resolve(name), content.getBytes(StandardCharsets.UTF_8))

  private def minimalConfig(): Path = {
    val services = Files.createTempDirectory("gmp-e2e").resolve("services")
    Files.createDirectories(services)
    write(services,
          "edu.gemini.gmp.top.Top-default.cfg",
          "epicsTop=test\ngiapiTop=test\n"
    )
    write(services,
          "edu.gemini.jms.activemq.broker.ActiveMQBrokerComponent-default.cfg",
          """brokerName=gmpe2e
            |brokerUrl=tcp://127.0.0.1:0
            |persistent=false
            |deleteMsgOnStartup=true
            |useAdvisoryMessages=true
            |useJmx=false
            |jmxRmiServerPort=0
            |jmxConnectorPort=0
            |memoryPercentage=0.1
            |maxStorageMB=10
            |maxMessagesLimit=10
            |""".stripMargin
    )
    write(services,
          "edu.gemini.jms.activemq.provider.ActiveMQJmsProvider-default.cfg",
          "brokerUrl=failover:(vm://gmpe2e?create=false)?timeout=4000\n"
    )
    write(services,
          "edu.gemini.aspen.giapi.statusservice.StatusService-default.cfg",
          "serviceName=E2E Status Service\nstatusName=>\n"
    )
    services
  }

  @Test
  def statusItemTravelsThroughJmsIntoTheDatabase(): Unit = {
    val app = new GmpApp(minimalConfig())
    try {
      assertTrue("JMS provider should be configured", app.provider.isDefined)

      // The provider connects asynchronously, so send periodically the way
      // instruments do, until the item shows up in the database.
      val deadline = System.currentTimeMillis() + 15000
      var item: Object = null
      while (item == null && System.currentTimeMillis() < deadline) {
        app.statusSetter.setStatusItem(new BasicStatus[Integer]("test:e2e", 42))
        Thread.sleep(100)
        item = app.statusDatabase.getStatusItem[Integer]("test:e2e")
      }
      assertNotNull("status item should arrive in the status database", item)
      assertEquals(Integer.valueOf(42),
                   app.statusDatabase.getStatusItem[Integer]("test:e2e").getValue
      )
    } finally app.stop()
  }
}
