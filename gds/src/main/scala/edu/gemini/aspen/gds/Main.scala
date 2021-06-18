package edu.gemini.aspen.gds

import cats.effect._
import cats.effect.std._
import cats.syntax.all._
import fs2.Stream
import scala.concurrent.duration._

import edu.gemini.aspen.gds.configuration.KeywordConfiguration
import edu.gemini.aspen.gds.fits.FitsFileProcessor
import edu.gemini.aspen.gds.keywords._
import edu.gemini.aspen.gds.observations.{ ObservationManager, ObservationStateEvent }
import edu.gemini.aspen.gds.seqexec.SeqexecServer
import edu.gemini.aspen.giapi.data.DataLabel
import edu.gemini.aspen.giapi.status.StatusDatabaseService
import edu.gemini.epics.EpicsReader

object Main {
  val purgeRate = 10.minutes // TODO: Add to configuration

  val stream: Stream[IO, Unit] =
    Stream.repeatEval(IO(println(java.time.LocalTime.now))).metered(30.second) //  .take(5)

  def run(
    keywordConfig:  KeywordConfiguration,
    epicsReaderRef: Ref[IO, Option[EpicsReader]],
    statusDbRef:    Ref[IO, Option[StatusDatabaseService]],
    obsStateQ:      Queue[IO, ObservationStateEvent],
    seqexecPort:    Integer
  ): IO[Unit] =
    for {
      fitsQ        <- Queue.unbounded[IO, (DataLabel, List[CollectedKeyword])]
      kwMgr        <- KeywordManager[IO](
                        EpicsKeywordCollector(epicsReaderRef, keywordConfig),
                        StatusKeywordCollector(statusDbRef, keywordConfig),
                        PropertyKeywordCollector(keywordConfig),
                        ConstantKeywordCollector(keywordConfig)
                      )
      obsMgr       <- ObservationManager(kwMgr, obsStateQ, fitsQ)
      fitsProcessor = FitsFileProcessor[IO](keywordConfig)
      seqexecServer = SeqexecServer(obsStateQ, seqexecPort)
      obsPurge      = Stream
                        .fixedDelay[IO](purgeRate)
                        .foreach(_ => obsStateQ.offer(ObservationStateEvent.PurgeStale))
      obsProcess    = Stream
                        .fromQueueUnterminated(obsStateQ)
                        .foreach(obsMgr.process)
      fitsProcess   = Stream.fromQueueUnterminated(fitsQ).foreach { case (dl, kwl) =>
                        fitsProcessor.processFile(dl, kwl)
                      }
      tempStream    = // just to show something happening in the console
        Stream.repeatEval(IO(println(java.time.LocalTime.now))).metered(5.second)
      app          <- Stream(seqexecServer,
                             obsPurge,
                             obsProcess,
                             fitsProcess,
                             tempStream
                      ).parJoinUnbounded.compile.drain
    } yield app
}
