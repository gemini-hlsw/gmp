package edu.gemini.aspen.gmp.commands.model.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.gemini.aspen.giapi.commands.Activity;
import edu.gemini.aspen.giapi.commands.Command;
import edu.gemini.aspen.giapi.commands.CommandSender;
import edu.gemini.aspen.giapi.commands.HandlerResponse;
import edu.gemini.aspen.giapi.commands.SequenceCommand;
import edu.gemini.aspen.giapitestsupport.commands.CompletionListenerMock;
import edu.gemini.aspen.gmp.commands.model.Action;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static edu.gemini.aspen.giapi.commands.DefaultConfiguration.emptyConfiguration;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test suite for the Action Manager class.
 */
public class ActionManagerImplTest {
    private static final int TOTAL_ACTIONS = 10;
    private static final int TIMEOUT_FOR_RESPONSE = 1000;
    private static final int TIMEOUT_FOR_NO_RESPONSE = 200;

    private ActionManagerImpl manager;
    private List<Action> actions;
    private Map<Action, CompletionListenerMock> completionListeners = Maps.newHashMap();

    @Before
    public void setUp() {
        manager = new ActionManagerImpl();
        manager.start();
        actions = Lists.newArrayList();

        for (int i = 0; i < TOTAL_ACTIONS; i++) {
            CompletionListenerMock listener = new CompletionListenerMock();
            Action action = new Action(
                    new Command(SequenceCommand.ABORT, Activity.PRESET, emptyConfiguration()),
                    listener);

            completionListeners.put(action, listener);
            actions.add(action);
        }
    }

    @After
    public void tearDown() {
        manager.stop();
    }

    /**
     * Basic test. One action being monitored, and completion info.
     * received for that action
     */
    @Test
    public void testOneActionOneCompletion() {
        Action action = actions.get(0);

        manager.registerAction(action);
        manager.registerCompletionInformation(action.getId(),
                HandlerResponse.COMPLETED);

        CompletionListenerMock cl = completionListeners.get(action);

        cl.waitForCompletion(TIMEOUT_FOR_RESPONSE);
        assertTrue(cl.wasInvoked());
    }

    /**
     * This test will verify the locking mechanism in the manager works
     */
    @Test
    public void testLock() {
        Action action = actions.get(0);

        manager.registerAction(action);

        //lock the manager, so it should not update the listener...
        manager.lock();
        manager.registerCompletionInformation(action.getId(),
                HandlerResponse.COMPLETED);

        CompletionListenerMock cl = completionListeners.get(action);

        cl.waitForCompletion(TIMEOUT_FOR_NO_RESPONSE);
        assertFalse(cl.wasInvoked());

        //unlock the manager
        manager.unlock();

        // Now it should be invoked
        cl.waitForCompletion(TIMEOUT_FOR_RESPONSE);
        assertTrue(cl.wasInvoked());
    }

    /**
     * Validates that completion info for an action will trigger the listeners
     */
    @Test
    public void testMultiplePendingActions() {
        for (Action a : actions) {
            manager.registerAction(a);
        }

       // Set all actions bar last complete
        for (int i = 0; i < TOTAL_ACTIONS - 1; i++) {
           manager.registerCompletionInformation(actions.get(i).getId(),  
                   HandlerResponse.COMPLETED);
       }
       
        //and give the listeners a chance to run...
        for (Action a : actions) {
            CompletionListenerMock cl = completionListeners.get(a);
            cl.waitForCompletion(TIMEOUT_FOR_RESPONSE);
        }

        for (int i = 0; i < TOTAL_ACTIONS - 1; i++) {
            CompletionListenerMock cl = completionListeners.get(actions.get(i));
            assertTrue(cl.wasInvoked());
        }

        CompletionListenerMock cl = completionListeners.get(actions.get(TOTAL_ACTIONS - 1));
        assertFalse(cl.wasInvoked());

    }

    /**
     * Test the correct behavior of receiving info about an action that
     * is not being tracked.
     */
    @Test
    public void testInvalidAction() {
        Action action = actions.get(0);

        manager.registerCompletionInformation(action.getId(),
                HandlerResponse.COMPLETED);

        CompletionListenerMock cl = completionListeners.get(action);
        cl.waitForCompletion(TIMEOUT_FOR_NO_RESPONSE);
        assertFalse(cl.wasInvoked());
    }

