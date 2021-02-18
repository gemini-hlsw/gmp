package edu.gemini.aspen.gmp.commands.jms.clientbridge;

import edu.gemini.aspen.giapi.commands.Activity;
import edu.gemini.aspen.giapi.commands.Command;
import edu.gemini.aspen.giapi.commands.CommandSender;
import edu.gemini.aspen.giapi.commands.CompletionListener;
import edu.gemini.aspen.giapi.commands.HandlerResponse;
import edu.gemini.aspen.giapi.commands.SequenceCommand;
import edu.gemini.aspen.giapi.util.jms.JmsKeys;
import edu.gemini.aspen.giapi.util.jms.test.MapMessageMock;
import edu.gemini.aspen.gmp.commands.jms.MockedJmsArtifactsTestBase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Queue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;

public class CommandMessagesBridgeImplTest extends MockedJmsArtifactsTestBase {
    protected CommandSender commandsSender;
    protected CommandMessagesBridgeImpl messagesBridge;
    protected ArgumentCaptor<Command> commandCaptor;
    protected ArgumentCaptor<CompletionListener> listenerCaptor;

    @Before
    public void setUp() throws Exception {
        super.createMockedObjects();

        commandsSender = mock(CommandSender.class);
        messagesBridge = new CommandMessagesBridgeImpl(provider, commandsSender);

        commandCaptor = ArgumentCaptor.forClass(Command.class);
        listenerCaptor = ArgumentCaptor.forClass(CompletionListener.class);
    }

    @Test
    public void testOnMessageWithAcceptedResponse() throws JMSException {
        mockCommandResponse(HandlerResponse.Response.ACCEPTED);
        MapMessage message = createClientApplyCommandMessage();

        messagesBridge.onMessage(message);

        verifyCommandWasCalled();
        // Check the reply message was sent
        verifyReplyToClientSent(1);
        // Check the listener closed itself
        verifyListenerClosedItself();
    }

    private void verifyListenerClosedItself() throws JMSException {
        verify(session).close();
    }

    private void verifyReplyToClientSent(int times) throws JMSException {
        verify(producer, times(times)).send(any(Queue.class), any(MapMessage.class));
    }

    /**
     * Mocks the response of an APPLY message sent to CommandSender
     */
    private void mockCommandResponse(HandlerResponse.Response response) throws JMSException {
        HandlerResponse handlerResponse = HandlerResponse.get(response);
        when(commandsSender.sendCommand(commandCaptor.capture(), listenerCaptor.capture())).thenReturn(handlerResponse);
    }

    @Test
    public void testOnMessageWithStartedAndSecondResponse() throws JMSException {
        mockCommandResponse(HandlerResponse.Response.STARTED);
        MapMessage message = createClientApplyCommandMessage();
        messagesBridge.onMessage(message);

        verifyCommandWasCalled();

        verifyReplyToClientSent(1);

        // Now send a completion, this should happen when the instrument completes and responds
        listenerCaptor.getValue().onHandlerResponse(HandlerResponse.get(HandlerResponse.Response.COMPLETED), commandCaptor.getValue());

        // A new message should have been sent over JMS
        verifyReplyToClientSent(2);

        // Check the listener closed itself
        verifyListenerClosedItself();

    }

    private void verifyCommandWasCalled() {
        verify(commandsSender).sendCommand(commandCaptor.capture(), listenerCaptor.capture());
    }

    private MapMessage createClientApplyCommandMessage() throws JMSException {
        MapMessage message = new MapMessageMock();
        message.setStringProperty(JmsKeys.GMP_SEQUENCE_COMMAND_KEY, SequenceCommand.APPLY.toString());
        message.setStringProperty(JmsKeys.GMP_ACTIVITY_KEY, Activity.PRESET.toString());
        message.setString("x:A", "1");
        message.setString("x:B", "2");

        message.setJMSCorrelationID("123");
        return message;
    }

}
