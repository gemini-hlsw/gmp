package edu.gemini.gmp.status.translator

import edu.gemini.aspen.giapi.status.Health
import edu.gemini.gmp.status.translator.generated.DataType
import org.junit.Test
import org.junit.Assert.assertEquals

/**
 * Class StatusItemTranslatorConfigurationTest
 *
 * @author Nicolas A. Barriga
 *         Date: 4/9/12
 */
class StatusItemTranslatorConfigurationTest {
  @Test def testSimpleConfiguration():Unit = {
    val configuration: StatusItemTranslatorConfiguration = new StatusItemTranslatorConfiguration(getClass.getResourceAsStream("status-translator.xml"))
    val statuses = configuration.statuses
    assertEquals(7, statuses.size)
    assertEquals("old", statuses(0).getOriginalName)
    assertEquals("new", statuses(0).getTranslatedName)
    assertEquals(DataType.INT, statuses(0).getOriginalType)
    assertEquals(DataType.HEALTH, statuses(0).getTranslatedType)
    for (i <- 0 to 2) {
      assertEquals(Integer.toString(i), statuses(0).getMaps.getMap.get(i).getFrom)
      assertEquals(Health.values.apply(i), Health.valueOf(statuses(0).getMaps.getMap.get(i).getTo))
    }
    assertEquals("BAD", statuses(0).getDefault)
    assertEquals(null, statuses(1).getDefault)
  }
}