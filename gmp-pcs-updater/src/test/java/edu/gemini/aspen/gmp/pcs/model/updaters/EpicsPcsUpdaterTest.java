package edu.gemini.aspen.gmp.pcs.model.updaters;

import com.google.common.collect.ImmutableList;
import edu.gemini.aspen.gmp.pcs.model.PcsUpdate;
import edu.gemini.aspen.gmp.pcs.model.PcsUpdaterException;
import edu.gemini.cas.ChannelAccessServer;
import edu.gemini.epics.EpicsException;
import edu.gemini.epics.ReadWriteClientEpicsChannel;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EpicsPcsUpdater
 */
public class EpicsPcsUpdaterTest {
    private ReadWriteClientEpicsChannel ch = mock(ReadWriteClientEpicsChannel.class);
    private ChannelAccessServer channelFactory = mock(ChannelAccessServer.class);
    private String channel = "X:val1";
    private List<Double> gains = ImmutableList.of(1.0);
    private int taiDiff = 0;

    @Before
    public void setUp() throws CAException {
        doReturn(ch).when(channelFactory).createChannel(eq(channel), eq(EpicsPcsUpdater.buildZeroZernikesArray()));
        when(ch.getName()).thenReturn(channel);
    }

    @Test
    public void constructionWithDefaultChannel() throws Exception {
        new EpicsPcsUpdater(channelFactory, null, gains, taiDiff);
        verifyBindings(EpicsPcsUpdater.TCS_ZERNIKES_BASE_CHANNEL);
    }

    @Test
    public void constructionWithExplicitChannel() throws Exception {
        new EpicsPcsUpdater(channelFactory, channel, gains, taiDiff);
        verifyBindings(channel);
    }

    private void verifyBindings(String baseChannel) throws CAException {
        doReturn(ch).when(channelFactory).createChannel(eq(baseChannel), eq(EpicsPcsUpdater.buildZeroZernikesArray()));
    }

    @Test(expected = PcsUpdaterException.class)
    public void constructionWithNadEpicsChannel() throws Exception {
        doThrow(new CAException("Test exception", new RuntimeException())).when(channelFactory).createChannel(eq(channel), eq(EpicsPcsUpdater.buildZeroZernikesArray()));
        new EpicsPcsUpdater(channelFactory, channel, gains, taiDiff);
    }

    @Test
    public void pcsUpdateWithoutValues() throws PcsUpdaterException, EpicsException, CAException, TimeoutException {
        EpicsPcsUpdater pcsUpdater = new EpicsPcsUpdater(channelFactory, channel, gains, taiDiff);
        pcsUpdater.update(new PcsUpdate(new Double[0]));
        verifyBindings(channel);
        verify(ch, never()).setValue(anyDouble());
    }

    @Test
    public void simplePcsUpdate() throws PcsUpdaterException, EpicsException, CAException, TimeoutException {
        EpicsPcsUpdater pcsUpdater = new EpicsPcsUpdater(channelFactory, channel, gains, taiDiff);
        pcsUpdater.update(new PcsUpdate(new Double[]{1.0, 2.0}));

        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        verify(ch).setValue(argument.capture());

        assertTrue(((Double) argument.getValue().get(0)) <= (double) System.currentTimeMillis());
        assertEquals(Double.valueOf(2), argument.getValue().get(1));
        assertEquals(Double.valueOf(1.0), argument.getValue().get(2));
        assertEquals(Double.valueOf(2.0), argument.getValue().get(3));
        for (int i = 4; i < EpicsPcsUpdater.ARRAY_LENGTH; i++) {
            assertEquals(Double.valueOf(0.0), argument.getValue().get(i));
        }
    }

    @Test
    public void testTAICorrection() throws PcsUpdaterException, EpicsException, CAException, TimeoutException {
        EpicsPcsUpdater pcsUpdater = new EpicsPcsUpdater(channelFactory, channel, gains, 35);
        pcsUpdater.update(new PcsUpdate(new Double[]{1.0, 2.0}));

        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        verify(ch).setValue(argument.capture());

        assertTrue((((Double) argument.getValue().get(0)) - 35*1000) <= (double)System.currentTimeMillis());
        assertEquals(Double.valueOf(2), argument.getValue().get(1));
        assertEquals(Double.valueOf(1.0), argument.getValue().get(2));
        assertEquals(Double.valueOf(2.0), argument.getValue().get(3));
        for (int i = 4; i < EpicsPcsUpdater.ARRAY_LENGTH; i++) {
            assertEquals(Double.valueOf(0.0), argument.getValue().get(i));
        }
    }

    @Test
    public void nullPcsUpdate() throws PcsUpdaterException, EpicsException, CAException, TimeoutException {
        EpicsPcsUpdater pcsUpdater = new EpicsPcsUpdater(channelFactory, channel, gains, taiDiff);
        pcsUpdater.update(null);
        verifyBindings(channel);
        verify(ch, never()).setValue(anyDouble());
    }

    @Test
    public void verifyTooLongPcsUpdate() throws PcsUpdaterException, EpicsException, CAException, TimeoutException {
        EpicsPcsUpdater pcsUpdater = new EpicsPcsUpdater(channelFactory, channel, gains, taiDiff);

        Double[] zernikes = new Double[EpicsPcsUpdater.ARRAY_LENGTH + 1];
        for (int i = 0; i < EpicsPcsUpdater.ARRAY_LENGTH + 1; i++) {
            zernikes[i] = (double)i;
        }

        pcsUpdater.update(new PcsUpdate(zernikes));

        verifyBindings(channel);
        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        verify(ch).setValue(argument.capture());
        assertTrue(((Double) argument.getValue().get(0)) <= (double) System.currentTimeMillis());
        assertEquals(Double.valueOf(19), argument.getValue().get(1));
        for (int i = 2; i < EpicsPcsUpdater.ARRAY_LENGTH; i++) {
            assertEquals(Double.valueOf((double)i-2), argument.getValue().get(i));
        }
    }

