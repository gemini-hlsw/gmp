package edu.gemini.aspen.gds.fits

import com.google.common.hash.Hashing
import java.net.URL
import java.io.{FileOutputStream, File}
import com.google.common.io.{Closer, Closeables, ByteStreams, Files}

trait FitsSamplesDownloader {
  // Check if file is available and md5 hashes matches
  def downloadFile(fileName: String, fileHash: String):Unit = {
    val sampleFile = new File(fileName)
    val hashingFunction = Hashing.md5()
    val available = sampleFile.exists() && hashingFunction.hashBytes(Files.toByteArray(sampleFile)).toString == fileHash

    if (!available) {
      println("Downloading sample to " + sampleFile)
      val closer = Closer.create()
      try {
        val fitsInputStream = closer.register(new URL("http://sbfgpidev1/gpi/samples/" + sampleFile).openStream())
        val output = closer.register(new FileOutputStream(sampleFile))
        ByteStreams.copy(fitsInputStream, output)
      } catch {
        case e:Throwable => throw closer.rethrow(e)
      } finally {
        closer.close()
      }
    }
  }

}