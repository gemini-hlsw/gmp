package edu.gemini.aspen.gds.keywords.database.impl

import java.util.concurrent.TimeUnit.MILLISECONDS

import com.google.common.cache.CacheBuilder
import edu.gemini.aspen.gds.keywords.database.{ProgramIdDatabase, RetrieveProgramId, StoreProgramId}
import edu.gemini.aspen.giapi.data.DataLabel

import scala.jdk.CollectionConverters._
import scala.collection.concurrent

/**
 * Implementation of ProgramIdDatabase */
class ProgramIdDatabaseImpl extends ProgramIdDatabase {
  // expiration of 1 day by default but tests can override it
  def expirationMillis: Int = 24 * 60 * 60 * 1000

  start()

  def act():Unit = {
    loop {
      react {
        case StoreProgramId(dataLabel, programId) => internalStore(dataLabel, programId)
        case RetrieveProgramId(dataLabel) => sender ! retrieve(dataLabel)
        case x => sys.error("Argument not known: " + x)
      }
    }
  }

  private val map: concurrent.Map[DataLabel, String] = CacheBuilder.newBuilder()
    .expireAfterWrite(expirationMillis, MILLISECONDS)
    .build[DataLabel, String]().asMap()

  override def store(dataLabel: DataLabel, programId: String):Unit = {
    this ! StoreProgramId(dataLabel, programId)
  }

  private def internalStore(dataLabel: DataLabel, programId: String):Unit = {
    map += dataLabel -> programId
  }

  private def retrieve(dataLabel: DataLabel): Option[String] = {
    map.get(dataLabel)
  }

}