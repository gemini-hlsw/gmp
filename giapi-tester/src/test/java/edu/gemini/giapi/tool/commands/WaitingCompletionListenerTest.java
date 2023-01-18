package edu.gemini.giapi.tool.commands;

import edu.gemini.aspen.giapi.commands.Activity;
import edu.gemini.aspen.giapi.commands.Command;
import edu.gemini.aspen.giapi.commands.CompletionInformation;
import edu.gemini.aspen.giapi.commands.HandlerResponse;
import edu.gemini.aspen.giapi.commands.SequenceCommand;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WaitingCompletionListenerTest {
    private static final int TIMEOUT = 1000;

    @Test
    public void testWaitForResponse() {
        WaitingCompletionListener completionListener = new WaitingCompletionListener();

        long start = System.currentTimeMillis();
        // No responses will arrive in this case
        CompletionInformation completionInformation = completionListener.waitForResponse(TIMEOUT);

        assertEquals(HandlerResponse.createError("Response not arrived in the timeout of " + TIMEOUT + " [ms]"), completionInformation.getHandlerResponse());
        assertTrue((System.currentTimeMillis() - start) >= TIMEOUT);
    }

    @Test
    public void testWaitForRealResponse() {
        WaitingCompletionListener completionListener = new WaitingCompletionListener();

        long start = System.currentTimeMillis();
        // Fake the CommandSender that sends a message
        Command command = new Command(SequenceCommand.PARK, Activity.START);
        HandlerResponse response = HandlerResponse.get(HandlerResponse.Response.COMPLETED);

        completionListener.onHandlerResponse(response, command);
        // No responses will be sent in this case
        CompletionInformation completionInformation = completionListener.waitForResponse(TIMEOUT);

        assertEquals(HandlerResponse.get(HandlerResponse.Response.COMPLETED), completionInformation.getHandlerResponse());

        assertTrue((System.currentTimeMillis() - start) < TIMEOUT);
    }
}
