package edu.gemini.aspen.gmp.status.simulator;

import edu.gemini.aspen.gmp.status.simulator.generated.StatusType;
import org.junit.Test;

import jakarta.xml.bind.JAXBException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SimulatorConfigurationTest {
    @Test
    public void testSimpleConfiguration() throws JAXBException {
        SimulatorConfiguration configuration = new SimulatorConfiguration(getClass().getResourceAsStream("status-simulator.xml"));
        List<StatusType> statuses = configuration.getStatuses();
        assertEquals(3, statuses.size());
    }

    @Test
    public void testRandomChannel() throws JAXBException {
        SimulatorConfiguration configuration = new SimulatorConfiguration(getClass().getResourceAsStream("status-simulator.xml"));
        List<StatusType> statuses = configuration.getStatuses();
        boolean verified = false;
        for (StatusType s:statuses) {
            if (s.getName().equals("gpi:ao.strehl")) {
                assertEquals("random", s.getMode());
                assertEquals(0, s.getParameters().getMin().intValue());
                assertEquals(10.0, s.getParameters().getMax().doubleValue(), 0);
                verified = true;
            }
        }
        assertTrue("Must have found the tested status", verified);
    }

    @Test
    public void testAsymptoticChannel() throws JAXBException {
        SimulatorConfiguration configuration = new SimulatorConfiguration(getClass().getResourceAsStream("status-simulator.xml"));
        List<StatusType> statuses = configuration.getStatuses();
        boolean verified = false;
        for (StatusType s:statuses) {
            if (s.getName().equals("gpi:ao.r0")) {
                assertEquals("asymptotic-with-noise", s.getMode());
                assertEquals(0, s.getParameters().getMin().intValue());
                assertEquals(10.0, s.getParameters().getMax().doubleValue(), 0);
                assertEquals(100, s.getParameters().getPeriod().intValue());
                assertEquals("false", s.getParameters().getRepeat());
                verified = true;
            }
        }
        assertTrue("Must have found the tested status", verified);
    }

    @Test
    public void testEnumeratedChannel() throws JAXBException {
        SimulatorConfiguration configuration = new SimulatorConfiguration(getClass().getResourceAsStream("status-simulator.xml"));
        List<StatusType> statuses = configuration.getStatuses();
        boolean verified = false;
        for (StatusType s:statuses) {
            if (s.getName().equals("gpi:aoWfsFilter")) {
                assertEquals("enumeration", s.getMode());
                assertEquals("Y", s.getEnumeration().getValue().get(0));
                verified = true;
            }
        }
        assertTrue("Must have found the tested status", verified);
    }
}