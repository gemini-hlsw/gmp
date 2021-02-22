package edu.gemini.epics;

import com.cosylab.epics.caj.CAJContext;
import gov.aps.jca.Context;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class EpicsServiceTest {
    private EpicsService epicsService;

    @Before
    public void setUp() {
        epicsService = new EpicsService("127.0.0.1", 1.0, 0);
    }

    @Test
    public void testNormalLifeCycle() {
        epicsService.startService();

        assertNotNull(epicsService.getJCAContext());

        epicsService.stopService();
    }

    @Test
    public void testStartingTwice() {
        epicsService.startService();

        Context context = epicsService.getJCAContext();
        epicsService.startService();

        assertFalse(context.equals(epicsService.getJCAContext()));

        epicsService.stopService();
    }

    @Test
    public void testStopBeforeStart() {
        epicsService.stopService();

        assertFalse(epicsService.isContextAvailable());
    }

    /**
     * Simulates an update from ConfigAdmin
     */
    @Test
    public void updateAddressList() {
        String NEW_ADDRESS = "0.0.0.0";
        epicsService.setAddress(NEW_ADDRESS);

        epicsService.startService();
        assertEquals(NEW_ADDRESS, System.getProperty("com.cosylab.epics.caj.CAJContext.addr_list"));

        // Now pass an empty set
        epicsService.setAddress(null);
        epicsService.startService();
        assertEquals(NEW_ADDRESS, System.getProperty("com.cosylab.epics.caj.CAJContext.addr_list"));
    }

    @Test(expected = IllegalStateException.class)
    public void testGetContextBeforeReady() {
        epicsService.getJCAContext();
    }

    @Test
    public void testGetContextPassedInitially() {
        CAJContext context = mock(CAJContext.class);
        EpicsService epicsService = new EpicsService(context);
        assertEquals(context, epicsService.getJCAContext());
    }

    /**
     * Simulates multiple IP addresses
     */
    @Test
    public void testSupportForMultipleIPs() {
        assertNotNull(new EpicsService("127.0.0.1 0.0.0.0", 1.0, 0));
    }


}
