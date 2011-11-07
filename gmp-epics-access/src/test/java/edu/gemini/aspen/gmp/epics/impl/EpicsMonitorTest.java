package edu.gemini.aspen.gmp.epics.impl;

import com.google.common.collect.ImmutableList;
import edu.gemini.aspen.gmp.epics.EpicsRegistrar;
import edu.gemini.aspen.gmp.epics.EpicsUpdateImpl;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class EpicsMonitorTest {
    private EpicsRegistrar registrar = mock(EpicsRegistrar.class);

    @Test
    public void testConnected() {
        EpicsMonitor epicsMonitor = new EpicsMonitor(registrar, null, null);
        assertFalse(epicsMonitor.isConnected());
        epicsMonitor.connected();
        assertTrue(epicsMonitor.isConnected());
    }

    @Test
    public void testDisconnected() {
        EpicsMonitor epicsMonitor = new EpicsMonitor(registrar, null, null);
        assertFalse(epicsMonitor.isConnected());
        epicsMonitor.connected();
        epicsMonitor.disconnected();
        assertFalse(epicsMonitor.isConnected());
    }

    @Test
    public void testChannelChanged() {
        EpicsMonitor epicsMonitor = new EpicsMonitor(registrar, null, null);

        epicsMonitor.valueChanged("X.val1", ImmutableList.of(1));
        verify(registrar).processEpicsUpdate(new EpicsUpdateImpl<Integer>("X.val1", ImmutableList.of(1)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructionWithNullRegistrar() {
        new EpicsMonitor(null, null, null);
    }
}
