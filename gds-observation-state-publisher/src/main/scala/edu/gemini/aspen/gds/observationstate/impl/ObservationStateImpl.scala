package edu.gemini.aspen.gds.observationstate.impl

import java.time.{Duration, LocalDateTime, ZoneId}
import java.util.Date
import java.util.concurrent.TimeUnit._

import com.google.common.cache.CacheBuilder
import edu.gemini.aspen.gds.api.fits.FitsKeyword
import edu.gemini.aspen.gds.api.{CollectedValue, CollectionError}
import edu.gemini.aspen.gds.observationstate.{ObservationInfo, _}
import edu.gemini.aspen.giapi.data.DataLabel

import scala.jdk.CollectionConverters._
import scala.collection.concurrent._
import scala.collection.mutable
import scala.collection.mutable.{HashSet, Set, SynchronizedSet}

class ObservationStateImpl(obsStatePubl: ObservationStatePublisher) extends ObservationStateRegistrar with ObservationStateProvider {
  // expiration of 1 day by default but tests can override it
  def expirationMillis: Int = 24 * 60 * 60 * 1000

  class ObservationState {
    val missingKeywords: mutable.Set[FitsKeyword] = new mutable.HashSet[FitsKeyword] with mutable.SynchronizedSet[FitsKeyword]
    val errorKeywords: mutable.Set[(FitsKeyword, CollectionError.CollectionError)] = new mutable.HashSet[(FitsKeyword, CollectionError.CollectionError)] with mutable.SynchronizedSet[(FitsKeyword, CollectionError.CollectionError)]
    val times: mutable.Set[(AnyRef, Option[Duration])] = new mutable.HashSet[(AnyRef, Option[Duration])] with mutable.SynchronizedSet[(AnyRef, Option[Duration])]
    var started = false
    var ended = false
    var inError = false
    var failed = false
    var timestamp = new Date()
  }

  val obsInfoMap: Map[DataLabel, ObservationState] = CacheBuilder.newBuilder()
    .expireAfterWrite(expirationMillis, MILLISECONDS)
    .build[DataLabel, ObservationState]().asMap()

  override def registerMissingKeyword(label: DataLabel, keywords: Traversable[FitsKeyword]):Unit = {
    obsInfoMap.getOrElseUpdate(label, new ObservationState).missingKeywords ++= keywords
    if (keywords.nonEmpty) {
      obsInfoMap.getOrElseUpdate(label, new ObservationState).inError = true
    }
  }

  //todo: use cause for something
  override def registerError(label: DataLabel, cause: String):Unit = {
    obsInfoMap.getOrElseUpdate(label, new ObservationState).failed = true
    obsStatePubl.publishObservationError(ObservationInfo(label, ObservationError, errorMsg = Option(cause)))
  }

  override def registerCollectionError(label: DataLabel, errors: Traversable[(FitsKeyword, CollectionError.CollectionError)]):Unit = {
    obsInfoMap.getOrElseUpdate(label, new ObservationState).errorKeywords ++= errors
    if (errors.nonEmpty) {
      obsInfoMap.getOrElseUpdate(label, new ObservationState).inError = true
    }
  }

  override def registerTimes(label: DataLabel, times: Traversable[(AnyRef, Option[Duration])]):Unit = {
    obsInfoMap.getOrElseUpdate(label, new ObservationState).times ++= times
  }

  override def endObservation(label: DataLabel, writeTime:Long, collectedValues: Traversable[CollectedValue[_]]):Unit = {
    obsInfoMap.getOrElseUpdate(label, new ObservationState).ended = true
    obsStatePubl.publishEndObservation(ObservationInfo(label, Successful, writeTime = Some(writeTime), collectedValues = collectedValues))
  }

  override def startObservation(label: DataLabel):Unit = {
    obsInfoMap.getOrElseUpdate(label, new ObservationState).started = true
    obsStatePubl.publishStartObservation(label)
  }

  //-----------------------------------------------------------------------

  override def isInError(label: DataLabel): Option[Boolean] = {
    obsInfoMap.get(label) map {_.inError}
  }

  override def isFailed(label: DataLabel): Option[Boolean] = {
    obsInfoMap.get(label) map {_.failed}
  }

  override def getLastDataLabel(n: Int): Traversable[DataLabel] = {
    obsInfoMap.toList.sortWith({
      (a: (DataLabel, ObservationState), b: (DataLabel, ObservationState)) => a._2.timestamp.compareTo(b._2.timestamp) > 0
    }).take(n) map {
      a: (DataLabel, ObservationState) => a._1
    }
  }

  override def getTimes(label: DataLabel): Traversable[(AnyRef, Option[Duration])] = {
    obsInfoMap.getOrElse(label, new ObservationState).times
  }

  override def getTimestamp(label: DataLabel): Option[LocalDateTime] = obsInfoMap.get(label) map { o => LocalDateTime.ofInstant(o.timestamp.toInstant, ZoneId.systemDefault()) }

  override def getMissingKeywords(label: DataLabel): Traversable[FitsKeyword] = {
    obsInfoMap.getOrElse(label, new ObservationState).missingKeywords
  }

  override def getKeywordsInError(label: DataLabel): Traversable[(FitsKeyword, CollectionError.CollectionError)] = {
    obsInfoMap.getOrElse(label, new ObservationState).errorKeywords
  }

  override def getObservationsInProgress: Traversable[DataLabel] = {
    obsInfoMap.filter {
      case (_, value) => value.started && !value.ended
    }.keySet
  }

  override def getLastDataLabel: Option[DataLabel] = {
    getLastDataLabel(1).headOption
  }
}