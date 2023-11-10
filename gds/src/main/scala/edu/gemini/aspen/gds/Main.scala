package edu.gemini.aspen.gds

import cats.effect._
import cats.effect.kernel.RefSource
import cats.effect.std._
import cats.syntax.all._
import fs2.Stream
import scala.concurrent.duration._

import edu.gemini.aspen.gds.configuration.GdsConfiguration
import edu.gemini.aspen.gds.fits.FitsFileProcessor
import edu.gemini.aspen.gds.keywords._
import edu.gemini.aspen.gds.observations.{ ObservationManager, ObservationStateEvent }
import edu.gemini.aspen.gds.seqexec.SeqexecServer
import edu.gemini.aspen.giapi.data.DataLabel
import edu.gemini.aspen.giapi.status.StatusDatabaseService
import edu.gemini.epics.EpicsReader

object Main {
  val stream: Stream[IO, Unit] =
    Stream.repeatEval(IO(println(java.time.LocalTime.now))).metered(30.second) //  .take(5)

  def run(
    config:         GdsConfiguration,
    epicsReaderRef: RefSource[IO, Option[EpicsReader]],
    statusDbRef:    RefSource[IO, Option[StatusDatabaseService]],
    obsStateQ:      Queue[IO, ObservationStateEvent]
  ): IO[Unit] =
    for {
      fitsQ        <- Queue.unbounded[IO, (DataLabel, List[CollectedKeyword])]
      keywordConfig = config.keywords
      kwMgr        <- KeywordManager[IO](
                        config.keywordRetries,
                        EpicsKeywordCollector(epicsReaderRef, keywordConfig),
                        StatusKeywordCollector(statusDbRef, keywordConfig),
                        PropertyKeywordCollector(keywordConfig),
                        ConstantKeywordCollector(keywordConfig)
                      )
      obsMgr       <- ObservationManager(config.observation, kwMgr, obsStateQ, fitsQ)
      fitsProcessor = FitsFileProcessor[IO](config.fitsConfig, keywordConfig)
      seqexecServer = SeqexecServer(obsStateQ, config.seqexecHost, config.seqexecPort)
      obsPurge      = Stream
                        .fixedDelay[IO](config.observation.cleanupRate)
                        .foreach(_ => obsStateQ.offer(ObservationStateEvent.PurgeStale))
      obsProcess    = Stream
                        .fromQueueUnterminated(obsStateQ)
                        .foreach(obsMgr.process)
      fitsProcess   = Stream.fromQueueUnterminated(fitsQ).foreach { case (dl, kwl) =>
                        fitsProcessor.processFile(dl, kwl)
                      }
      app          <- Stream(seqexecServer, obsPurge, obsProcess, fitsProcess).parJoinUnbounded.compile.drain
    } yield app
}
