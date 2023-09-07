package edu.gemini.giapi.tool.arguments;


import edu.gemini.giapi.tool.parser.AbstractArgument;
import edu.gemini.giapi.tool.parser.Util;

/**
 * Argument to get a Wait time
 */
public class WaitArgument extends AbstractArgument {

    private int _wait;

    public WaitArgument() {
        super("wait");
    }

    public boolean requireParameter() {
        return true;
    }

    public void parseParameter(String arg) {
        try {
            // Parse the second argument as an integer
            _wait = Integer.parseInt(arg);

            // Check if the value is positive
            if (_wait <= 0) {
                Util.die("Wait time must be a positive number");
            }
        } catch (NumberFormatException ex) {
            Util.die("Invalid wait time format. Try -wait <wait time in seconds>");
        }
    }

    public String getInvalidArgumentMsg() {
        return "Invalid wait argument. Try -wait <wait time in seconds>";
    }

    public int getWaitTime() {
        return _wait;
    }

}
