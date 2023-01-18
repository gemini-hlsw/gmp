package edu.gemini.aspen.gmp.commands.model.impl;

import edu.gemini.aspen.giapi.commands.HandlerResponse;
import edu.gemini.aspen.gmp.commands.model.Action;

/**
 * ActionManager defines an internal interface to a service that keeps track
 * of the set of submitted action and handles the logic of keeping track of their IDs
 *
 * This service shouldn't be called from outside this bundle
 */
public interface ActionManager {
    /**
     * Register that there is a future {@link edu.gemini.aspen.giapi.commands.HandlerResponse}
     * to be received for the given APPLY action. If the action is not an APPLY, this method does nothing.
     *
     * @param action the APPLY action that will get a future response later.
     */
    void increaseRequiredResponses(Action action);

    /**
     * Remove a required response, this is used when a sub action completes right away
     *
     * @param action the APPLY action that will get a future response later.
     */
    void decreaseRequiredResponses(Action action);

    /**
     * Indicates if the action has completed
     */
    boolean isComplete(Action action);

    /**
     * Register this Action to keep track its progress internally. When
     * the completion information associated to this action is available, the
     * listener contained in it will be invoked.
     *
     * @param action the action to register
     */
    void registerAction(Action action);

    /**
     * Deregister the given action from the monitored actions. This
     * is done so the system don't attempt to update with completion
     * information those actions that complete immediately.
     * @param action the action to deregister.
     */
    void unregisterAction(Action action);

    /**
     * Register the completion information to be sent to the clients identified
     * by the given action Id. This information is queued and used by the
     * processing thread to notify the clients.
     *
     * @param actionId the action Id used to identify the clients waiting for
     *                 completion feedback
     * @param response the completion information to be sent to the clients.
     */

    void registerCompletionInformation(int actionId, HandlerResponse response);

    /**
     * Acquire the updater processor lock. This way, the update processor
     * can't notify handler until the lock is released
     */
    void lock();

    /**
     * Release the lock of the update processor thread. The update
     * processor thread will resume execution as soon as reacquires the
     * lock.
     */
    void unlock();
}
