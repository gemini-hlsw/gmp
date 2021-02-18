package edu.gemini.aspen.gds.fits

import edu.gemini.aspen.giapi.data.DataLabel
import java.io.File
import collection.JavaConversions._
import actors.Reactor
import java.util.logging.{Level, Logger}
import edu.gemini.aspen.gds.api.fits.Header
import com.google.common.io.Files
import scala.annotation.tailrec

case class Update(namingFunction: DataLabel => String = label => FitsUpdater.toFitsFileName(label))

/**
 * Class that can take an existing file and add the headers passed in the constructor
 *
 * The FitsUpdater must preserve all the existing data and only add or update the headers
 * passed at construction
 */
class FitsUpdater(fromDirectory: File, toDirectory: File, dataLabel: DataLabel, headers: List[Header]) extends Reactor[Update] {
  protected val LOG = Logger.getLogger(this.getClass.getName)
  require(fromDirectory.exists, {
    LOG.severe("Directory " + fromDirectory + " doesn't exist")
    "Directory " + fromDirectory + " doesn't exist"
  })
  require(fromDirectory.isDirectory, {
    LOG.severe(fromDirectory + " is not a directory")
    fromDirectory + " is not a directory"
  })
  if (!toDirectory.exists) {
    toDirectory.mkdirs()
  }
  require(toDirectory.exists, {
    LOG.severe("Directory " + toDirectory + " doesn't exist")
    "Directory " + toDirectory + " doesn't exist"
  })
  require(toDirectory.isDirectory, {
    LOG.severe(toDirectory + " is not a directory")
    toDirectory + " is not a directory"
  })
  require(dataLabel != null, {
    LOG.severe("Datalabel is null")
    "Datalabel is null"
  })
  start()

  def act():Unit = {
    loop {
      react {
        case Update(namingFunction) => {
          try {
            updateFitsHeaders(outputNamingFunction = namingFunction)
          } catch {
            case ex:Exception =>
              LOG.log(Level.WARNING, ex.getMessage, ex)
          }
        }
      }
    }
  }

  /**
   * Updates the headers in the destination file, adding to the current set of
   * headers the ones passed in the constructor
   *
   * @param outputNamingFunction, it is a an optional method to key the new file. It is a relative key, not absolute. For example: {label => "N-" + label.getName + ".fits"}
   */
  def updateFitsHeaders(inputNamingFunction: DataLabel => String = FitsUpdater.toFitsFileName, outputNamingFunction: DataLabel => String = FitsUpdater.toFitsFileName): (File, File) = {
    val originalFile = new File(fromDirectory, inputNamingFunction(dataLabel))
    val tempFile = File.createTempFile("tmp", "_fits", toDirectory)
    val destinationFile = new File(toDirectory, outputNamingFunction(dataLabel))
    LOG.info(s"Updating file $originalFile to $destinationFile")

    val updatedHeaders = headers.sortBy {
      _.index
    }

    val writer = new FitsWriter(originalFile)

    writer.updateHeaders(updatedHeaders, tempFile)
    // Make sure the dirs exist

    Files.createParentDirs(destinationFile)
    val newDestinationFile = FitsUpdater.safeDestinationFile(destinationFile)
    if (newDestinationFile.getAbsolutePath != destinationFile.getAbsolutePath) {
      LOG.warning(s"File $destinationFile already exist, using $newDestinationFile instead")
    }
    Files.move(tempFile, newDestinationFile)
    (originalFile, newDestinationFile)
  }

}

object FitsUpdater {
  def toFitsFileName(dataLabel: DataLabel):String = dataLabel.toString

  @tailrec
  final def safeDestinationFile(file: File): File = {
    val NameRegex = """(\w*)-(\d+)""".r
    if (!file.exists()) {
      file
    } else {
      val name = Files.getNameWithoutExtension(file.getAbsolutePath)
      val ext = Files.getFileExtension(file.getAbsolutePath)
      val nextFilename = name match {
        case NameRegex(n, d) => s"$n-${d.toInt + 1}.$ext"
        case _               => s"$name-1.$ext"
      }
      safeDestinationFile(new File(file.getParentFile, nextFilename))
    }
  }

}

