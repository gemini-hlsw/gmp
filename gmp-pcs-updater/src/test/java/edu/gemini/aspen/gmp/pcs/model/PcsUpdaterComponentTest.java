package edu.gemini.aspen.gmp.pcs.model;

import com.google.common.collect.ImmutableMap;
import edu.gemini.aspen.gmp.pcs.model.updaters.EpicsPcsUpdater;
import edu.gemini.cas.ChannelAccessServer;
import edu.gemini.epics.EpicsException;
import edu.gemini.epics.EpicsWriter;
import edu.gemini.epics.api.Channel;
import edu.gemini.epics.api.ReadOnlyChannel;
import gov.aps.jca.CAException;
import org.junit.Test;

import java.util.Hashtable;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class PcsUpdaterComponentTest {
    private EpicsWriter epicsWriter = mock(EpicsWriter.class);
    private ChannelAccessServer channelFactory = mock(ChannelAccessServer.class);
    private String channel = "tst";
    private String gains = "1.0";
    private int taiDiff = 0;

    @Test
    public void registerWriter() throws Exception {
        PcsUpdaterComponent component = buildComponent();

        component.startComponent();
        verifyBindings(channel, 1);
    }

    private PcsUpdaterComponent buildComponent() {
        return new PcsUpdaterComponent(channelFactory, false, channel, gains, taiDiff);
    }

    private void verifyBindings(String baseChannel, int count) throws CAException {
        verify(channelFactory, times(count)).createChannel(eq(baseChannel), eq(EpicsPcsUpdater.buildZeroZernikesArray()));
    }

    @Test
    public void registerWriterInSimulation() throws EpicsException {
        PcsUpdaterComponent component = buildComponentInSimulation();

        component.startComponent();
        verifyNoInteractions(epicsWriter);
    }

    private PcsUpdaterComponent buildComponentInSimulation() {
        return new PcsUpdaterComponent(channelFactory, true, channel, gains, taiDiff);
    }

    @Test
    public void exceptionOnRegistrationCaught() throws EpicsException {
        PcsUpdaterComponent component = buildComponentInSimulation();
        doThrow(new EpicsException("Test exception", new RuntimeException())).when(epicsWriter).getDoubleChannel(anyString());

        component.startComponent();
        verifyNoInteractions(epicsWriter);
    }

    @Test
    public void stopComponent() throws Exception {
        Channel<Double> epicsChannel = mock(Channel.class);
        when(channelFactory.createChannel(anyString(), any(List.class))).thenReturn(epicsChannel);

        PcsUpdaterComponent component = buildComponent();

        component.startComponent();

        component.stopComponent();
        verifyBindings(channel, 1);
        verify(channelFactory).destroyChannel(any(ReadOnlyChannel.class));
    }

    @Test
    public void unregisterWriterInSimulation() throws EpicsException {
        PcsUpdaterComponent component = buildComponentInSimulation();

        component.startComponent();

        component.stopComponent();
        verifyNoInteractions(epicsWriter);
    }

    @Test
    public void transitionToSimulation() throws Exception {
        Channel<Double> epicsChannel = mock(Channel.class);
        when(channelFactory.createChannel(anyString(), any(List.class))).thenReturn(epicsChannel);

        PcsUpdaterComponent component = buildComponent();

        component.startComponent();

        component.updatedComponent(new Hashtable(ImmutableMap.of("simulation", "true", "epicsChannel", channel)));

        verifyBindings(channel, 1);
        verify(channelFactory).destroyChannel(any(ReadOnlyChannel.class));
    }

    @Test
    public void transitionToNonSimulation() throws Exception {
        Channel<Double> epicsChannel = mock(Channel.class);
        when(channelFactory.createChannel(anyString(), any(List.class))).thenReturn(epicsChannel);

        PcsUpdaterComponent component = buildComponentInSimulation();

        component.startComponent();

        component.updatedComponent(new Hashtable(ImmutableMap.of("simulation", "false", "epicsChannel", channel)));

        verifyBindings(channel, 1);
    }

    @Test
    public void changeChannel() throws Exception {
        Channel<Double> epicsChannel = mock(Channel.class);
        when(channelFactory.createChannel(anyString(), any(List.class))).thenReturn(epicsChannel);

        PcsUpdaterComponent component = buildComponent();

        component.startComponent();

        component.updatedComponent(new Hashtable(ImmutableMap.of("simulation", "false", "epicsChannel", "test:newchannel")));

        verify(channelFactory).destroyChannel(any(ReadOnlyChannel.class));
        verify(channelFactory).createChannel(eq("test:newchannel"), eq(EpicsPcsUpdater.buildZeroZernikesArray()));
    }

}
