package edu.gemini.aspen.gmp.epics.simulator;

import edu.gemini.aspen.gmp.epics.EpicsRegistrar;
import edu.gemini.aspen.gmp.epics.EpicsUpdate;
import edu.gemini.aspen.gmp.epics.simulator.channels.SimulatedEpicsChannel;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ChannelSimulatorTest {
    private static final int PASSES = 10;

    @Test
    public void testRunningSimulator() throws InterruptedException {
        ScheduledExecutorService executorService =
                Executors.newScheduledThreadPool(1);
        long updateRate = 10L;
        String channelName = "channel";
        int size = 5;

        SimulatedEpicsChannel channel = SimulatedEpicsChannel
                .buildSimulatedEpicsChannel(channelName, size, DataType.INT, updateRate);

        EpicsRegistrar registrar = mock(EpicsRegistrar.class);
        ChannelSimulator simulator = new ChannelSimulator(channel, registrar);
        executorService.scheduleAtFixedRate(simulator, 0, updateRate, TimeUnit.MILLISECONDS);

        TimeUnit.MILLISECONDS.sleep(PASSES * updateRate);

        verify(registrar, atLeast(PASSES - 2)).processEpicsUpdate(any(EpicsUpdate.class));
    }
}
