package edu.gemini.giapi.clients.lib.commands

import edu.gemini.aspen.giapi.commands._
import edu.gemini.aspen.gmp.commands.jms.client.CommandSenderClient
import org.junit.runner.RunWith
import org.junit.Assert._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.junit.JUnitRunner
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

@RunWith(classOf[JUnitRunner])
class CommandsTest extends AnyFunSuite with MockitoSugar with ScalaFutures {
  test("no reply") {
    val sender = mock[CommandSenderClient]
    val command = new Command(SequenceCommand.TEST, Activity.PRESET)
    when(sender.sendCommand(any(classOf[Command]), any(classOf[CompletionListener]), anyLong())).thenReturn(HandlerResponse.NOANSWER)

    val r = operationResult(sender, command, None)

    whenReady(r) { x =>
      assertTrue(x.isError)
    }
  }
}
