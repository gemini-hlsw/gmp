package edu.gemini.aspen.gds.observationstate.impl

import org.junit.Assert._
import org.mockito.Mockito._
import org.junit.Test
import edu.gemini.aspen.gds.api.configuration.GDSConfigurationService
import edu.gemini.aspen.gds.observationstate.ObservationStatePublisher
import edu.gemini.aspen.gds.api.Conversions._
import edu.gemini.aspen.gds.api._
import fits.FitsKeyword

class InspectPolicyTest {
    @Test
    def testMissing():Unit = {
        val config = mock(classOf[GDSConfigurationService])
        when(config.getConfiguration).thenReturn(GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS", 0, "DOUBLE", true, "NONE", "EPICS", "gpi:value", 0, "", "Mean airmass for the observation") :: GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS2", 0, "DOUBLE", true, "NONE", "EPICS", "gpi:value", 0, "", "Mean airmass for the observation") :: Nil)
        val obsState = new ObservationStateImpl(mock(classOf[ObservationStatePublisher]))
        val policy: PostProcessingPolicy = new InspectPolicy(config, obsState)
        policy.applyPolicy("label", CollectedValue("AIRMASS", "strValue", "comment", 0, None) :: Nil)

        assertEquals(Set(new FitsKeyword("AIRMASS2")), obsState.getMissingKeywords("label"))
    }

    @Test
    def testError():Unit = {
        val config = mock(classOf[GDSConfigurationService])
        when(config.getConfiguration).thenReturn(GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS", 0, "DOUBLE", true, "NONE", "EPICS", "gpi:value", 0, "", "Mean airmass for the observation") :: GDSConfiguration("GPI", "OBS_START_ACQ", "AIRMASS2", 0, "DOUBLE", true, "NONE", "EPICS", "gpi:value", 0, "", "Mean airmass for the observation") :: Nil)
        val obsState = new ObservationStateImpl(mock(classOf[ObservationStatePublisher]))
        val policy: PostProcessingPolicy = new InspectPolicy(config, obsState)
        policy.applyPolicy("label", CollectedValue("AIRMASS", "strValue", "comment", 0, None) :: ErrorCollectedValue("AIRMASS2", CollectionError.GenericError, "comment", 0) :: Nil)

        assertEquals(Set((new FitsKeyword("AIRMASS2"), CollectionError.GenericError)), obsState.getKeywordsInError("label"))
    }
}