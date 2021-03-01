package edu.gemini.epics;

import com.google.common.collect.ImmutableList;
import edu.gemini.cas.impl.ChannelAccessServerImpl;
import edu.gemini.epics.api.Channel;
import edu.gemini.epics.api.ChannelListener;
import edu.gemini.epics.impl.EpicsReaderImpl;
import edu.gemini.epics.impl.ReadOnlyEpicsChannelImpl;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReadOnlyClientEpicsChannelTest {

    private static String CHANNEL_NAME="dummy:test";
    private ChannelAccessServerImpl giapicas;
    private EpicsService epicsService;
    private EpicsReader epicsReader;

    @Before
    public void setUp() throws Exception {
        giapicas = new ChannelAccessServerImpl();
        giapicas.start();

        epicsService = new EpicsService("127.0.0.1", 1.0, 0);
        epicsService.startService();
        epicsReader = new EpicsReaderImpl(epicsService);
    }

    @After
    public void tearDown() throws Exception {
        epicsService.stopService();
        epicsService = null;

        Thread.sleep(20);//can't start and stop immediately, and our tests are too short
        giapicas.stop();
    }

    @Test
    @Ignore
    public void testReadInteger() throws CAException, TimeoutException {
        int testValue = 1;
        giapicas.createChannel(CHANNEL_NAME, testValue);

        ReadOnlyClientEpicsChannel<Integer> roChannel = epicsReader.getIntegerChannel(CHANNEL_NAME);

        int readValue = roChannel.getFirst();

        assertTrue("Failed to read Integer channel.", readValue==testValue);

    }

    @Test
    @Ignore
    public void testReadShort() throws CAException, TimeoutException {
        short testValue = 1;
        Channel ch = giapicas.createChannel(CHANNEL_NAME, testValue);

        ReadOnlyClientEpicsChannel<Short> roChannel = epicsReader.getShortChannel(CHANNEL_NAME);

        short readValue = roChannel.getFirst();

        assertTrue("Failed to read Short channel.", readValue==testValue);

    }

    @Test
    @Ignore
    public void testReadDouble() throws CAException, TimeoutException {
        double testValue = 2.0;
        giapicas.createChannel(CHANNEL_NAME, testValue);

        ReadOnlyClientEpicsChannel<Double> roChannel = epicsReader.getDoubleChannel(CHANNEL_NAME);

        double readValue = roChannel.getFirst();

        assertTrue("Failed to read Double channel.", readValue==testValue);
    }

    @Test
    @Ignore
    public void testReadFloat() throws CAException, TimeoutException {
        float testValue = 3.0f;
        giapicas.createChannel(CHANNEL_NAME, testValue);

        ReadOnlyClientEpicsChannel<Float> roChannel = epicsReader.getFloatChannel(CHANNEL_NAME);

        float readValue = roChannel.getFirst();

        assertTrue("Failed to read Float channel.", readValue==testValue);
    }

    @Test
    @Ignore
    public void testReadString() throws CAException, TimeoutException {
        String testValue = "dummy string";
        giapicas.createChannel(CHANNEL_NAME, testValue);

        ReadOnlyClientEpicsChannel<String> roChannel = epicsReader.getStringChannel(CHANNEL_NAME);

        String readValue = roChannel.getFirst();

        assertEquals("Failed to read String channel.", readValue, testValue);
    }

    enum DummyEnum {
        VAL1, VAL2, VAL3
    }

    @Test
    @Ignore
    public void testReadEnum() throws CAException, TimeoutException {
        DummyEnum testValue = DummyEnum.VAL2;
        giapicas.createChannel(CHANNEL_NAME, testValue);

        ReadOnlyClientEpicsChannel<DummyEnum> roChannel = epicsReader.getEnumChannel(CHANNEL_NAME, DummyEnum.class);

        DummyEnum readValue = roChannel.getFirst();

        assertEquals("Failed to read Enum channel.", readValue, testValue);
    }

    private boolean updated = false;

    @Test
    @Ignore
    public void testMonitor() throws CAException, TimeoutException {
        Channel ch = giapicas.createChannel(CHANNEL_NAME, 1);

        ReadOnlyClientEpicsChannel<Integer> roChannel = epicsReader.getIntegerChannel(CHANNEL_NAME);
        roChannel.registerListener(new ChannelListener<Integer>() {
            @Override
            public void valueChanged(String channelName, List<Integer> values) {
                updated = true;
            }
        });

        ch.setValue(ImmutableList.of(2));

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue("Failed to trigger channel monitor.", updated);

    }

    @Test
    @Ignore
    public void testSetReadRetries() throws CAException, TimeoutException {
        int testValue = 1;
        int readRetries = 3;
        giapicas.createChannel(CHANNEL_NAME, testValue);

        epicsService.setReadRetries(readRetries);
        EpicsReader reader = new EpicsReaderImpl(epicsService);

        ReadOnlyEpicsChannelImpl<Integer> roChannel =  (ReadOnlyEpicsChannelImpl<Integer>)reader.getIntegerChannel(CHANNEL_NAME);

        int rr = roChannel.getReadRetries();

        assertTrue("Failed to set number of read retries.", readRetries==rr);

    }

}
