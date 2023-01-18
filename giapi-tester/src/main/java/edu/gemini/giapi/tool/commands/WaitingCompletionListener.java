package edu.gemini.giapi.tool.commands;

import edu.gemini.aspen.giapi.commands.Command;
import edu.gemini.aspen.giapi.commands.CompletionInformation;
import edu.gemini.aspen.giapi.commands.CompletionListener;
import edu.gemini.aspen.giapi.commands.HandlerResponse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class that implements CompletionListener and that can wait for a given
 * time for a response, otherwise it returns an Error.
 * <br>
 * Note that this listener can only accept one response, attempts to use
 * again will throw an IllegalStateException
 */
public class WaitingCompletionListener implements CompletionListener {
    private final AtomicReference<CompletionInformation> response = new AtomicReference<CompletionInformation>();
    private final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void onHandlerResponse(HandlerResponse response, Command command) {
        this.response.set(new CompletionInformation(response, command));
        latch.countDown();
    }

    /**
     * Waits for a response arrived as a message to onHandlerResponse for a maximum timeout
     *
     * @param timeout Max time to wait in milliseconds
     * @return the Response Arrived
     */
    public CompletionInformation waitForResponse(long timeout) {
        try {
            latch.await(timeout, TimeUnit.MILLISECONDS);
            if (response.get() != null) {
                return response.get();
            }
        } catch (InterruptedException e) {
            // Ignore, this means we should return an error
        }
        return new CompletionInformation(HandlerResponse.createError("Response not arrived in the timeout of " + timeout + " [ms]"), Command.noCommand());
    }

}
