package edu.gemini.aspen.giapi.data;

import com.gargoylesoftware.base.testing.EqualsTester;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the Observation Event class.
 */
public class ObservationEventTest {

    @Test
    public void testEquals() {
        new EqualsTester(ObservationEvent.OBS_START_ACQ,
                ObservationEvent.OBS_START_ACQ,
                ObservationEvent.OBS_END_ACQ,
                null);
    }

    @Test
    public void testGetObservationEventName() {
        assertEquals(ObservationEvent.OBS_END_ACQ.getObservationEventName(), "OBS_END_ACQ");
        assertEquals(ObservationEvent.OBS_END_DSET_WRITE.getObservationEventName(), "OBS_END_DSET_WRITE");
        assertEquals(ObservationEvent.OBS_END_READOUT.getObservationEventName(), "OBS_END_READOUT");
        assertEquals(ObservationEvent.OBS_PREP.getObservationEventName(), "OBS_PREP");
        assertEquals(ObservationEvent.OBS_START_ACQ.getObservationEventName(), "OBS_START_ACQ");
        assertEquals(ObservationEvent.OBS_START_DSET_WRITE.getObservationEventName(), "OBS_START_DSET_WRITE");
        assertEquals(ObservationEvent.OBS_START_READOUT.getObservationEventName(), "OBS_START_READOUT");
    }

    @Test
    public void testValidParsing() {
        assertEquals(ObservationEvent.valueOf("OBS_END_ACQ"), ObservationEvent.OBS_END_ACQ);
        assertEquals(ObservationEvent.valueOf("OBS_END_DSET_WRITE"), ObservationEvent.OBS_END_DSET_WRITE);
        assertEquals(ObservationEvent.valueOf("OBS_END_READOUT"), ObservationEvent.OBS_END_READOUT);
        assertEquals(ObservationEvent.valueOf("OBS_PREP"), ObservationEvent.OBS_PREP);
        assertEquals(ObservationEvent.valueOf("OBS_START_ACQ"), ObservationEvent.OBS_START_ACQ);
        assertEquals(ObservationEvent.valueOf("OBS_START_DSET_WRITE"), ObservationEvent.OBS_START_DSET_WRITE);
        assertEquals(ObservationEvent.valueOf("OBS_START_READOUT"), ObservationEvent.OBS_START_READOUT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidParsing() {
        ObservationEvent.valueOf("unexistant event");
    }

    @Test(expected = NullPointerException.class)
    public void testNullParsing() {
        ObservationEvent.valueOf(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyParsing() {
        ObservationEvent.valueOf("");
    }
}
