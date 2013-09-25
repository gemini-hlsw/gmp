package edu.gemini.aspen.gmp.pcs.model.updaters;

import edu.gemini.aspen.gmp.pcs.model.PcsUpdate;
import edu.gemini.aspen.gmp.pcs.model.PcsUpdaterException;
import edu.gemini.cas.ChannelAccessServer;
import edu.gemini.cas.ChannelFactory;
import edu.gemini.epics.EpicsException;
import edu.gemini.epics.EpicsWriter;
import edu.gemini.epics.ReadWriteClientEpicsChannel;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EpicsPcsUpdater
 */
public class EpicsPcsUpdaterTest {
    private EpicsWriter writer = mock(EpicsWriter.class);
    private ReadWriteClientEpicsChannel ch = mock(ReadWriteClientEpicsChannel.class);
    private ChannelAccessServer channelFactory = mock(ChannelAccessServer.class);
    private String channel = "X:val1";

    @Before
    public void setUp(){
        doReturn(ch).when(writer).getDoubleChannel(anyString());
    }

    @Test
    public void constructionWithDefaultChannel() throws PcsUpdaterException, EpicsException {
        new EpicsPcsUpdater(channelFactory, writer, null);
        verifyBindings(EpicsPcsUpdater.TCS_ZERNIKES_BASE_CHANNEL);
    }

    @Test
    public void constructionWithExplicitChannel() throws PcsUpdaterException, EpicsException {
        new EpicsPcsUpdater(channelFactory, writer, channel);
        verifyBindings(channel);
    }

    private void verifyBindings(String baseChannel) {
        verify(writer).getDoubleChannel(baseChannel);
    }

    @Test(expected = PcsUpdaterException.class)
    public void constructionWithNadEpicsChannel() throws PcsUpdaterException, EpicsException {
        doThrow(new EpicsException("Test exception", new RuntimeException())).when(writer).getDoubleChannel(anyString());
        new EpicsPcsUpdater(channelFactory, writer, channel);
    }

    @Test
    public void pcsUpdateWithoutValues() throws PcsUpdaterException, EpicsException, CAException, TimeoutException {
        EpicsPcsUpdater pcsUpdater = new EpicsPcsUpdater(channelFactory, writer, channel);
        pcsUpdater.update(new PcsUpdate(new Double[0]));
        verifyBindings(channel);
        verify(ch, never()).setValue(anyDouble());
    }

    @Test
    public void simplePcsUpdate() throws PcsUpdaterException, EpicsException, CAException, TimeoutException {
        EpicsPcsUpdater pcsUpdater = new EpicsPcsUpdater(channelFactory, writer, channel);
        pcsUpdater.update(new PcsUpdate(new Double[]{1.0, 2.0}));

        InOrder inOrder= inOrder(ch);
        inOrder.verify(ch).setValue(eq(1.0));
        inOrder.verify(ch).setValue(eq(2.0));

    }

    @Test
    public void nullPcsUpdate() throws PcsUpdaterException, EpicsException, CAException, TimeoutException {
        EpicsPcsUpdater pcsUpdater = new EpicsPcsUpdater(channelFactory, writer, channel);
        pcsUpdater.update(null);
        verifyBindings(channel);
        verify(ch, never()).setValue(anyDouble());
    }

    //@Test
    /*public void verifyTooLongPcsUpdate() throws PcsUpdaterException, EpicsException, CAException, TimeoutException {
        EpicsPcsUpdater pcsUpdater = new EpicsPcsUpdater(channelFactory, writer, channel);

        Double[] zernikes = new Double[EpicsPcsUpdater.INPUTS.length + 1];
        for (int i = 0; i < EpicsPcsUpdater.INPUTS.length + 1; i++) {
            zernikes[i] = (double)i;
        }

        pcsUpdater.update(new PcsUpdate(zernikes));

        verifyBindings(channel);
        InOrder inOrder= inOrder(ch);

        for (int i = 0; i < EpicsPcsUpdater.INPUTS.length; i++) {
            inOrder.verify(ch).setValue(eq(zernikes[i]));
        }

        verifyNoMoreInteractions(writer);
    }*/
}
