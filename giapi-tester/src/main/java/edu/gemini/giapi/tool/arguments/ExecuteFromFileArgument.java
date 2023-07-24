package edu.gemini.giapi.tool.arguments;

import edu.gemini.giapi.tool.parser.Argument;
import edu.gemini.giapi.tool.parser.util.AbstractArgument;

/**
 * Argument to specify a file containing commands to execute.
 */
public class ExecuteFromFileArgument extends AbstractArgument {

    private String fileName;

    public ExecuteFromFileArgument() {
        super("file");
    }

    public boolean requireParameter() {
        return true;
    }

    public void parseParameter(String arg) {
        fileName = arg;
    }

    public String getFileName() {
        return fileName;
    }

    public String getInvalidArgumentMsg() {
        return "You must specify a filename after the -file argument";
    }
}