    /**
     * Test case to make sure if completion information is received more than once,
     * it won't trigger the completion listener twice
     */
    @Test
    public void testDuplicatedAction() {
        Action action = actions.get(0);

        manager.registerAction(action);

        manager.registerCompletionInformation(action.getId(),
                HandlerResponse.COMPLETED);

        CompletionListenerMock cl = completionListeners.get(action);
        cl.waitForCompletion(TIMEOUT_FOR_RESPONSE);
        assertTrue(cl.wasInvoked());

        //now, receive completion again...
        cl.reset();

        manager.registerCompletionInformation(action.getId(),
                HandlerResponse.COMPLETED);
        
        cl.waitForCompletion(TIMEOUT_FOR_NO_RESPONSE);
        assertFalse(cl.wasInvoked());
    }


    /**
     * Test case for actions that are not yet produced by the system.
     * It shouldn't invoke any handlers since this is a problem in the
     * code.
     */
    @Test
    public void testFutureAction() {
        Action action = actions.get(0);

        manager.registerAction(action);

        //receive completion info for an ID we haven't produced..
        manager.registerCompletionInformation(actions.get(TOTAL_ACTIONS - 1).getId() + 1,
                HandlerResponse.COMPLETED);

        //see if this triggers action #1.
        CompletionListenerMock cl = completionListeners.get(action);
        cl.waitForCompletion(TIMEOUT_FOR_NO_RESPONSE);
        //it shouldn't have been called.
        assertFalse(cl.wasInvoked());
    }

    /**
     * This test verifies the handling of completion information
     * for the APPLY sequence command, which is slightly different
     * from all the other sequence commands in the sense it
     * can be handled by multiple handler, hence the action manager
     * can receive multiple responses. This test verifies the simpler
     * case when only one handler replies.
     */
    @Test
    public void testCompletionForApplySequenceCommand() {
        CompletionListenerMock cl = new CompletionListenerMock();

        Action action = new Action(new Command(SequenceCommand.APPLY,
                Activity.PRESET_START,
                emptyConfiguration()),
                cl);

        manager.registerAction(action);

        //An action started has been registered.
        manager.increaseRequiredResponses(action);

        manager.registerCompletionInformation(action.getId(),
                HandlerResponse.COMPLETED);

        //the completion listener should have been called.
        cl.waitForCompletion(TIMEOUT_FOR_RESPONSE);
        assertTrue(cl.wasInvoked());
    }

    /**
     * Test the handling of multiple completion information for the same
     * action ID. This happens for the APPLY sequence command. If an
     * APPLY configuration is handled by multiple handlers, then
     * the configuration will be split and the several handlers
     * will reply completion information to the SAME action ID.
     */
    @Test
    public void testMultipleCompletionForApplySequenceCommand() {
        CompletionListenerMock cl = new CompletionListenerMock();

        Action action = new Action(new Command(SequenceCommand.APPLY,
                Activity.PRESET_START,
                emptyConfiguration()),
                cl);

        manager.registerAction(action);

        //two handlers will be expected..
        manager.increaseRequiredResponses(action);
        manager.increaseRequiredResponses(action);

        cl.reset();
        manager.registerCompletionInformation(action.getId(),
                HandlerResponse.COMPLETED);

        //the completion listener should not have been called.
        cl.waitForCompletion(TIMEOUT_FOR_NO_RESPONSE);
        assertFalse(cl.wasInvoked());

        cl.reset();

        // Now it should be called
        manager.registerCompletionInformation(action.getId(),
                HandlerResponse.COMPLETED);

        cl.waitForCompletion(TIMEOUT_FOR_RESPONSE);
        assertTrue(cl.wasInvoked());
    }

    @Test
    public void testReceptionOfUnexpectedActionId() {

        CompletionListenerMock cl = new CompletionListenerMock();

        Action fakedAction = new Action(Action.getCurrentId() + 1,
                new Command(SequenceCommand.APPLY,
                        Activity.PRESET_START,
                        emptyConfiguration()),
                cl, CommandSender.DEFAULT_COMMAND_RESPONSE_TIMEOUT);

        manager.registerAction(fakedAction);
        //An action started has been registered.
        manager.increaseRequiredResponses(fakedAction);

        manager.registerCompletionInformation(fakedAction.getId(),
                HandlerResponse.COMPLETED);

        //the completion listener should not have been called.
        //a log message should appear in the logs saying that an unexpected action was received.
        cl.waitForCompletion(TIMEOUT_FOR_NO_RESPONSE);
        assertFalse(cl.wasInvoked());
    }

