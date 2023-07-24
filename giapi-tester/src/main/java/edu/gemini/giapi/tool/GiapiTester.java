package edu.gemini.giapi.tool;

import edu.gemini.giapi.tool.fileevents.MonitorFileEventsOperation;
import edu.gemini.giapi.tool.obsevents.SendObsEventOperation;
import edu.gemini.giapi.tool.parser.*;
import edu.gemini.giapi.tool.arguments.*;
import edu.gemini.giapi.tool.commands.*;
import edu.gemini.giapi.tool.help.HelpOperation;
import edu.gemini.giapi.tool.status.*;
import edu.gemini.giapi.tool.obsevents.MonitorObsEventOperation;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.logging.LogManager;


/**
 * The GIAPI Tester main class.
 */
public class GiapiTester {
    
    public static void main(String[] args) throws Exception {
        initializeLogging();

        //register the default message to show in case of problems
        Util.registerDefaultMessage("Let me help you. Try: java -jar giapi-tester.jar -?");


	    if (Arrays.asList(args).contains("-file")) {
		    // Find the index of the -file argument in the array
        	int index = Arrays.asList(args).indexOf("-file");

        	// Check if there is a filename after the -file argument
        	if (args.length > index + 1) {
            		String fileName = args[index + 1];
            		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            			String line;
            			while ((line = br.readLine()) != null) {
                			// Execute each line from the file
					        String[] lineArray = line.split("\\s+");
                			executeArgs(lineArray);
            			}
        		    } 
			        catch (IOException e) {
            			Util.die("Failed to read file");
        		    }
        	} 
		    else {
            		Util.die("You must specify a filename after the -file argument");
        	}
	    }
	    else{
		    executeArgs(args);
	    }

    }

    private static void executeArgs(String [] args) throws Exception {	

        //create the parser
        ArgumentParser parser = new ArgumentParser(args);

        //possible arguments
        parser.registerArgument(new SequenceCommandArgument());
        parser.registerArgument(new ActivityArgument());
        parser.registerArgument(new ConfigArgument());
        parser.registerArgument(new TimeoutArgument());
        parser.registerArgument(new HostArgument());
        parser.registerArgument(new HelpArgument());
        parser.registerArgument(new RepetitionArgument());
        parser.registerArgument(new MonitorStatusArgument());
        parser.registerArgument(new GetStatusArgument());
        parser.registerArgument(new SetStatusArgument());
        parser.registerArgument(new TypeArgument());
        parser.registerArgument(new ValueArgument());
        parser.registerArgument(new AlarmSeverityArgument());
        parser.registerArgument(new AlarmCauseArgument());
        parser.registerArgument(new AlarmMessageArgument());
        parser.registerArgument(new GetStatusNamesArgument());
        parser.registerArgument(new GetAllStatusItemsArgument());
        parser.registerArgument(new MonitorObsEventArgument());
        parser.registerArgument(new SendObsEventArgument());
        parser.registerArgument(new MonitorFileEventsArgument());
        parser.registerArgument(new DataLabelArgument());
        parser.registerArgument(new ExpectedValueArgument());
        parser.registerArgument(new ShowMillisecondsArgument());
	parser.registerArgument(new WaitArgument());

        //possible operations
        parser.registerOperation(new HelpOperation());
        parser.registerOperation(new CommandOperation());
        parser.registerOperation(new MonitorStatusOperation());
        parser.registerOperation(new GetStatusOperation());
        parser.registerOperation(new SetStatusOperation());
        parser.registerOperation(new GetStatusNamesOperation());
        parser.registerOperation(new GetAllStatusItemsOperation());
        parser.registerOperation(new MonitorObsEventOperation());
        parser.registerOperation(new SendObsEventOperation());
        parser.registerOperation(new MonitorFileEventsOperation());
	parser.registerOperation(new WaitOperation());

        
	//get the Operation the parser found
	Operation op = parser.parse();

        if (op != null) {
            execute(op);
        } else {
            Util.die("I'm sorry, what operation do you mean?");
        }
    }


    private static void execute(Operation op) throws Exception {
        try {
            System.exit(op.execute());
        } catch (Exception e) {
            Util.die("Error executing operation " + op + ", error message: " + e.getMessage());
        }
    }

    private static void initializeLogging() {
        if (!System.getProperties().containsKey("java.util.logging.config.file") && !System.getProperties().containsKey("java.util.logging.config.file")) {
            // If JULI is not already configured, do a very limited configuration locally
            LogManager logManager = LogManager.getLogManager();
            try {
                logManager.readConfiguration(GiapiTester.class.getResourceAsStream("logging.properties"));
            } catch (IOException e) {
                System.err.println("Couldn't read logging configuration");
            }
        }
        // Also configure Log4J as is used by ActiveMQ
        PropertyConfigurator.configure(GiapiTester.class.getResource("log4j.properties"));
    }

}
