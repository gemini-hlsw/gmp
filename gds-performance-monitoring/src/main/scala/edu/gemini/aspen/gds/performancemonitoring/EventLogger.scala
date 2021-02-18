package edu.gemini.aspen.gds.performancemonitoring

import java.time.{Duration, LocalDateTime}

import scala.collection.mutable

//todo: Add javadoc to this class
/**
 * This event logger works by logging "event"'s starting and finishing times. The event are grouped in "sets".
 * For example, you can create a set for a datalabel, and log the starting and ending times of every observation event
 * received for said datalabel.
 *
 * You can then retrieve or log the data for a set, or average a certain event over all sets.
 */
class EventLogger[A, B] {
  //map: eventSet -> (event -> (startTime, endTime))
  private val map = new mutable.HashMap[A, collection.mutable.Map[B, (Option[LocalDateTime], Option[LocalDateTime])]] with mutable.SynchronizedMap[A, collection.mutable.Map[B, (Option[LocalDateTime], Option[LocalDateTime])]]

  def addEventSet(set: A):Unit = {
    map += set -> new mutable.HashMap[B, (Option[LocalDateTime], Option[LocalDateTime])] with mutable.SynchronizedMap[B, (Option[LocalDateTime], Option[LocalDateTime])]
  }

  def start(set: A, evt: B):Unit = {
    map.getOrElseUpdate(set, new mutable.HashMap[B, (Option[LocalDateTime], Option[LocalDateTime])] with mutable.SynchronizedMap[B, (Option[LocalDateTime], Option[LocalDateTime])]) += evt ->(Some(LocalDateTime.now), map(set).getOrElse(evt, (None, None))._2)
  }

  def end(set: A, evt: B):Unit = {
    map.getOrElseUpdate(set, new mutable.HashMap[B, (Option[LocalDateTime], Option[LocalDateTime])] with mutable.SynchronizedMap[B, (Option[LocalDateTime], Option[LocalDateTime])]) += evt ->(map(set).getOrElse(evt, (None, None))._1, Some(LocalDateTime.now))
  }

  def retrieve(set: A): scala.collection.Map[B, Option[Duration]] =
    map.getOrElse(set, collection.mutable.Map.empty[B, (Option[LocalDateTime], Option[LocalDateTime])])
      .mapValues({
      case (Some(start), Some(end)) => Some(Duration.between(start, end))
      case _ => None
    })

  def retrieve(set: A, evt: B): Option[Duration] =
    map.getOrElse(set, collection.mutable.Map.empty[B, (Option[LocalDateTime], Option[LocalDateTime])]).get(evt) flatMap {
      case (Some(start), Some(end)) => Some(Duration.between(start, end))
      case _ => None
    }

  def average(evt: B): Option[Duration] = {
    val values = for {
      (_, innerMap) <- map
      (_evt, times) <- innerMap
      if _evt == evt
    } yield times

    val durations = values.collect({
      case (Some(start), Some(end)) => Duration.between(start, end)
    })

    case class Average(sum: Duration, count: Int) {
      def +(other: Duration): Average = {
        Average(sum.plus(other), count + 1)
      }

      def average(): Option[Duration] = {
        count match {
          case 0 => None
          case x => Some(Duration.ofMillis(sum.toMillis / x))
        }
      }
    }
    val avg = Average(Duration.ofSeconds(0), 0)
    durations.foldLeft(avg) {
      (currentAvg, currentVal) => currentAvg + currentVal
    }.average()
  }

  def retrieveAll(): scala.collection.Map[A, scala.collection.Map[B, Option[Duration]]] = {
    map.mapValues({
      m => m.mapValues({
        case (Some(start), Some(end)) => Some(Duration.between(start, end))
        case _ => None
      })
    })
  }

  def check(set: A, evt: B, millis: Long): Boolean = retrieve(set, evt) match {
    case Some(x: Duration) => x.toMillis <= millis
    case _ => false
  }
}
