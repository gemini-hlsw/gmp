package edu.gemini.giapi.tool.commands;

import edu.gemini.aspen.giapi.commands.*;
import edu.gemini.aspen.gmp.commands.jms.client.CommandSenderClient;
import edu.gemini.giapi.tool.arguments.ActivityArgument;
import edu.gemini.giapi.tool.arguments.ConfigArgument;
import edu.gemini.giapi.tool.arguments.SequenceCommandArgument;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;

public class CommandOperationTest {
    @Test
    public void testConstructionOfPark() {
        CommandOperation commandOperation = new CommandOperation();

        SequenceCommandArgument sequenceCommandArgument = new SequenceCommandArgument();
        sequenceCommandArgument.parseParameter("PARK");

        ActivityArgument activityArgument = new ActivityArgument();
        activityArgument.parseParameter("PRESET");

        assertFalse(commandOperation.isReady());

        commandOperation.setArgument(sequenceCommandArgument);
        assertFalse(commandOperation.isReady());

        commandOperation.setArgument(activityArgument);
        assertTrue(commandOperation.isReady());
    }

    @Test
    public void testConstructionOfApply() {
        CommandOperation commandOperation = new CommandOperation();

        buildApplyCommand(commandOperation);

        assertTrue(commandOperation.isReady());
    }

    private void buildApplyCommand(CommandOperation commandOperation) {
        SequenceCommandArgument sequenceCommandArgument = new SequenceCommandArgument();
        sequenceCommandArgument.parseParameter("APPLY");

        ActivityArgument activityArgument = new ActivityArgument();
        activityArgument.parseParameter("PRESET_START");

        ConfigArgument configArgument = new ConfigArgument();
        configArgument.parseParameter("x:A=1 x:B=2");

        commandOperation.setArgument(sequenceCommandArgument);
        commandOperation.setArgument(activityArgument);
        commandOperation.setArgument(configArgument);
    }

    @Test
    public void testErrorResponse() throws Exception {
        CommandSenderClient senderClient = mock(CommandSenderClient.class);
        CommandOperation commandOperation = new CommandOperation(senderClient);

        buildApplyCommand(commandOperation);

        when(senderClient.sendCommand(any(Command.class), any(CompletionListener.class))).thenReturn(HandlerResponse.createError("Error"));

        assertEquals(1, commandOperation.execute());
    }

    @Test
    public void testMultipleConfigBug() throws Exception {
        CommandSenderClient senderClient = mock(CommandSenderClient.class);
        CommandOperation commandOperation = new CommandOperation(senderClient);

        SequenceCommandArgument sequenceCommandArgument = new SequenceCommandArgument();
        sequenceCommandArgument.parseParameter("APPLY");

        ActivityArgument activityArgument = new ActivityArgument();
        activityArgument.parseParameter("PRESET_START");

        ConfigArgument configArgument = new ConfigArgument();
        configArgument.parseParameter("x:A=1 x:B=2");

        ConfigArgument secondConfigArgument = new ConfigArgument();
        secondConfigArgument.parseParameter("x:C=3");

        commandOperation.setArgument(sequenceCommandArgument);
        commandOperation.setArgument(activityArgument);
        commandOperation.setArgument(configArgument);
        commandOperation.setArgument(secondConfigArgument);

        ArgumentCaptor<Command> argument = ArgumentCaptor.forClass(Command.class);
        when(senderClient.sendCommand(argument.capture(), any(CompletionListener.class))).thenReturn(HandlerResponse.createError("Error"));

        assertEquals(1, commandOperation.execute());

        Configuration config = DefaultConfiguration.configurationBuilder().withConfiguration("x:A","1").withConfiguration("x:B", "2").withConfiguration("x:C", "3").build();
        Command command = new Command(SequenceCommand.APPLY, Activity.PRESET_START, config);
        assertEquals(command, argument.getValue());
    }

    @Test
    public void testNoAnswerIsAnError() throws Exception {
        CommandSenderClient senderClient = mock(CommandSenderClient.class);
        CommandOperation commandOperation = new CommandOperation(senderClient);

        buildApplyCommand(commandOperation);

        when(senderClient.sendCommand(any(Command.class), any(CompletionListener.class))).thenReturn(HandlerResponse.get(HandlerResponse.Response.NOANSWER));

        assertEquals(1, commandOperation.execute());
    }

}
