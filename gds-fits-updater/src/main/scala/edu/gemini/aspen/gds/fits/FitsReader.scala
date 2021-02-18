package edu.gemini.aspen.gds.fits

import java.io.File
import scala.jdk.CollectionConverters._
import java.util.logging.Logger
import nom.tam.fits.{HeaderCard, Fits}
import edu.gemini.aspen.gds.api.fits.{HeaderItem, FitsKeyword, Header}
import com.google.common.hash.Hashing

class FitsReader(file: File) {
  protected val LOG: Logger = Logger.getLogger(this.getClass.getName)
  require(file.exists)
  require(file.isFile, {
    LOG.severe("File " + file + " is not a file")
    "File " + file + " is not a file"
  })
  require(file.canRead, {
    LOG.severe("Cannot read file " + file)
    "Cannot read file " + file
  })
  LOG.info("Reading FITS file " + file)
  val fitsFile = new Fits(file)

  /**
   * Returns the header with the given index if available
   *
   * @param index The header index, must be 0 or higher */
  def header(index: Int = 0): Option[Header] = Option(fitsFile.getHDU(index)) map {
    hdu =>
      val headerItems = hdu.getHeader.iterator().collect {
        case k: HeaderCard if k.isKeyValuePair => k
      }.map {
        k => HeaderItem(FitsKeyword(k.getKey), k.getValue, k.getComment, None)
      }
      Header(index, headerItems.toSeq, hdu.getFileOffset, hdu.getHeader.getSize, hdu.getHeader.getDataSize)
  }

  /**
   * Returns the checksum of the data section of a file
   * @param index
   */
  def checksumData(index: Int): String = Option(fitsFile.getHDU(index)).map { hdu =>
      val hasher = Hashing.md5().newHasher()
      hdu.getKernel match {
        case l: Array[Array[Float]] =>
          l.map {
            r => r.map(hasher.putFloat)
          }
          hasher.hash().toString
        case _: Array[Array[Double]] => sys.error("Unimplemented")
        case _: Array[Array[Int]] => sys.error("Unimplemented")
        case _: Array[Array[Short]] => sys.error("Unimplemented")
        case _: Array[Array[Boolean]] => sys.error("Unimplemented")
        case l: Array[AnyRef] =>
          l.foreach {
            // This is a bit unreliable so we hash a counter
            _ => hasher.putInt(1)
          }
          hasher.hash().toString
        case x => sys.error("Unknown data type to hash " + x)
      }
  }.getOrElse("")

  /**
   * Returns a collection of keys contained in a header
   *
   * @param index Header index
   * @return a set of contained keywords or empty if the header is not available */
  def keys(index: Int): Traversable[FitsKeyword] = header(index).map {
    h => h.keywords.map {
      _.keywordName
    }
  }.getOrElse(Traversable.empty[FitsKeyword])

}