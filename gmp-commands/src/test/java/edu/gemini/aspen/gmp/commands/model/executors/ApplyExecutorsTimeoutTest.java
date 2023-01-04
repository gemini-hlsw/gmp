package edu.gemini.aspen.gmp.commands.model.executors;

import com.google.common.collect.ImmutableList;
import edu.gemini.aspen.giapi.commands.*;
import edu.gemini.aspen.giapitestsupport.commands.CompletionListenerMock;
import edu.gemini.aspen.gmp.commands.handlers.CommandHandlers;
import edu.gemini.aspen.gmp.commands.model.Action;
import edu.gemini.aspen.gmp.commands.model.ActionMessageBuilder;
import edu.gemini.aspen.gmp.commands.model.impl.ActionManagerImpl;
import edu.gemini.aspen.gmp.commands.model.impl.CommandUpdaterImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static edu.gemini.aspen.giapi.commands.ConfigPath.configPath;
import static edu.gemini.aspen.giapi.commands.DefaultConfiguration.configurationBuilder;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for the sender of APPLY sequence commands.
 */
public class ApplyExecutorsTimeoutTest {

    private ApplySenderExecutor _executor;

    private Configuration _applyConfig;
    private ActionManagerImpl actionManager;
    private ActionMessageBuilder builder = new MockActionMessageBuilder();
    private CommandHandlers handlers = mock(CommandHandlers.class);
    private CommandUpdater updater;

    @Before
    public void setUp() {
        actionManager = new ActionManagerImpl();
        actionManager.start();
        updater = new CommandUpdaterImpl(actionManager);
    }

    @After
    public void shutDown() {
        actionManager.stop();
    }

    /**
     * This test the case with immediate COMPLETION
     */
    @Test
    public void testOneStartedOneTimeout() throws InterruptedException {
        _executor = new ApplySenderExecutor(builder, actionManager, handlers);

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S2:C.val1"), "xc1")
                .build();

        Command command = new Command(
                SequenceCommand.APPLY,
                Activity.START,
                _applyConfig);


        CompletionListenerMock listener = new CompletionListenerMock();
        final Action action = new Action(command, listener);
        actionManager.registerAction(action);

        List<ConfigPath> registeredHandlers = ImmutableList.of(configPath("X:S1"), configPath("X:S2"));
        when(handlers.getApplyHandlers()).thenReturn(registeredHandlers);

        ActionSenderMock sender = new ActionSenderMock() {
            @Override
            public HandlerResponse responseFor(Configuration subConfigurationS1, Configuration subConfigurationS2) {
                if (subConfigurationS1.isEmpty()) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(500);
                                updater.updateOcs(action.getId(), HandlerResponse.COMPLETED);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
                return HandlerResponse.STARTED;
            }
        };
        HandlerResponse myResponse = _executor.execute(action, sender);
        assertFalse(actionManager.isComplete(action));
        assertEquals(HandlerResponse.STARTED, myResponse);
        listener.waitForCompletion(1000);
        assertFalse(listener.wasInvoked());
        // 2 responses. overall COMPLETED
        assertEquals(2, sender.getCallsCounter());
    }

    /**
     * This test the case with immediate COMPLETION
     */
    @Test
    public void testBothStarted() throws InterruptedException {
        _executor = new ApplySenderExecutor(builder, actionManager, handlers);

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S2:C.val1"), "xc1")
                .build();

        Command command = new Command(
                SequenceCommand.APPLY,
                Activity.START,
                _applyConfig);

        final Action action = new Action(command, new CompletionListenerMock());
        actionManager.registerAction(action);

        List<ConfigPath> registeredHandlers = ImmutableList.of(configPath("X:S1"), configPath("X:S2"));
        when(handlers.getApplyHandlers()).thenReturn(registeredHandlers);

        ActionSenderMock sender = new ActionSenderMock() {
            @Override
            public HandlerResponse responseFor(Configuration subConfigurationS1, Configuration subConfigurationS2) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(500);
                            updater.updateOcs(action.getId(), HandlerResponse.COMPLETED);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                return HandlerResponse.STARTED;
            }
        };
        HandlerResponse myResponse = _executor.execute(action, sender);
        assertFalse(actionManager.isComplete(action));
        assertEquals(HandlerResponse.STARTED, myResponse);
        Thread.sleep(1000);
        assertTrue(actionManager.isComplete(action));
        // 2 responses. overall COMPLETED
        assertEquals(2, sender.getCallsCounter());
    }

    /**
     * This test a case seen in GHOST
     */
    @Test
    public void testOneCompletedBeforeTheOtherStarts() throws InterruptedException {
        _executor = new ApplySenderExecutor(builder, actionManager, handlers);

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S2:C.val1"), "xc1")
                .build();

        Command command = new Command(
                SequenceCommand.APPLY,
                Activity.START,
                _applyConfig);

        final Action action = new Action(command, new CompletionListenerMock());
        actionManager.registerAction(action);

        List<ConfigPath> registeredHandlers = ImmutableList.of(configPath("X:S1"), configPath("X:S2"));
        when(handlers.getApplyHandlers()).thenReturn(registeredHandlers);

        ActionSenderMock sender = new ActionSenderMock() {
            @Override
            public HandlerResponse responseFor(Configuration subConfigurationS1, Configuration subConfigurationS2) {
                if (subConfigurationS1.isEmpty()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(100);
                            updater.updateOcs(action.getId(), HandlerResponse.COMPLETED);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                return HandlerResponse.STARTED;
            }
        };
        HandlerResponse myResponse = _executor.execute(action, sender);
        assertFalse(actionManager.isComplete(action));
        assertEquals(HandlerResponse.STARTED, myResponse);
        Thread.sleep(1000);
        assertTrue(actionManager.isComplete(action));
        // 2 responses. overall COMPLETED
        assertEquals(2, sender.getCallsCounter());
    }
}