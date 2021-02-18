package edu.gemini.aspen.gds.fits

import edu.gemini.aspen.gds.api.fits.Header

import scala.jdk.CollectionConverters._
import edu.gemini.aspen.gds.api.Conversions._
import com.google.common.base.Stopwatch
import nom.tam.util.BufferedFile
import java.io.File
import java.util.Locale

import annotation.tailrec
import nom.tam.fits.{BasicHDU, HeaderCard}
import com.google.common.io.Files
import java.util.logging.Level
import java.util.concurrent.TimeUnit

/**
 * Wrapper class that can write fits files
 *
 * @param file Name of the file
 */
class FitsWriter(file: File) extends FitsReader(file) {
  traverseHeaders(0)

  private def addNewKeywords(hdu: BasicHDU, header: Header):Unit = {
    val hduHeader = hdu.getHeader
    val existingKeywords = hduHeader.iterator().collect {
      case k: HeaderCard if k.isKeyValuePair => k
    }

    // Find keys in header but not in the file
    val newKeywords = header.keywords.collect {
      // Select keys
      case k if !(existingKeywords exists {
        _.getKey == k.keywordName.key
      }) => k
    }
    LOG.info("Updating FITS file %s, header id:%d, with %d new keywords".format(file, header.index, newKeywords.size))
    newKeywords foreach {
      k => if (k.format.isDefined) {
        k.value match {
          case s: String => try {
            val card = new HeaderCard(k.keywordName, s, k.comment)
            try {
              val value = String.format(k.format.get, s)
              validateValue(value)
              card.setValue(value)
            } catch {
              case ex: Exception =>
                LOG.log(Level.WARNING, "Couldn't properly format value '" + s + "' with formatter '" + k.format.get + "'", ex)
            }
            hduHeader.addLine(card)
          }
          case i: Int =>
            val card = new HeaderCard(k.keywordName, i, k.comment)
            try {
              val value = String.format(k.format.get, i.underlying())
              validateValue(value)
              card.setValue(value)
            } catch {
              case ex: Exception =>
                LOG.log(Level.WARNING, "Couldn't properly format value '" + i + "' with formatter '" + k.format.get + "'", ex)
            }
            hduHeader.addLine(card)
          case d: Double if d.isInfinite =>
            val card = new HeaderCard(k.keywordName, "", k.comment)
            val value = "INF"
            validateValue(value)
            card.setValue(value)
            hduHeader.addLine(card)
          case d: Double if d.isNaN =>
            val card = new HeaderCard(k.keywordName, "", k.comment)
            val value = "NAN"
            validateValue(value)
            card.setValue(value)
            hduHeader.addLine(card)
          case d: Double =>
            val card = new HeaderCard(k.keywordName, d, k.comment)
            try {
              val value = String.format(Locale.US, k.format.get, d.underlying())
              validateValue(value)
              card.setValue(value)
            } catch {
              case ex: Exception =>
                LOG.log(Level.WARNING, "Couldn't properly format value '" + d + "' with formatter '" + k.format.get + "'", ex)
            }
            hduHeader.addLine(card)
          case b: Boolean =>
            val card = new HeaderCard(k.keywordName, b, k.comment)
            try {
              val value = String.format(k.format.get, b: java.lang.Boolean)
              validateValue(value)
              card.setValue(value)
            }
            catch {
              case ex: Exception =>
                LOG.log(Level.WARNING, "Couldn't properly format value '" + b + "' with formatter '" + k.format.get + "'", ex)
            }
            hduHeader.addLine(card)
          case _ => LOG.warning("Ignored key of unknown type " + k)
        }
      } else {
        k.value match {
          case s: String => hduHeader.addValue(k.keywordName, s, k.comment)
          case i: Int => hduHeader.addValue(k.keywordName, i, k.comment)
          case d: Double => hduHeader.addValue(k.keywordName, d, k.comment)
          case b: Boolean => hduHeader.addValue(k.keywordName, b, k.comment)
          case _ => LOG.warning("Ignored key of unknown type " + k)
        }
      }
    }
    // This call is needed to force fits file to read until the end of the
    // data section or the write may fail
    Option(hdu.getData) map {
      _.getData
    }
  }

  private def validateValue(value: String):Unit = {
    if (value.length() > HeaderCard.MAX_VALUE_LENGTH) {
      throw new IllegalArgumentException()
    } else if (value.startsWith("'") && !value.endsWith("'")) {
      throw new IllegalArgumentException()
    }
  }

  private def commitChanges(destinationFile: File):Unit = {
    val stopwatch = Stopwatch.createStarted()

    Files.createParentDirs(destinationFile)
    val finalFile = new BufferedFile(destinationFile, "rw")
    fitsFile.write(finalFile)

    LOG.info("File %s, written in %d [ms]".format(destinationFile, stopwatch.stop().elapsed(TimeUnit.MILLISECONDS)))
  }

  /**
   * Updates the FITS file with new keywords add to the header
   * In case the header does not exist the operation is ignored
   * Existing keywords are not updated */
  def updateHeader(header: Header, destinationFile: File):Unit = {
    Option(fitsFile.getHDU(header.index)) foreach {
      hdu =>
        addNewKeywords(hdu, header)
        commitChanges(destinationFile)
    }
  }

  /**
   * Updates the FITS file with new keywords added to several headers at a time
   * In case the header does not exist the operation is ignored
   * Existing keywords are not updated */
  def updateHeaders(headers: Traversable[Header], destinationFile: File):Unit = {
    headers foreach {
      h => if (fitsFile.getHDU(h.index) != null) addNewKeywords(fitsFile.getHDU(h.index), h)
    }
    commitChanges(destinationFile)
  }

  @tailrec
  private def traverseHeaders(index: Int):Unit = {
    if (fitsFile.getHDU(index) != null) {
      traverseHeaders(index + 1)
    }
  }

}