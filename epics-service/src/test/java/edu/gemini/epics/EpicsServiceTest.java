package edu.gemini.epics;

import com.cosylab.epics.caj.CAJContext;
import gov.aps.jca.Context;
import gov.aps.jca.event.DirectEventDispatcher;
import gov.aps.jca.event.LatestMonitorOnlyQueuedEventDispatcher;
import gov.aps.jca.event.QueuedEventDispatcher;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileAlreadyExistsException;

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

    @Test
    public void testSetDirectEventDispatcher() {
        epicsService.setEventDispatcherSelector(new DirectEventDispatcherSelector());

        epicsService.startService();

        assert(epicsService.getJCAContext().getEventDispatcher() instanceof DirectEventDispatcher);
    }

    @Test
    public void testSetQueuedEventDispatcher() {
        int QUEUE_LIMIT = 66;
        int PRIORITY = Thread.NORM_PRIORITY + 1;
        int CHANNEL_QUEUE_LIMIT = 4;

        epicsService.setEventDispatcherSelector(new QueuedEventDispatcherSelector()
                .setPriority(PRIORITY)
                .setPerChannelLimit(CHANNEL_QUEUE_LIMIT)
                .setQueueLimit(QUEUE_LIMIT)
        );

        epicsService.startService();

        assert(epicsService.getJCAContext().getEventDispatcher() instanceof QueuedEventDispatcher);
        QueuedEventDispatcher eventDispatcher = (QueuedEventDispatcher) epicsService.getJCAContext().getEventDispatcher();
        assertEquals(eventDispatcher.getPriority(), PRIORITY);
        // There are no "getters" for the next two attributes. The next best thing is to test the properties that
        // QueuedEventDispatcher uses
        assertEquals(Integer.parseInt(System.getProperty(QueuedEventDispatcher.class.getName() + ".queue_limit")), QUEUE_LIMIT);
        assertEquals(Integer.parseInt(System.getProperty(QueuedEventDispatcher.class.getName() + ".channel_queue_limit")), CHANNEL_QUEUE_LIMIT);
    }


    @Test
    public void testSetLatestMonitorOnlyQueuedEventDispatcherSelector() {
        int QUEUE_LIMIT = 66;
        int PRIORITY = Thread.NORM_PRIORITY + 1;
        int CHANNEL_QUEUE_LIMIT = 4;
        String FILE_OUTPUT = "stdout";

        epicsService.setEventDispatcherSelector(new LatestMonitorOnlyQueuedEventDispatcherSelector()
                .setPriority(PRIORITY)
                .setPerChannelLimit(CHANNEL_QUEUE_LIMIT)
                .setQueueLimit(QUEUE_LIMIT)
                .setMonitorOutput(FILE_OUTPUT)
        );

        epicsService.startService();

        assert(epicsService.getJCAContext().getEventDispatcher() instanceof LatestMonitorOnlyQueuedEventDispatcher);
        LatestMonitorOnlyQueuedEventDispatcher eventDispatcher =
                (LatestMonitorOnlyQueuedEventDispatcher) epicsService.getJCAContext().getEventDispatcher();
        assertEquals(eventDispatcher.getPriority(), PRIORITY);
        // There are no "getters" for the next attributes. The next best thing is to test the properties that
        // LatestMonitorOnlyQueuedEventDispatcher uses
        assertEquals(
            Integer.parseInt(System.getProperty(LatestMonitorOnlyQueuedEventDispatcher.class.getName() + ".queue_limit")),
            QUEUE_LIMIT
        );
        assertEquals(
            Integer.parseInt(System.getProperty(LatestMonitorOnlyQueuedEventDispatcher.class.getName() + ".channel_queue_limit")),
            CHANNEL_QUEUE_LIMIT
        );
        assertEquals(
            System.getProperty(LatestMonitorOnlyQueuedEventDispatcher.class.getName() + ".monitor_output"),
            FILE_OUTPUT
        );
    }

}
