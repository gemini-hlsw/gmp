package edu.gemini.aspen.gmp.commands.model.impl;

import edu.gemini.aspen.giapi.commands.HandlerResponse;
import edu.gemini.aspen.gmp.commands.model.Action;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * This class will monitor the responses received by an action. For the APPLY
 * sequence command, it is possible to have multiple handlers on the instrument
 * that will contribute to answer the command request.
 *
 * This class will keep track of the received answers and then it will be
 * able to provide a consolidated answer once all the responses are received. The
 * class knows how many responses are needed in order to declare a command request
 * as answered.
 */
class HandlerResponseTracker {

    private static final Logger LOG = Logger.getLogger(HandlerResponseTracker.class.getName());

    public static final String ERROR_MSG =  "The configuration has not been resolved yet. %d pending response%s";

    /**
     * This is a container data that is capable of collecting intermediate
     * responses and knows how many we have to have in order to produce a result.
     */
    private class ResponseHolder {
        /**
         * Keeps track of the received answers and can produce a summary out of them.
         */
        HandlerResponseAnalyzer analyzer = new HandlerResponseAnalyzer();

        private AtomicInteger pendingResponses = new AtomicInteger(); //how many responses are required to complete the request

        /**
         * Adds a response to the list.
         * @param response new response to be considered in the final result
         */
        public void addResponse(HandlerResponse response) {
            analyzer.addResponse(response);
            pendingResponses.decrementAndGet();
        }

        /**
         * Returns <code>true</code> if there are no more pending answers for this
         * action.
         * @return <code>true</code> if there are no more pending answers for this
         * action, <code>false</code> otherwise.
         */
        public boolean hasNoPendingResponses() {
            return pendingResponses.get() <= 0;   //note the <=0. This is because for actions other
            //than APPLY, the pending response will be <0 once we received their answers 
        }

        /**
         * Returns a summary of the results collected.
         * @return Summary of the results collected or an error response in
         * case the required ammount of responses have not been received.
         */
        public HandlerResponse getResponse() {
            if (hasNoPendingResponses()) {
                return analyzer.getSummaryResponse();
            }
            else {
                int pending = pendingResponses.get();
                return HandlerResponse.createError(
                        String.format(ERROR_MSG, pending, pending > 1 ? "s":""));
            }
        }

        /**
         * Increments in one the number of responses needed.
         */
        public void addPendingResponse() {
            pendingResponses.incrementAndGet();
        }

        /**
         * Increments in one the number of responses needed.
         */
        public void removePendingResponse() {
            pendingResponses.decrementAndGet();
        }
    }

    /**
     * Map each action to a structure that keeps track of the responses obtained for that
     * action
     */
    private final Map<Action, ResponseHolder> _actionResponsesMap =
            Collections.synchronizedMap(new HashMap<Action, ResponseHolder>());

    /**
     * Store the given response for the action.
     * @param action the action that triggered the response.
     * @param response response obtained for the given action
     */
    public void storeResponse(Action action, HandlerResponse response) {
        ResponseHolder responseHolder = getResponseHolder(action);
        responseHolder.addResponse(response);
    }

    /**
     * Returns <code>true</code> if all the responses required for the given action have
     * been received
     * @param action Action that is being evaluated for completeness
     * @return <code>true</code> if all the responses required for the given action have
     * been received, or <code>false</code> if there are pending responses to be
     * received for this action
     */
    public boolean isComplete(Action action) {
        ResponseHolder responseHolder = _actionResponsesMap.get(action);
        return responseHolder == null || responseHolder.hasNoPendingResponses();
    }


    /**
     * Get the response for the given action, if it has been completed
     * @param action the action to get the response to
     * @return the response associated to the given action, if available.
     * If the responses are not available yet, an error will be returned.
     * If the action is not being tracked, it will return <code>null</code>
     */
    public HandlerResponse getResponse(Action action) {

        ResponseHolder responseHolder = _actionResponsesMap.get(action);
        if (responseHolder != null) {
            return responseHolder.getResponse();
        }
        LOG.warning("We are not tracking progress for action " + action);
        return null;
    }

    /**
     * Increase by one the required number of responses necessary to declare the
     * action as finished.
     * @param a the action for which we will increase the required number of
     * responses
     */
    public void increaseRequiredResponses(Action a) {
        ResponseHolder responseHolder = getResponseHolder(a);
        responseHolder.addPendingResponse();
    }

    public void decreaseRequiredResponses(Action a) {
        ResponseHolder responseHolder = getResponseHolder(a);
        responseHolder.removePendingResponse();
    }
    /**
     * Remove the action from the list of tracked actions.
     * @param a action to remove
     */
    public void removeTrackedAction(Action a) {
        _actionResponsesMap.remove(a);
    }


    /**
     * An auxiliary method to get the internal storage structure
     * associated to a particular action. A new storage structure
     * is created if it is not available.
     * @param a the action for whose ResponseHolder will be obtained
     * @return the ResponseHolder for the given action.
     */
    private ResponseHolder getResponseHolder(Action a) {
        ResponseHolder responseHolder;
        if (_actionResponsesMap.containsKey(a)) {
            responseHolder = _actionResponsesMap.get(a);
        } else {
            responseHolder = new ResponseHolder();
            _actionResponsesMap.put(a, responseHolder);
        }
        return responseHolder;
    }
}
