package edu.gemini.aspen.gds.postprocessingpolicy

import org.junit.Test
import org.junit.Assert._
import com.google.common.io.Files

class DeleteOriginalPolicyTest {
  @Test
  def testNonErrors():Unit = {
    val file1 = Files.createTempDir()
    val deleteOriginal = new DeleteOriginalPolicy()
    deleteOriginal.fileReady(file1, file1)

    assertFalse(file1.exists())
  }
}
