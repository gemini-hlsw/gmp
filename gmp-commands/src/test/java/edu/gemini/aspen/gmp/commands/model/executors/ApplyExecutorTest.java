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
public class ApplyExecutorTest {

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

        assert (_executor.canBeFullyHandled(_applyConfig1));
        assertEquals(2, _executor.countExpectedResponses(_applyConfig1, ConfigPath.EMPTY_PATH));

        Configuration _applyConfig3 = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S2:C.val3"), "xc3")
                .withPath(configPath("X:S3:C.val3"), "xc3") // This one is unhandled
                .build();

        assert (!_executor.canBeFullyHandled(_applyConfig3));
        assertEquals(1, _executor.countExpectedResponses(_applyConfig3, ConfigPath.EMPTY_PATH));
    }

    /**
     * This test runs with the case that there is no top level X handler
     * but there are several sub handlers
     */
    @Test
    public void testCommandWithNotEnoughCommandHandlers() {
        _executor = new ApplySenderExecutor(builder, actionManager, handlers);

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S2:C.val1"), "xc1")
                .withPath(configPath("X:S3:C.val3"), "xc3") // This is unhandled
                .build();

        Command command = new Command(
                SequenceCommand.APPLY,
                Activity.START,
                _applyConfig);

        Action action = new Action(command, new CompletionListenerMock());

        List<ConfigPath> registeredHandlers = ImmutableList.of(configPath("X:S1"), configPath("X:S2"));
        when(handlers.getApplyHandlers()).thenReturn(registeredHandlers);

        ActionSenderMock sender = new ActionSenderMock() {
            @Override
            public HandlerResponse responseFor(Configuration subConfigurationS1, Configuration subConfigurationS2) {
                return HandlerResponse.NOANSWER;
            }
        };
        HandlerResponse myResponse = _executor.execute(action, sender);
        assertEquals(HandlerResponse.NOANSWER, myResponse);
        assertEquals(0, sender.getCallsCounter());
    }

    /**
     * This test the case there are no handlers at all
     */
    @Test
    public void testCommandNoHandlersAtAll() {
        _executor = new ApplySenderExecutor(builder, actionManager, handlers);

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S2:C.val1"), "xc1")
                .withPath(configPath("X:S3:C.val3"), "xc3")
                .build();

        Command command = new Command(
                SequenceCommand.APPLY,
                Activity.START,
                _applyConfig);

        Action action = new Action(command, new CompletionListenerMock());

        ActionSenderMock sender = new ActionSenderMock() {
            @Override
            public HandlerResponse responseFor(Configuration subConfigurationS1, Configuration subConfigurationS2) {
                return HandlerResponse.NOANSWER;
            }
        };
        HandlerResponse myResponse = _executor.execute(action, sender);
        assertEquals(HandlerResponse.NOANSWER, myResponse);
        // No sub calls in this case
        assertEquals(0, sender.getCallsCounter());
    }

    /**
     * This test the case there are no handlers at all
     */
    @Test
    public void testCommandNoHandlersForTheFullApply() {
        _executor = new ApplySenderExecutor(builder, actionManager, handlers);

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:S3:C.val3"), "xc3") // unknown case
                .build();

        Command command = new Command(
                SequenceCommand.APPLY,
                Activity.START,
                _applyConfig);

        Action action = new Action(command, new CompletionListenerMock());
        List<ConfigPath> registeredHandlers = ImmutableList.of(configPath("X:S1"), configPath("X:S2"));
        when(handlers.getApplyHandlers()).thenReturn(registeredHandlers);

        ActionSenderMock sender = new ActionSenderMock() {
            @Override
            public HandlerResponse responseFor(Configuration subConfigurationS1, Configuration subConfigurationS2) {
                return HandlerResponse.NOANSWER;
            }
        };
        HandlerResponse myResponse = _executor.execute(action, sender);
        assertEquals(HandlerResponse.NOANSWER, myResponse);
        // No sub calls in this case
        assertEquals(0, sender.getCallsCounter());
    }

    /**
     * This test the case when both sides return a NOANSWER
     */
    @Test
    public void testCommandNoAnswer1() {
        _executor = new ApplySenderExecutor(builder, actionManager, handlers);

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S2:C.val1"), "xc1")
                .build();

        Command command = new Command(
                SequenceCommand.APPLY,
                Activity.START,
                _applyConfig);

        Action action = new Action(command, new CompletionListenerMock());

        List<ConfigPath> registeredHandlers = ImmutableList.of(configPath("X:S1"), configPath("X:S2"));
        when(handlers.getApplyHandlers()).thenReturn(registeredHandlers);

        // s1 and s2 return NOANSWER
        ActionSenderMock sender = new ActionSenderMock() {
            @Override
            public HandlerResponse responseFor(Configuration subConfigurationS1, Configuration subConfigurationS2) {
                return HandlerResponse.NOANSWER;
            }
        };
        HandlerResponse myResponse = _executor.execute(action, sender);
        assertEquals(HandlerResponse.NOANSWER, myResponse);
        // Only 1 response as we stop as soon as there is an unhandled response
        assertEquals(1, sender.getCallsCounter());
    }

    /**
     * This test the case only one sides return a NOANSWER
     */
    @Test
    public void testCommandNoAnswerAndCompleted() {
        _executor = new ApplySenderExecutor(builder, actionManager, handlers);

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S2:C.val1"), "xc1")
                .build();

        Command command = new Command(
                SequenceCommand.APPLY,
                Activity.START,
                _applyConfig);

        Action action = new Action(command, new CompletionListenerMock());

        List<ConfigPath> registeredHandlers = ImmutableList.of(configPath("X:S1"), configPath("X:S2"));
        when(handlers.getApplyHandlers()).thenReturn(registeredHandlers);

        // s2 returns NOANSWER
        ActionSenderMock sender = new ActionSenderMock() {
            @Override
            public HandlerResponse responseFor(Configuration subConfigurationS1, Configuration subConfigurationS2) {
                if (subConfigurationS1.isEmpty())
                    return HandlerResponse.NOANSWER;
                else
                    return HandlerResponse.COMPLETED;
            }
        };
        HandlerResponse myResponse = _executor.execute(action, sender);
        assertEquals(HandlerResponse.NOANSWER, myResponse);
        // 2 responses, first completed but the second is no answer. overall NOANSWER
        assertEquals(2, sender.getCallsCounter());
    }

    /**
     * This test the case with immediate COMPLETE single
     */
    @Test
    public void testCommandCompletedSingle() {
        _executor = new ApplySenderExecutor(builder, actionManager, handlers);

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .build();

        Command command = new Command(
                SequenceCommand.APPLY,
                Activity.START,
                _applyConfig);

        Action action = new Action(command, new CompletionListenerMock());

        List<ConfigPath> registeredHandlers = ImmutableList.of(configPath("X:S1"), configPath("X:S2"));
        when(handlers.getApplyHandlers()).thenReturn(registeredHandlers);

        ActionSenderMock sender = new ActionSenderMock() {
            @Override
            public HandlerResponse responseFor(Configuration subConfigurationS1, Configuration subConfigurationS2) {
                return HandlerResponse.COMPLETED;
            }
        };
        HandlerResponse myResponse = _executor.execute(action, sender);
        assertEquals(HandlerResponse.COMPLETED, myResponse);
        // 1 responses. overall COMPLETED
        assertEquals(1, sender.getCallsCounter());
    }

    /**
     * This test the case with immediate COMPLETION
     */
    @Test
    public void testCommandCompletedBoth() {
        _executor = new ApplySenderExecutor(builder, actionManager, handlers);

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:S1:A.val1"), "xa1")
                .withPath(configPath("X:S2:C.val1"), "xc1")
                .build();

        Command command = new Command(
                SequenceCommand.APPLY,
                Activity.START,
                _applyConfig);

        Action action = new Action(command, new CompletionListenerMock());

        List<ConfigPath> registeredHandlers = ImmutableList.of(configPath("X:S1"), configPath("X:S2"));
        when(handlers.getApplyHandlers()).thenReturn(registeredHandlers);

        ActionSenderMock sender = new ActionSenderMock() {
            @Override
            public HandlerResponse responseFor(Configuration subConfigurationS1, Configuration subConfigurationS2) {
                return HandlerResponse.COMPLETED;
            }
        };
        HandlerResponse myResponse = _executor.execute(action, sender);
        assertEquals(HandlerResponse.COMPLETED, myResponse);
        // 2 responses. overall COMPLETED
        assertEquals(2, sender.getCallsCounter());
    }
}