    @Test
    public void testReceptionOfUnexpectedActionIdLowerThanLastOneProcessed() {
        CompletionListenerMock cl = new CompletionListenerMock();

        Action fakedAction = new Action(18,
                new Command(SequenceCommand.GUIDE,
                        Activity.PRESET_START,
                        emptyConfiguration()),
                cl, CommandSender.DEFAULT_COMMAND_RESPONSE_TIMEOUT);
        //register this action with the manager
        manager.registerAction(fakedAction);

        //and let's fake the reception of an unexpected action ID, lower than the
        //last registered (18)
        manager.registerCompletionInformation(5, HandlerResponse.COMPLETED);

        //the completion listener should not have been called
        //a log message should appear in the logs saying that an unexpected action was received.
        cl.waitForCompletion(TIMEOUT_FOR_NO_RESPONSE);
        assertFalse(cl.wasInvoked());
    }

    @Test
    public void testReceptionOfErrorAndCompleteInParallel() {
        CompletionListenerMock clA = new CompletionListenerMock();

        int idA = 1;
        Action commandA = new Action(idA,
                new Command(SequenceCommand.GUIDE,
                        Activity.PRESET_START,
                        emptyConfiguration()),
                clA, CommandSender.DEFAULT_COMMAND_RESPONSE_TIMEOUT);
        //register this action with the manager
        manager.registerAction(commandA);

        CompletionListenerMock clB = new CompletionListenerMock();

        int idB = 2;
        Action commandB = new Action(idB,
                new Command(SequenceCommand.TEST,
                        Activity.PRESET_START,
                        emptyConfiguration()),
                clB, CommandSender.DEFAULT_COMMAND_RESPONSE_TIMEOUT);
        //register this action with the manager
        manager.registerAction(commandB);

        //command A fails
        manager.registerCompletionInformation(idA, HandlerResponse.createError("Some error"));
        //But command B is fine
        manager.registerCompletionInformation(idB, HandlerResponse.COMPLETED);

        //the command A gets an error
        clA.waitForCompletion(TIMEOUT_FOR_NO_RESPONSE);
        assertTrue(clA.wasInvoked());
        assertTrue(clA.getLastResponse().hasErrorMessage());

        //And command B should be Ok
        clB.waitForCompletion(TIMEOUT_FOR_NO_RESPONSE);
        assertTrue(clB.wasInvoked());
        assertFalse(clB.getLastResponse().hasErrorMessage());
    }

    @Test
    @Ignore
    public void testReceptionOfErrorAndCompleteInParallelError2() {
        CompletionListenerMock clA = new CompletionListenerMock();

        int idA = 1;
        Action commandA = new Action(idA,
                new Command(SequenceCommand.GUIDE,
                        Activity.PRESET_START,
                        emptyConfiguration()),
                clA, CommandSender.DEFAULT_COMMAND_RESPONSE_TIMEOUT);
        //register this action with the manager
        manager.registerAction(commandA);

        CompletionListenerMock clB = new CompletionListenerMock();

        int idB = 2;
        Action commandB = new Action(idB,
                new Command(SequenceCommand.TEST,
                        Activity.PRESET_START,
                        emptyConfiguration()),
                clB, CommandSender.DEFAULT_COMMAND_RESPONSE_TIMEOUT);
        //register this action with the manager
        manager.registerAction(commandB);

        //command B fails
        manager.registerCompletionInformation(idB, HandlerResponse.createError("Some error"));
        //But command A is fine
        manager.registerCompletionInformation(idA, HandlerResponse.COMPLETED);

        //the command B gets an error
        clB.waitForCompletion(TIMEOUT_FOR_NO_RESPONSE);
        assertTrue(clB.wasInvoked());
        assertTrue(clB.getLastResponse().hasErrorMessage());

        //And command A should be Ok
        clA.waitForCompletion(TIMEOUT_FOR_NO_RESPONSE);
        assertTrue(clA.wasInvoked());
        assertFalse(clA.getLastResponse().hasErrorMessage());
    }
}
