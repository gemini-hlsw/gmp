package edu.gemini.giapi.tool.fileevents;

import edu.gemini.giapi.tool.parser.Operation;
import edu.gemini.giapi.tool.parser.Argument;
import edu.gemini.giapi.tool.GiapiTester;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Execute commands from a file.
 */
public class ExecuteFromFileOperation implements Operation {

    private boolean fileRequested = false;
    private String fileName;

    @Override
    public void setArgument(Argument arg) {
        if (arg instanceof ExecuteFromFileArgument) {
            fileRequested = true;
            ExecuteFromFileArgument fileArg = (ExecuteFromFileArgument) arg;
            fileName = fileArg.getFileName();
        }
    }

    @Override
    public boolean isReady() {
        return fileRequested;
    }

    @Override
    public int execute() throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Split the line into an array of arguments and execute them
                String[] lineArray = line.split("\\s+");
                GiapiTester.executeArgs(lineArray);
            }
        } catch (IOException e) {
            System.err.println("Failed to read file: " + e.getMessage());
            return 1;
        }
        return 0;
    }
}
