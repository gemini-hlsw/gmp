package edu.gemini.aspen.gmp.commands.model.executors;

import edu.gemini.aspen.giapi.commands.*;
import edu.gemini.aspen.giapitestsupport.commands.CompletionListenerMock;
import edu.gemini.aspen.gmp.commands.handlers.CommandHandlers;
import edu.gemini.aspen.gmp.commands.model.Action;
import edu.gemini.aspen.gmp.commands.model.ActionMessageBuilder;
import edu.gemini.aspen.gmp.commands.model.ActionSender;
import edu.gemini.aspen.gmp.commands.model.impl.ActionManagerImpl;
import edu.gemini.aspen.gmp.commands.test.ActionSenderMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static edu.gemini.aspen.giapi.commands.ConfigPath.configPath;
import static edu.gemini.aspen.giapi.commands.DefaultConfiguration.configurationBuilder;
import static edu.gemini.aspen.giapi.commands.DefaultConfiguration.emptyConfiguration;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;


/**
 * Test class for the sender of APPLY sequence commands.
 */
public class ApplySenderExecutorTest {

    private ApplySenderExecutor _executor;

    private HandlerResponse[] _responses;

    private Configuration _applyConfig;
    private ActionManagerImpl actionManager;

    @Before
    public void setUp() {
        ActionMessageBuilder builder = mock(ActionMessageBuilder.class);
        CommandHandlers handlers = mock(CommandHandlers.class);

        actionManager = new ActionManagerImpl();
        actionManager.start();

        _executor = new ApplySenderExecutor(builder, actionManager, handlers);

        _responses = new HandlerResponse[]{
                HandlerResponse.COMPLETED,
                HandlerResponse.STARTED,
                HandlerResponse.ACCEPTED,
                HandlerResponse.NOANSWER,
                HandlerResponse.createError("Error message")
        };

        _applyConfig = configurationBuilder()
                .withPath(configPath("X:A.val1"), "xa1")
                .withPath(configPath("X:A.val2"), "xa2")
                .withPath(configPath("X:A.val2"), "xa2")
                .withPath(configPath("X:A.val3"), "xa3")
                .withPath(configPath("X:B.val1"), "xb1")
                .withPath(configPath("X:B.val1"), "xb1")
                .withPath(configPath("X:B.val2"), "xb2")
                .withPath(configPath("X:B.val3"), "xb3")
                .withPath(configPath("X:C.val1"), "xc1")
                .withPath(configPath("X:C.val2"), "xc2")
                .withPath(configPath("X:C.val3"), "xc3")
                .build();
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
        assertEquals(1, _executor.countExpectedResponses(_applyConfig, ConfigPath.EMPTY_PATH));
    }

    /**
     * Test that an APPLY command without configuration will produce an ERROR response
     */
    @Test
    public void testEmptyConfiguration() {
        Action action = new Action(new Command(SequenceCommand.APPLY,
                Activity.START,
                emptyConfiguration()), new CompletionListenerMock());
        ActionSender sender = new ActionSenderMock(HandlerResponse.ACCEPTED);
        HandlerResponse response = _executor.execute(action, sender);
        assertEquals(HandlerResponse.createError("No configuration present for Apply Sequence command"), response);
    }

    /**
     * Test that an APPLY command with a non hierarchical configuration will be handled correctly
     */
    @Test
    public void testSimpleConfiguration() {
        Configuration basicConfiguration = configurationBuilder()
                .withConfiguration("X", "1")
                .build();

        Action action = new Action(new Command(SequenceCommand.APPLY,
                Activity.START,
                basicConfiguration), new CompletionListenerMock());
        ActionSender sender = new ActionSenderMock(HandlerResponse.NOANSWER);
        HandlerResponse response = _executor.execute(action, sender);
        assertEquals(HandlerResponse.get(HandlerResponse.Response.NOANSWER), response);
    }
}