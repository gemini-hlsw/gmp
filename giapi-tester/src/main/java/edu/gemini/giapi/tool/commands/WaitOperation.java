package edu.gemini.giapi.tool.commands;

import edu.gemini.giapi.tool.arguments.WaitArgument;
import edu.gemini.giapi.tool.parser.Operation;
import edu.gemini.giapi.tool.parser.Argument;

/**
 * Perform a wait operation for a specified amount of time
 */
public class WaitOperation implements Operation {

    private boolean waitRequested = false;
    private int waitTime = 0; // Default wait time in seconds

    @Override
    public void setArgument(Argument arg) {
        if (arg instanceof WaitArgument) {
            waitRequested = true;
            WaitArgument waitArg = (WaitArgument) arg;
            waitTime = waitArg.getWaitTime();
        }
    }

    @Override
    public boolean isReady() {
        return waitRequested;
    }

    @Override
    public int execute() throws Exception {
        // Perform the wait operation here
        Thread.sleep(waitTime * 1000); // Convert waitTime to milliseconds

        System.out.println("Waited for " + waitTime + " seconds.");
        return 0;
    }
}
