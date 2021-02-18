package edu.gemini.aspen.gds.datalabelprovider

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Test
import edu.gemini.aspen.giapi.data.DataLabel


class DataLabelProviderTest extends AssertionsForJUnit{

  @Test
  def testBasic():Unit = {
    val dlp:DataLabelProvider = new DataLabelProviderImpl
    assert(dlp.getDataLabel() == new DataLabel("S20110505B0001"))
  }
}