package edu.gemini.aspen.gmp.epics.simulator;

import edu.gemini.aspen.gmp.epics.EpicsRegistrar;
import edu.gemini.aspen.gmp.epics.EpicsUpdate;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

public class EpicsSimulatorComponentTest {
    private EpicsSimulatorComponent component;

    @Test
    public void testAndWaitForUpdate() throws InterruptedException {
        EpicsRegistrar registrar = mock(EpicsRegistrar.class);
        File configFile = new File(getClass().getResource("simulated-epics-channels.xml").getFile());
        component = new EpicsSimulatorComponent(registrar, configFile.getAbsolutePath());
        component.readConfiguration();
        component.startSimulation();

        // Wait for one update
        TimeUnit.MILLISECONDS.sleep(1100L);

        verify(registrar, atLeastOnce()).processEpicsUpdate(any(EpicsUpdate.class));
    }

    @Test
    public void testWithBadConfiguration() throws InterruptedException {
        EpicsRegistrar registrar = mock(EpicsRegistrar.class);
        component = new EpicsSimulatorComponent(registrar, "badFile");
        component.readConfiguration();
        component.startSimulation();

        // Wait for one update
        TimeUnit.MILLISECONDS.sleep(1100L);

        verifyNoInteractions(registrar);
    }

    @After
    public void cleanupAfterTests() {
        component.stopSimulation();
    }
}
