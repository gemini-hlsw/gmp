package edu.gemini.epics.impl;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import com.google.common.collect.ImmutableMap;
import edu.gemini.epics.api.EpicsClient;
import edu.gemini.epics.EpicsClientMock;
import edu.gemini.epics.EpicsObserver;
import edu.gemini.epics.EpicsService;
import gov.aps.jca.event.ConnectionListener;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EpicsClientSubscriber}
 */
public class EpicsClientSubscriberTest {
    private static final String[] CHANNELS_TO_READ = new String[]{"tst:tst"};
    private Map<String, Object> serviceProperties = ImmutableMap.<String, Object>of(EpicsClient.EPICS_CHANNELS, CHANNELS_TO_READ);
    private CAJContext jcaContext;
    private EpicsService epicsService;
    private EpicsObserver epicsObserver;
    private EpicsClientSubscriber subscriber;

    @Before
    public void setUp() throws Exception {
        jcaContext = mock(CAJContext.class);

        CAJChannel channel = mock(CAJChannel.class);
        when(jcaContext.createChannel(anyString(), any(ConnectionListener.class))).thenReturn(channel);

        epicsService = new EpicsService(jcaContext);
        epicsObserver = new EpicsObserverImpl(epicsService);
        subscriber = new EpicsClientSubscriber(epicsObserver);
    }

    @Test
    public void testServiceRegistration() {
        EpicsClientMock epicsClient = new EpicsClientMock();

        subscriber.bindEpicsClient(epicsClient, serviceProperties);

        assertTrue(epicsClient.wasConnectedCalled());
    }

    @Test
    public void testServiceRegistrationWithNoProperties() {
        EpicsClientMock epicsClient = new EpicsClientMock();

        subscriber.bindEpicsClient(epicsClient, ImmutableMap.<String, Object>of());

        assertFalse(epicsClient.wasConnectedCalled());
    }

    @Test
    public void testServiceRegistrationAndUnRegistration() {
        EpicsClientMock epicsClient = new EpicsClientMock();

        subscriber.bindEpicsClient(epicsClient, serviceProperties);
        subscriber.unbindEpicsClient(epicsClient);

        assertTrue(epicsClient.wasDisconnectedCalled());
    }
}