    @Test
    public void testGains() throws PcsUpdaterException, EpicsException, CAException, TimeoutException {
        ImmutableList<Double> gains = ImmutableList.of(0.0, 3.0, 0.5, 2.5);
        EpicsPcsUpdater pcsUpdater = new EpicsPcsUpdater(channelFactory, channel, gains, taiDiff);

        Double[] zernikes = new Double[EpicsPcsUpdater.MAX_ZERNIKES];
        for (int i = 0; i < EpicsPcsUpdater.MAX_ZERNIKES; i++) {
            zernikes[i] = (double)i;
        }

        pcsUpdater.update(new PcsUpdate(zernikes));

        verifyBindings(channel);
        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        verify(ch).setValue(argument.capture());
        assertTrue(((Double) argument.getValue().get(0)) <= (double) System.currentTimeMillis());
        assertEquals(Double.valueOf(19), argument.getValue().get(1));
        assertEquals(Double.valueOf(0.0), argument.getValue().get(2));
        assertEquals(Double.valueOf(3.0), argument.getValue().get(3));
        assertEquals(Double.valueOf(1.0), argument.getValue().get(4));
        assertEquals(Double.valueOf(7.5), argument.getValue().get(5));
    }

    @Test
    public void testGPISample() throws PcsUpdaterException, EpicsException, CAException, TimeoutException {
        ImmutableList<Double> gains = ImmutableList.of(1.0);
        EpicsPcsUpdater pcsUpdater = new EpicsPcsUpdater(channelFactory, channel, gains, 35);

        Double[] zernikes = new Double[EpicsPcsUpdater.MAX_ZERNIKES + 1];
        zernikes[0]  =  0.0;
        zernikes[1]  =  0.0;
        zernikes[2]  =  0.0;
        zernikes[3]  =  1.4269236999098212E-4;
        zernikes[4]  = -1.173403870780021E-5;
        zernikes[5]  =  7.754975740681402E-6;
        zernikes[6]  =  5.736726052418817E-6;
        zernikes[7]  = -2.4758485324127832E-6;
        zernikes[8]  = -1.2555369721667375E-6;
        zernikes[9]  = -2.3237418645294383E-5;
        zernikes[10] = -2.1918547645327635E-5;
        zernikes[11] = -1.1538284070411464E-6;
        zernikes[12] =  1.753231231305108E-6;
        zernikes[13] = -7.1751132963981945E-6;
        zernikes[14] =  1.630298538657371E-5;
        zernikes[15] =  1.4524449397868011E-5;
        zernikes[16] =  2.3826235064916546E-6;
        zernikes[17] =  4.152916233124415E-7;
        zernikes[18] =  7.609909971506568E-6;
        zernikes[19] =  0.0;

        pcsUpdater.update(new PcsUpdate(zernikes));

        verifyBindings(channel);
        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        verify(ch).setValue(argument.capture());
        assertTrue((((Double) argument.getValue().get(0)) - 35*1000) <= (double)System.currentTimeMillis());
        assertEquals(Double.valueOf(19),  argument.getValue().get(1));
        assertEquals(Double.valueOf(0.0), argument.getValue().get(2));
        assertEquals(Double.valueOf(0.0), argument.getValue().get(3));
        assertEquals(Double.valueOf(0.0), argument.getValue().get(4));
        assertEquals(Double.valueOf( 1.4269236999098212E-4), argument.getValue().get(5));
        assertEquals(Double.valueOf(-1.173403870780021E-5),  argument.getValue().get(6));
        assertEquals(Double.valueOf( 7.754975740681402E-6),  argument.getValue().get(7));
        assertEquals(Double.valueOf( 5.736726052418817E-6),  argument.getValue().get(8));
        assertEquals(Double.valueOf(-2.4758485324127832E-6), argument.getValue().get(9));
        assertEquals(Double.valueOf(-1.2555369721667375E-6), argument.getValue().get(10));
        assertEquals(Double.valueOf(-2.3237418645294383E-5), argument.getValue().get(11));
        assertEquals(Double.valueOf(-2.1918547645327635E-5), argument.getValue().get(12));
        assertEquals(Double.valueOf(-1.1538284070411464E-6), argument.getValue().get(13));
        assertEquals(Double.valueOf( 1.753231231305108E-6),  argument.getValue().get(14));
        assertEquals(Double.valueOf(-7.1751132963981945E-6), argument.getValue().get(15));
        assertEquals(Double.valueOf( 1.630298538657371E-5),  argument.getValue().get(16));
        assertEquals(Double.valueOf( 1.4524449397868011E-5), argument.getValue().get(17));
        assertEquals(Double.valueOf( 2.3826235064916546E-6), argument.getValue().get(18));
        assertEquals(Double.valueOf( 4.152916233124415E-7),  argument.getValue().get(19));
        assertEquals(Double.valueOf( 7.609909971506568E-6),  argument.getValue().get(20));

        assertEquals(Double.valueOf(0.0),  argument.getValue().get(21));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(22));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(23));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(24));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(25));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(26));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(27));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(28));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(29));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(30));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(31));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(32));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(33));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(34));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(35));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(36));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(37));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(38));
        assertEquals(Double.valueOf(0.0),  argument.getValue().get(39));
    }
}
