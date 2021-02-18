package edu.gemini.giapi.clients.lib

import java.io.{ByteArrayOutputStream, InputStream}

import com.jcraft.jsch._
import edu.gemini.aspen.giapi.commands.HandlerResponse.Response
import edu.gemini.giapi.clients.lib.commands._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.io.Source
import scala.util.{Failure, Success}

package object sshexec {

  trait MessageListener {
    def newLine(line: String): Unit
    def newErrorLine(line: String): Unit
  }

  def sshexec(command: String, listener: MessageListener, username: String, host: String, key: InputStream, knownHosts: InputStream): CommandOperationResult = {
    val p = Promise[CommandResult]()
    val r = Future.apply {
      val jsch = new JSch
      val session = jsch.getSession(username, host, 22)
      jsch.setKnownHosts(knownHosts)
      val privateKey = Source.fromInputStream(key).getLines().mkString("\n").getBytes
      jsch.addIdentity(username, privateKey, Array[Byte](), Array[Byte]())
      session.connect()

      val channel = session.openChannel("exec")
      channel match {
        case x: ChannelExec =>
          x.setCommand(command)
          x.connect()
          val out = new ByteArrayOutputStream()
          x.setErrStream(out)
          Source.fromInputStream(x.getInputStream).getLines().foreach(listener.newLine)
          if (x.getExitStatus == 0) {
            p.complete(Success(Completed(Response.COMPLETED)))
          } else {
            listener.newErrorLine(out.toString)
            p.complete(Success(Error(Response.ERROR, s"Error executing command $command")))
          }
      }
    }
    r.onComplete {
      case Failure(f) =>
        listener.newErrorLine(f.getMessage)
        p.complete(Success(Error(Response.ERROR, s"Error executing command $command: ${f.getMessage}")))
      case Success(_) =>
    }
    r.map(_ => Accepted(Some(command), Response.ACCEPTED, p.future))
  }
}
