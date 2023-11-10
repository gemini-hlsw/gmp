package edu.gemini.aspen.gds.seqexec

import cats.effect._
import cats.effect.std.QueueSink
import cats.syntax.all._
import edu.gemini.aspen.gds.keywords.CollectedKeyword
import edu.gemini.aspen.gds.model.KeywordSource
import edu.gemini.aspen.gds.observations.ObservationStateEvent
import edu.gemini.aspen.gds.observations.ObservationStateEvent._
import fs2.Stream
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.blaze.server._
import org.http4s.server.Router

import Decoders._

object SeqexecServer {

  // The http4s entity decoders
  implicit val kwrEntityDecoder = jsonOf[IO, KeywordRequest]
  implicit val oorEntityDecoder = jsonOf[IO, OpenObservationRequest]
  implicit val dlrEntityDecoder = jsonOf[IO, DataLabelRequest]

  def apply(
    obsStateQ: QueueSink[IO, ObservationStateEvent],
    host:      String,
    port:      Integer
  ): Stream[IO, Unit] = {
    def kwv2Collected(kwv: KeywordValue) =
      CollectedKeyword.Value(kwv.keyword, KeywordSource.SeqExec, none, kwv.value)

    val service = HttpRoutes
      .of[IO] {
        case req @ POST -> Root / "keywords"          =>
          for {
            kwr <- req.as[KeywordRequest]
            _   <- kwr.keywords.traverse { kw =>
                     obsStateQ.offer(AddKeyword(kwr.dataLabel, kwv2Collected(kw)))
                   }
            ok  <- Ok("Success")
          } yield ok
        case req @ POST -> Root / "open-observation"  =>
          for {
            oor <- req.as[OpenObservationRequest]
            _   <-
              obsStateQ.offer(Start(oor.dataLabel, oor.programId))
            _   <- oor.keywords.traverse { kw =>
                     obsStateQ.offer(AddKeyword(oor.dataLabel, kwv2Collected(kw)))
                   }
            ok  <- Ok("Success")
          } yield ok
        case req @ POST -> Root / "close-observation" =>
          for {
            cor <- req.as[DataLabelRequest]
            _   <- obsStateQ.offer(Stop(cor.dataLabel))
            ok  <- Ok("Success")
          } yield ok
        case req @ POST -> Root / "abort-observation" =>
          for {
            cor <- req.as[DataLabelRequest]
            _   <-
              obsStateQ.offer(Abort(cor.dataLabel))
            ok  <- Ok("Success")
          } yield ok
      }

    val httpApp = Router("gds-seqexec" -> service).orNotFound

    BlazeServerBuilder[IO](scala.concurrent.ExecutionContext.Implicits.global)
      .bindHttp(port, host)
      .withHttpApp(httpApp)
      .serve
      .as(())
  }
}
