package edu.gemini.aspen.gds.api

import org.junit.Test
import org.junit.Assert._
import edu.gemini.aspen.gds.api.Conversions._
import edu.gemini.aspen.giapi.data.DataLabel
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._

class PostProcessingPolicyTest {
  val dataLabel = new DataLabel("some key")

  @Test
  def testDefault():Unit = {
    val ep = new DefaultPostProcessingPolicy()
    val collectedValues = CollectedValue[Double]("KEY1", 1.0, "comment", 0, None) :: Nil

    assertEquals(collectedValues, ep.applyPolicy(dataLabel, collectedValues))
  }

  @Test
  def testComposite():Unit = {
    val ep = new CompositePostProcessingPolicyImpl()
    val collectedValues = CollectedValue[Double]("KEY1", 1.0, "comment", 0, None) :: ErrorCollectedValue("KEY2", CollectionError.GenericError, "comment", 0) :: Nil

    assertEquals(collectedValues, ep.applyPolicy(dataLabel, collectedValues))
  }

  @Test
  def testCompositeDefault():Unit = {
    val ep = new CompositePostProcessingPolicyImpl()
    val collectedValues = CollectedValue[Double]("KEY1", 1.0, "comment", 0, None) :: ErrorCollectedValue("KEY2", CollectionError.GenericError, "comment", 0) :: Nil

    ep.addPolicy(new DefaultPostProcessingPolicy)
    assertEquals(collectedValues, ep.applyPolicy(dataLabel, collectedValues))
  }

  @Test
  def testCompositeDefaultTwice():Unit = {
    val ep = new CompositePostProcessingPolicyImpl()
    val collectedValues = CollectedValue[Double]("KEY1", 1.0, "comment", 0, None) :: ErrorCollectedValue("KEY2", CollectionError.GenericError, "comment", 0) :: Nil

    ep.addPolicy(new DefaultPostProcessingPolicy)
    ep.addPolicy(new DefaultPostProcessingPolicy)
    assertEquals(collectedValues, ep.applyPolicy(dataLabel, collectedValues))
  }

  @Test
  def testOrder():Unit = {
    val errPol1 = mock(classOf[PostProcessingPolicy])
    when(errPol1.priority).thenReturn(7)

    val errPol2 = mock(classOf[PostProcessingPolicy])
    when(errPol2.priority).thenReturn(6)

    val ep = new CompositePostProcessingPolicyImpl()
    ep.addPolicy(errPol1)
    ep.addPolicy(errPol2)
    ep.applyPolicy(dataLabel, Nil)

    val order = inOrder(errPol1, errPol2)
    order.verify(errPol2).applyPolicy(any(), any())
    order.verify(errPol1).applyPolicy(any(), any())
    order.verifyNoMoreInteractions()
  }

}