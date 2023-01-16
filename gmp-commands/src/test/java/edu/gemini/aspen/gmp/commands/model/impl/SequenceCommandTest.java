package edu.gemini.aspen.gmp.commands.model.impl;

import edu.gemini.aspen.giapi.commands.Activity;
import edu.gemini.aspen.giapi.commands.Command;
import edu.gemini.aspen.giapi.commands.CommandSender;
import edu.gemini.aspen.giapi.commands.CommandUpdater;
import edu.gemini.aspen.giapi.commands.HandlerResponse;
import edu.gemini.aspen.giapi.commands.SequenceCommand;
import edu.gemini.aspen.gmp.commands.model.Action;
import edu.gemini.aspen.gmp.commands.model.ActionSender;
import edu.gemini.aspen.gmp.commands.test.ActionSenderMock;
import edu.gemini.aspen.giapitestsupport.commands.CompletionListenerMock;
import edu.gemini.aspen.gmp.commands.test.SequenceCommandExecutorMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * A Class to test Sequence Commands. It will exercise
 * the Command Sender and Command Updater implementations
 */
public class SequenceCommandTest {
    private CommandUpdater commandUpdater;

    private ActionManagerImpl actionManager;

    private SequenceCommandExecutorMock executor; //a simplified executor, that don't take into account the type of action.

    private final CompletionListenerMock completionListener = new CompletionListenerMock();

    @Before
    public void setUp() {
        actionManager = new ActionManagerImpl();
        actionManager.start();

        commandUpdater = new CommandUpdaterImpl(actionManager);

        executor = new SequenceCommandExecutorMock(commandUpdater, completionListener);

        completionListener.reset();
    }

    @After
    public void tearDown() {
        actionManager.stop();
    }

    /**
     * Test of a command that completes immediately, so the answer
     * returned to the client is COMPLETED, ERROR or ACCEPTED.
     */
    @Test
    public void testImmediateCommand() throws InterruptedException {

        HandlerResponse[] answers = new HandlerResponse[]{
                HandlerResponse.COMPLETED,
                HandlerResponse.createError(""),
                HandlerResponse.ACCEPTED,
        };

        for (HandlerResponse r : answers) {
            ActionSender sender = new ActionSenderMock(r);
            CommandSender commandSender = new CommandSenderImpl(actionManager, sender, executor);

            HandlerResponse response = commandSender.sendCommand(new Command(
                    SequenceCommand.ABORT,
                    Activity.PRESET),
                    completionListener
            );

            assertEquals(r, response);

            //let's fake completion information...(we shouldn't receive this, since
            //the action completed immediately). If we do, there is something
            //wrong in the instrument code since it's  sending completion
            //info for actions that did not "STARTED".
            //The code will generate WARNING Messages, but that's okay
            HandlerResponse completedResponse = HandlerResponse.COMPLETED;
            commandUpdater.updateOcs(Action.getCurrentId(), completedResponse);

            //now, make sure the completion listener is not triggered, since
            //this action completed immediately.
            completionListener.waitForCompletion(1000L);

            //the listener should not have been invoked
            assertFalse(completionListener.wasInvoked());
        }

    }

    /**
     * Test a command that takes time to complete. So the first answer is
     * STARTED and then we receive completion information as "COMPLETED".
     */
    @Test
    public void testLongCommand() throws InterruptedException {
        ActionSender sender = new ActionSenderMock(HandlerResponse.STARTED);
        CommandSender cs = new CommandSenderImpl(actionManager, sender, executor);
        HandlerResponse response = cs.sendCommand(new Command(
                SequenceCommand.ABORT,
                Activity.START),
                completionListener
        );

        assertEquals(HandlerResponse.STARTED, response);

        HandlerResponse r1 = HandlerResponse.COMPLETED;
        commandUpdater.updateOcs(Action.getCurrentId(), r1);

        completionListener.waitForCompletion(1000L);

        assertTrue(completionListener.wasInvoked());
        assertEquals(r1, completionListener.getLastResponse());
    }

    /**
     * Test a command that is not process and thus it timeouts
     */
    @Test
    @Ignore
    public void testCommandWithTimeout() throws InterruptedException {
        ActionSender sender = new ActionSenderMock(HandlerResponse.STARTED);
        actionManager.stop();
        CommandSender cs = new CommandSenderImpl(actionManager, sender, executor);
        HandlerResponse response = cs.sendCommand(new Command(
                SequenceCommand.ABORT,
                Activity.START),
                completionListener,
                3000L
        );

        assertEquals(HandlerResponse.createError("Timeout"), response);
    }

    /**
     * This case test a command whose associated handler finishes faster than
     * the code that receives the request... so we get "Completed" for instance,
     * for an action we have not quite yet send the "Started".
     */
    @Test
    @Ignore
    public void testFasterHandler() {

        //the answer to the command will be Started
        HandlerResponse r = HandlerResponse.STARTED;
        ActionSender sender = new ActionSenderMock(r);
        CommandSender cs = new CommandSenderImpl(actionManager, sender, executor);

        //let's configure the ActionSender to reply completion information

        //we will simulate the situation when a fast handler returns first...
        HandlerResponse r1 = HandlerResponse.COMPLETED;

        //configure the executor in the command sender to send completion
        //information BEFORE answering the command.
        executor.simulateFastHandler(r1);

        //This will send the command, which normally would have
        //returned "STARTED". However, since we receive first a "COMPLETED"
        //due to the handler, internally the command sender has to
        //consider that and return the final answer.
        HandlerResponse response = cs.sendCommand(new Command(
                SequenceCommand.DATUM,
                Activity.START),
                completionListener
        );

        //so the answer should be the final one.
        assertEquals(r1, response);
    }


}
