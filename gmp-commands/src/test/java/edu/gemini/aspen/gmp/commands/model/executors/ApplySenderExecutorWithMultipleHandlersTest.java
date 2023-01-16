package edu.gemini.aspen.gmp.commands.model.executors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import edu.gemini.aspen.giapi.commands.*;
import edu.gemini.aspen.giapi.util.jms.JmsKeys;
import edu.gemini.aspen.giapitestsupport.commands.CompletionListenerMock;
import edu.gemini.aspen.gmp.commands.handlers.CommandHandlers;
import edu.gemini.aspen.gmp.commands.model.*;
import edu.gemini.aspen.gmp.commands.model.impl.ActionManagerImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.gemini.aspen.giapi.commands.ConfigPath.configPath;
import static edu.gemini.aspen.giapi.commands.DefaultConfiguration.configurationBuilder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Test class for the sender of APPLY sequence commands.
 */
public class ApplySenderExecutorWithMultipleHandlersTest {

    private ApplySenderExecutor _executor;

    private Configuration _applyConfig;
    private ActionManagerImpl actionManager;
    private ActionMessageBuilder builder = new MockActionMessageBuilder();
    private CommandHandlers handlers = mock(CommandHandlers.class);

    @Before
    public void setUp() {
        actionManager = new ActionManagerImpl();
        actionManager.start();
    }

    @After
    public void shutDown() {
        actionManager.stop();
    }

    /**
     * This test runs with the case that there is no top level X handler
     * but there are several sub handlers
     */
    @Test
    public void testCommandWithoutCommandHandlers() {
        _executor = new ApplySenderExecutor(builder, actionManager, handlers);

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S1:A.val2"), "xa2")
                .withPath(configPath("X:S1.A.val2"), "xa2")
                .withPath(configPath("X:S1:A.val3"), "xa3")
                .withPath(configPath("X:S1:B.val1"), "xb1")
                .withPath(configPath("X:S1:B.val2"), "xb2")
                .withPath(configPath("X:S1:B.val3"), "xb3")
                .withPath(configPath("X:S2:C.val1"), "xc1")
                .withPath(configPath("X:S2:C.val2"), "xc2")
                .withPath(configPath("X:S2:C.val3"), "xc3")
                .build();

        Command command = new Command(
                SequenceCommand.APPLY,
                Activity.START,
                _applyConfig);

        Action action = new Action(command, new CompletionListenerMock());

        List<ConfigPath> registeredHandlers = ImmutableList.of(configPath("X:S1"), configPath("X:S2"));
        when(handlers.getApplyHandlers()).thenReturn(registeredHandlers);

        ActionSenderMockWithoutTopLevel sender = new ActionSenderMockWithoutTopLevel();
        HandlerResponse myResponse = _executor.execute(action, sender);
        assertEquals(HandlerResponse.ACCEPTED, myResponse);
        assertEquals(2, sender.getCallsCounter());
    }

    /**
     * Count the handlers
     */
    @Test
    public void testCountResponses() {
        _executor = new ApplySenderExecutor(builder, actionManager, handlers);
        List<ConfigPath> registeredHandlers = ImmutableList.of(configPath("X:S1"), configPath("X:S2"));
        when(handlers.getApplyHandlers()).thenReturn(registeredHandlers);

        Configuration _applyConfig1 = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S1:A.val2"), "xa2")
                .withPath(configPath("X:S1.A.val2"), "xa2")
                .withPath(configPath("X:S1:A.val3"), "xa3")
                .withPath(configPath("X:S1:B.val1"), "xb1")
                .withPath(configPath("X:S1:B.val2"), "xb2")
                .withPath(configPath("X:S1:B.val3"), "xb3")
                .withPath(configPath("X:S2:C.val1"), "xc1")
                .withPath(configPath("X:S2:C.val2"), "xc2")
                .withPath(configPath("X:S2:C.val3"), "xc3")
                .build();

        assertEquals(2, _executor.countExpectedResponses(_applyConfig1, ConfigPath.EMPTY_PATH));

        Configuration _applyConfig2 = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S1:A.val2"), "xa2")
                .withPath(configPath("X:S1.A.val2"), "xa2")
                .withPath(configPath("X:S1:A.val3"), "xa3")
                .withPath(configPath("X:S1:B.val1"), "xb1")
                .withPath(configPath("X:S1:B.val2"), "xb2")
                .withPath(configPath("X:S1:B.val3"), "xb3")
                .build();

        assertEquals(1, _executor.countExpectedResponses(_applyConfig2, ConfigPath.EMPTY_PATH));

        Configuration _applyConfig3 = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S2:C.val3"), "xc3")
                .withPath(configPath("X:S3:C.val3"), "xc3") // This one is unhandled
                .build();

        assertEquals(1, _executor.countExpectedResponses(_applyConfig3, ConfigPath.EMPTY_PATH));

        assertEquals(1, _executor.countExpectedResponses(configurationBuilder().build(), ConfigPath.EMPTY_PATH));
    }

