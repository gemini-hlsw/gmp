package edu.gemini.aspen.gds.obsevent.handler

import scala.jdk.CollectionConverters._
import scala.collection.concurrent._
import com.google.common.cache.CacheBuilder
import edu.gemini.aspen.giapi.data.DataLabel
import java.util.concurrent.TimeUnit

/**
 * Simple class to store in a self-cleaning cache the observation transactions */
class ObservationTransactionsStore {
  // expiration of 1 day by default but tests can override it
  def expirationMillis = 24 * 60 * 60 * 1000

  val observationTransactions: Map[DataLabel, String] = CacheBuilder.newBuilder()
    .expireAfterWrite(expirationMillis, TimeUnit.MILLISECONDS)
    .build[DataLabel, String]().asMap()

  def startTransaction(dataLabel: DataLabel):Unit = {
    observationTransactions.put(dataLabel, "")
  }

  def hasTransaction(dataLabel: DataLabel): Boolean = observationTransactions.contains(dataLabel)
}