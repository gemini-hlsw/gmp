package edu.gemini.epics.impl;

import com.cosylab.epics.caj.CAJChannel;
import com.cosylab.epics.caj.CAJContext;
import com.google.common.collect.ImmutableList;
import edu.gemini.epics.api.EpicsClient;
import edu.gemini.epics.EpicsClientMock;
import edu.gemini.epics.JCAContextController;
import gov.aps.jca.CAException;
import gov.aps.jca.CAStatus;
import gov.aps.jca.Channel;
import gov.aps.jca.Monitor;
import gov.aps.jca.dbr.DBR_TIME_Double;
import gov.aps.jca.event.ConnectionEvent;
import gov.aps.jca.event.ConnectionListener;
import gov.aps.jca.event.GetEvent;
import gov.aps.jca.event.GetListener;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class EpicsObserverImplTest {
    private static final ImmutableList<String> CHANNELS_TO_READ = ImmutableList.of("tst:tst2");
    private EpicsObserverImpl epicsObserver;
    private JCAContextController contextController;
    private CAJContext jcaContext;
    private CAJChannel channel;

    @Before
    public void setUp() throws Exception {
        // Mock controller and JCA
        mockJcaAndController();

        epicsObserver = new EpicsObserverImpl(contextController);
    }

    private void mockJcaAndController() throws CAException {
        contextController = mock(JCAContextController.class);
        when(contextController.isContextAvailable()).thenReturn(true);

        jcaContext = mock(CAJContext.class);
        when(contextController.getJCAContext()).thenReturn(jcaContext);

        channel = mock(CAJChannel.class);
        when(jcaContext.createChannel(anyString(), any(ConnectionListener.class))).thenReturn(channel);
        when(channel.getContext()).thenReturn(jcaContext);
    }

    @Test
    public void testClientRegistration() throws CAException {
        EpicsClientMock epicsClient = new EpicsClientMock();

        epicsObserver.registerEpicsClient(epicsClient, CHANNELS_TO_READ);

        verifyChannelsAreRegisteredToClient(epicsClient);
    }

    private void verifyChannelsAreRegisteredToClient(EpicsClientMock epicsClient) throws CAException {
        assertTrue(epicsClient.wasConnectedCalled());
        verify(jcaContext).createChannel(eq("tst:tst2"), any(ConnectionListener.class));
    }

    @Test
    public void testClientGetsUpdates() throws CAException {
        EpicsClientMock epicsClient = new EpicsClientMock();
        epicsObserver.registerEpicsClient(epicsClient, CHANNELS_TO_READ);

        // We need to capture the GetListener to update the channel internally
        GetListener getListener = captureGetListener();
        GetEvent getEvent = new GetEvent(channel, new DBR_TIME_Double(new double[]{1., 2.}), CAStatus.NORMAL);
        getListener.getCompleted(getEvent);

        assertEquals(1, epicsClient.getUpdatesCount());
        epicsObserver.unregisterEpicsClient(epicsClient);
    }

    private GetListener captureGetListener() throws CAException {
        ArgumentCaptor<ConnectionListener> connectionListenerCaptor = ArgumentCaptor.forClass(ConnectionListener.class);
        ArgumentCaptor<MonitorListener> monitorListenerCaptor = ArgumentCaptor.forClass(MonitorListener.class);
        ArgumentCaptor<GetListener> getListenerCaptor = ArgumentCaptor.forClass(GetListener.class);

        // Simulate connection established
        verify(jcaContext).createChannel(eq("tst:tst2"), connectionListenerCaptor.capture());
        ConnectionEvent connectionEvent = new ConnectionEvent(channel, true);
        connectionListenerCaptor.getValue().connectionChanged(connectionEvent);

        verify(channel).addMonitor(eq(Monitor.VALUE), monitorListenerCaptor.capture());

        // Simulate monitor event
        MonitorEvent monitorEvent = new MonitorEvent(channel, new DBR_TIME_Double(new double[]{1., 2.}), CAStatus.NORMAL);

        when(channel.getConnectionState()).thenReturn(Channel.CONNECTED);

        monitorListenerCaptor.getValue().monitorChanged(monitorEvent);

        verify(channel).get(getListenerCaptor.capture());
        return getListenerCaptor.getValue();
    }

    @Test
    public void testRegistrationAfterStart() throws CAException {
        when(contextController.isContextAvailable()).thenReturn(false);

        EpicsClientMock epicsClient = new EpicsClientMock();
        epicsObserver.registerEpicsClient(epicsClient, CHANNELS_TO_READ);

        when(contextController.isContextAvailable()).thenReturn(true);
        epicsObserver.startObserver();

        verifyChannelsAreRegisteredToClient(epicsClient);
    }

    @Test
    public void testClientRegistrationWithNoChannels() {
        EpicsClientMock epicsClient = new EpicsClientMock();

        epicsObserver.registerEpicsClient(epicsClient, ImmutableList.<String>of());

        verifyNoInteractionsWithClient(epicsClient);
    }

    private void verifyNoInteractionsWithClient(EpicsClientMock epicsClient) {
        assertFalse(epicsClient.wasConnectedCalled());
        verifyNoInteractions(jcaContext);
    }

    @Test
    public void testClientRegistrationWithNullChannels() {
        EpicsClientMock epicsClient = new EpicsClientMock();

        epicsObserver.registerEpicsClient(epicsClient, null);

        verifyNoInteractionsWithClient(epicsClient);
    }

    @Test
    public void testClientRegistrationBeforeJCAIsReady() throws CAException {
        // Override mock
        when(contextController.isContextAvailable()).thenReturn(false);

        EpicsClientMock epicsClient = new EpicsClientMock();
        epicsObserver.registerEpicsClient(epicsClient, CHANNELS_TO_READ);

        verifyNoInteractionsWithClient(epicsClient);

        // Now start the service
        when(contextController.isContextAvailable()).thenReturn(true);
        when(contextController.getJCAContext()).thenReturn(jcaContext);

        epicsObserver.startObserver();

        verifyChannelsAreRegisteredToClient(epicsClient);
    }

    @Test
    public void testUnregisterEpicsClient() {
        EpicsClientMock epicsClient = new EpicsClientMock();
        epicsObserver.registerEpicsClient(epicsClient, CHANNELS_TO_READ);

        epicsObserver.unregisterEpicsClient(epicsClient);
        assertTrue(epicsClient.wasDisconnectedCalled());
    }

    @Test
    public void testAutomaticDisconnect() {
        EpicsClientMock epicsClient = new EpicsClientMock();
        epicsObserver.registerEpicsClient(epicsClient, CHANNELS_TO_READ);

        // Stopping the service should stop the clients
        epicsObserver.stopObserver();

        assertTrue(epicsClient.wasDisconnectedCalled());
    }

    @Test
    public void testUnregisterUnknownClient() {
        EpicsClient epicsClient = mock(EpicsClient.class);
        epicsObserver.unregisterEpicsClient(epicsClient);

        verifyNoInteractions(epicsClient);
    }

}
