package edu.gemini.aspen.gds.staticheaderreceiver

import java.util.concurrent.TimeUnit._

import com.google.common.cache.CacheBuilder
import edu.gemini.aspen.gds.api.fits.FitsKeyword
import edu.gemini.aspen.gds.staticheaderreceiver.TemporarySeqexecKeywordsDatabaseImpl._
import edu.gemini.aspen.giapi.data.DataLabel

import scala.actors.Actor
import scala.jdk.CollectionConverters._
import scala.collection.concurrent._

/**
 * Companion object used to logically group message classes.
 */
object TemporarySeqexecKeywordsDatabaseImpl {

  // store a keyword/value pair for a given data label
  case class Store(dataLabel: DataLabel, keyword: FitsKeyword, value: AnyRef)

  // retrieve the value associated to a keyword and data label
  case class Retrieve(dataLabel: DataLabel, keyword: FitsKeyword)

  // retrieve the value associated to a data label
  case class RetrieveAll(dataLabel: DataLabel)

  // delete data for a given data label
  case class Clean(dataLabel: DataLabel)

  // delete the whole database
  case class CleanAll()

}

trait TemporarySeqexecKeywordsDatabase extends Actor

/**
 * This component stores keyword values coming from the seqexec, so that later an actor will pick them up and
 * complete the information with data from the config file.
 */
class TemporarySeqexecKeywordsDatabaseImpl extends TemporarySeqexecKeywordsDatabase {
  type ValuesCollection = collection.mutable.Map[FitsKeyword, AnyRef]
  // expiration of 1 day by default but tests can override it
  def expirationMillis: Int = 24 * 60 * 60 * 1000

  start()

  def act():Unit = {
    loop {
      react {
        case Store(dataLabel, key, value) => store(dataLabel, key, value)
        case Retrieve(dataLabel, key) => reply(retrieveValue(dataLabel, key))
        case RetrieveAll(dataLabel) => reply(retrieveAll(dataLabel))
        case Clean(dataLabel) => clean(dataLabel)
        case CleanAll() => cleanAll()
        case _ => sys.error("Argument not known ")
      }
    }
  }

  private val map: Map[DataLabel, ValuesCollection] = CacheBuilder.newBuilder()
    .expireAfterWrite(expirationMillis, MILLISECONDS)
    .build[DataLabel, ValuesCollection]().asMap()

  private def cleanAll():Unit = {
    map.clear()
  }

  private def clean(dataLabel: DataLabel):Unit = {
    map -= dataLabel
  }

  private def retrieveValue(dataLabel: DataLabel, keyword: FitsKeyword): Option[AnyRef] = {
    map.get(dataLabel) flatMap {
      x => x.get(keyword)
    }
  }

  private def retrieveAll(dataLabel: DataLabel): collection.immutable.Map[FitsKeyword, AnyRef] = {
    map.get(dataLabel) map {
      _.toMap[FitsKeyword, AnyRef]
    } getOrElse collection.immutable.Map.empty[FitsKeyword, AnyRef]
  }


  private def store(dataLabel: DataLabel, keyword: FitsKeyword, value: AnyRef):Unit = {
    if (!map.contains(dataLabel)) {
      map += (dataLabel -> collection.mutable.Map.empty[FitsKeyword, AnyRef])
    }
    map(dataLabel) += (keyword -> value)
  }
}
