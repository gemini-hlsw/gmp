package edu.gemini.giapi.tool.commands;

import edu.gemini.aspen.giapi.commands.Activity;
import edu.gemini.aspen.giapi.commands.Command;
import edu.gemini.aspen.giapi.commands.CompletionListener;
import edu.gemini.aspen.giapi.commands.Configuration;
import edu.gemini.aspen.giapi.commands.HandlerResponse;
import edu.gemini.aspen.giapi.commands.SequenceCommand;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class that implements CompletionListener and that can wait for a given
 * time for a response, otherwise it returns an Error.
 * <p/>
 * Note that this listener can only accept one response, attempts to use
 * again will throw an IllegalStateException
 */
public class WaitingCompletionListener implements CompletionListener {
    private final AtomicReference<HandlerResponse> response = new AtomicReference<HandlerResponse>();
    private final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void onHandlerResponse(HandlerResponse response, Command command) {
        this.response.set(response);
        latch.countDown();
    }

    @Override
    public void onHandlerResponse(HandlerResponse response, SequenceCommand command, Activity activity, Configuration config) {
        throw new UnsupportedOperationException("");
    }

    /**
     * Waits for a response arrived as a message to onHandlerResponse for a maximum timeout
     *
     * @param timeout Max time to wait in milliseconds
     * @return the Response Arrived
     */
    public HandlerResponse waitForResponse(long timeout) {
        try {
            latch.await(timeout, TimeUnit.MILLISECONDS);
            if (response.get() != null) {
                return response.get();
            }
        } catch (InterruptedException e) {
            // Ignore, this means we should return an error
        }
        return HandlerResponse.createError("Response not arrived in time: " + timeout);
    }
}
