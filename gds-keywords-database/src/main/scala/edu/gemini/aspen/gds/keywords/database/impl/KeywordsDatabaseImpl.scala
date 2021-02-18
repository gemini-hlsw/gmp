package edu.gemini.aspen.gds.keywords.database.impl

import java.util.concurrent.TimeUnit._
import java.util.logging.Logger

import com.google.common.cache.CacheBuilder
import edu.gemini.aspen.gds.api.CollectedValue
import edu.gemini.aspen.gds.keywords.database._
import edu.gemini.aspen.giapi.data.DataLabel

import scala.jdk.CollectionConverters._
import scala.collection.concurrent

/**
 * Component to store CollectedValue as HeaderItem, associated to DataLabel */
class KeywordsDatabaseImpl extends KeywordsDatabase {
  private val LOG = Logger.getLogger(this.getClass.getName)
  // expiration of 1 day by default but tests can override it
  def expirationMillis: Int = 24 * 60 * 60 * 1000

  start()

  def act():Unit = {
    loop {
      react {
        case Store(dataLabel: DataLabel, value: CollectedValue[_])           => _store(dataLabel, List[CollectedValue[_]](value))
        case StoreList(dataLabel: DataLabel, value: List[CollectedValue[_]]) => _store(dataLabel, value)
        case Retrieve(dataLabel)                                             => reply(_retrieve(dataLabel))
        case Clean(dataLabel)                                                => _clean(dataLabel)
        case x                                                               => sys.error("Argument not known " + x)
      }
    }
  }

  def store(dataLabel: DataLabel, value: CollectedValue[_]):Unit = {
    this ! Store(dataLabel, value)
  }

  def storeList[T](dataLabel: DataLabel, value: List[CollectedValue[T]]):Unit = {
    this ! StoreList(dataLabel, value)
  }

  def retrieve(dataLabel: DataLabel): List[CollectedValue[_]] = {
    val ret = this !?(1000, Retrieve(dataLabel))
    ret.asInstanceOf[Option[List[CollectedValue[_]]]] getOrElse Nil
  }

  def clean(dataLabel: DataLabel):Unit = {
    this ! Clean(dataLabel)
  }

  private val map: concurrent.Map[DataLabel, List[CollectedValue[_]]] = CacheBuilder.newBuilder()
    .expireAfterWrite(expirationMillis, MILLISECONDS)
    .build[DataLabel, List[CollectedValue[_]]]().asMap()

  /**
   * Store the keyword
   *
   * @param dataLabel to which the keywords belong
   */
  private def _store(dataLabel: DataLabel, headerItem: List[CollectedValue[_]]) {
    map.put(dataLabel, headerItem ++ map.getOrElse(dataLabel, List[CollectedValue[_]]()))
  }

  /**
   * Retrieve all the data associated to a given data set
   *
   * @param dataLabel for which to retrieve data
   *
   * @return a List[CollectedValue[_]] containing the collected values of the data set
   * or an empty list if none found */
  private def _retrieve(dataLabel: DataLabel): List[CollectedValue[_]] = map.getOrElse(dataLabel, Nil)

  /**
   * Remove all keywords associated with a given DataLabel
   *
   * @param dataLabel for which to remove data */
  private def _clean(dataLabel: DataLabel) {
    map.remove(dataLabel)
  }

}