    /**
     * This test runs with the case that there is no top level X handler
     * but there are several sub handlers, and the CommandHandlers can inform the decision
     */
    @Test
    public void testCommandWithCommandHandlers() {

        _executor = new ApplySenderExecutor(builder, actionManager, handlers);
        List<ConfigPath> registeredHandlers = ImmutableList.of(configPath("X:S1"), configPath("X:S2"));
        when(handlers.getApplyHandlers()).thenReturn(registeredHandlers);

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S1:A.val2"), "xa2")
                .withPath(configPath("X:S1.A.val2"), "xa2")
                .withPath(configPath("X:S1:A.val3"), "xa3")
                .withPath(configPath("X:S1:B.val1"), "xb1")
                .withPath(configPath("X:S1:B.val2"), "xb2")
                .withPath(configPath("X:S1:B.val3"), "xb3")
                .withPath(configPath("X:S2:C.val1"), "xc1")
                .withPath(configPath("X:S2:C.val2"), "xc2")
                .withPath(configPath("X:S2:C.val3"), "xc3")
                .build();

        Command command = new Command(
                SequenceCommand.APPLY,
                Activity.START,
                _applyConfig);

        Action action = new Action(command, new CompletionListenerMock());

        ActionSenderMockWithoutTopLevel sender = new ActionSenderMockWithoutTopLevel();
        HandlerResponse myResponse = _executor.execute(action, sender);
        assertEquals(HandlerResponse.ACCEPTED, myResponse);
        assertEquals(2, sender.getCallsCounter());
    }

    /**
     * This test runs with the case that there is no top level X handler
     * but there are several sub handlers, and the CommandHandlers can inform the decision
     */
    @Test
    public void testCommandWithOneLevelCommandHandlers() {

        _executor = new ApplySenderExecutor(builder, actionManager, handlers);

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:A.val1"), "xa1")
                .withPath(configPath("X:A.val2"), "xa2")
                .withPath(configPath("X:B.val1"), "xb1")
                .withPath(configPath("X:B.val2"), "xb2")
                .build();

        Command command = new Command(
                SequenceCommand.APPLY,
                Activity.START,
                _applyConfig);

        Action action = new Action(command, new CompletionListenerMock());

        List<ConfigPath> registeredHandlers = ImmutableList.of(configPath("X:A"), configPath("X:B"));
        when(handlers.getApplyHandlers()).thenReturn(registeredHandlers);

        ActionSenderMockWithoutTopLevel sender = new ActionSenderMockWithoutTopLevel();
        HandlerResponse myResponse = _executor.execute(action, sender);
        assertEquals(HandlerResponse.ACCEPTED, myResponse);
        assertEquals(2, sender.getCallsCounter());
    }

    private static class ActionSenderMockWithoutTopLevel implements ActionSender {
        private int callsCounter = 0;

        public int getCallsCounter() {
            return callsCounter;
        }

        @Override
        public HandlerResponse send(ActionMessage message) throws SequenceCommandException {
            return send(message, 0);
        }

        @Override
        public HandlerResponse send(ActionMessage message, long timeout) throws SequenceCommandException {
            callsCounter++;

            DefaultConfiguration.Builder configBuilder = configurationBuilder();
            for (String key : message.getDataElements().keySet()) {
                configBuilder.withConfiguration(key, message.getDataElements().get(key).toString());
            }
            Configuration applyConfig = configBuilder.build();
            Configuration subConfigurationS1 = applyConfig.getSubConfiguration(configPath("X:S1"));
            Configuration subConfigurationS2 = applyConfig.getSubConfiguration(configPath("X:S2"));

            // If only a sub path is being requested we respond accepted
            // This is the same as saying that there is no top level handler, only a sub handler
            if (subConfigurationS1.isEmpty() || subConfigurationS2.isEmpty()) {
                return HandlerResponse.ACCEPTED;
            } else {
                return HandlerResponse.NOANSWER;
            }
        }
    }
}