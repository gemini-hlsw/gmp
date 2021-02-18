package edu.gemini.gmp.commands.records;

import edu.gemini.aspen.giapi.commands.*;
import edu.gemini.gmp.top.Top;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Class MockFactory
 *
 * @author Nicolas A. Barriga
 *         Date: 4/11/11
 */
public class MockFactory {

    public static CommandSender createCommandSenderMock(Top epicsTop, String cadName) {
        CommandSender cs;

        cs = mock(CommandSender.class);
        Command start = new Command(SequenceCommand.valueOf(cadName.toUpperCase()), Activity.START, DefaultConfiguration.configurationBuilder().
                withConfiguration("DATA_LABEL", "label").
                build());
        Command preset = new Command(SequenceCommand.valueOf(cadName.toUpperCase()), Activity.PRESET, DefaultConfiguration.configurationBuilder().
                withConfiguration("DATA_LABEL", "label").
                build());
        Command cancel = new Command(SequenceCommand.valueOf(cadName.toUpperCase()), Activity.CANCEL, DefaultConfiguration.configurationBuilder().
                withConfiguration("DATA_LABEL", "label").
                build());
        Command preset_start = new Command(SequenceCommand.valueOf(cadName.toUpperCase()), Activity.PRESET_START, DefaultConfiguration.configurationBuilder().
                withConfiguration("DATA_LABEL", "label").
                build());
        when(cs.sendCommand(eq(preset), any(CompletionListener.class), anyLong())).thenReturn(HandlerResponse.ACCEPTED);
        when(cs.sendCommand(eq(preset), any(CompletionListener.class))).thenReturn(HandlerResponse.ACCEPTED);
        when(cs.sendCommand(eq(start), any(CompletionListener.class), anyLong())).thenReturn(HandlerResponse.COMPLETED);
        when(cs.sendCommand(eq(start), any(CompletionListener.class))).thenReturn(HandlerResponse.COMPLETED);
        when(cs.sendCommand(eq(cancel), any(CompletionListener.class), anyLong())).thenReturn(HandlerResponse.ACCEPTED);
        when(cs.sendCommand(eq(cancel), any(CompletionListener.class))).thenReturn(HandlerResponse.ACCEPTED);
        when(cs.sendCommand(eq(preset_start), any(CompletionListener.class), anyLong())).thenReturn(HandlerResponse.COMPLETED);
        when(cs.sendCommand(eq(preset_start), any(CompletionListener.class))).thenReturn(HandlerResponse.COMPLETED);

        return cs;
    }
}
