package edu.gemini.aspen.gmp.commands.jms.instrumentbridge;

import edu.gemini.aspen.giapi.commands.CommandUpdater;
import edu.gemini.aspen.giapi.commands.HandlerResponse;
import edu.gemini.aspen.giapi.util.jms.JmsKeys;
import edu.gemini.aspen.gmp.commands.jms.MockedJmsArtifactsTestBase;
import edu.gemini.aspen.gmp.commands.model.SequenceCommandException;
import org.junit.Before;
import org.junit.Test;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class CompletionInfoListenerTest extends MockedJmsArtifactsTestBase {
    private MapMessage message;
    private CommandUpdater commandUpdater;

    @Before
    public void setUp() throws Exception {
        message = mock(MapMessage.class);
        commandUpdater = mock(CommandUpdater.class);

        super.createMockedObjects();
    }

    @Test
    public void testOCSUpdate() throws JMSException {
        CompletionInfoListener listener = new CompletionInfoListener(commandUpdater);
        listener.startJms(provider);
        when(message.propertyExists(JmsKeys.GMP_ACTIONID_PROP)).thenReturn(true);
        when(message.getIntProperty(JmsKeys.GMP_ACTIONID_PROP)).thenReturn(1);
        when(message.getString(JmsKeys.GMP_HANDLER_RESPONSE_KEY)).thenReturn(HandlerResponse.Response.COMPLETED.toString());

        listener.onMessage(message);

        verify(commandUpdater).updateOcs(1, HandlerResponse.get(HandlerResponse.Response.COMPLETED));
    }

    @Test(expected = SequenceCommandException.class)
    public void testExceptionOnMessageQuerying() throws JMSException {
        CompletionInfoListener listener = new CompletionInfoListener(commandUpdater);
        listener.startJms(provider);
        when(message.propertyExists(JmsKeys.GMP_ACTIONID_PROP)).thenReturn(true);
        when(message.getString(JmsKeys.GMP_HANDLER_RESPONSE_KEY)).thenThrow(new JMSException("exception"));

        listener.onMessage(message);
    }

    @Test
    public void testNoMapMessage() throws JMSException {
        CompletionInfoListener listener = new CompletionInfoListener(commandUpdater);
        listener.startJms(provider);
        Message message = mock(Message.class);

        listener.onMessage(message);

        verifyNoInteractions(commandUpdater);
    }

    @Test
    public void testMessageMissingActionId() throws JMSException {
        CompletionInfoListener listener = new CompletionInfoListener(commandUpdater);
        listener.startJms(provider);
        MapMessage message = mock(MapMessage.class);

        listener.onMessage(message);

        verifyNoInteractions(commandUpdater);
    }

    @Test
    public void testLifeCycle() throws JMSException, InterruptedException {
        CompletionInfoListener listener = new CompletionInfoListener(commandUpdater);
        listener.startJms(provider);

        TimeUnit.MILLISECONDS.sleep(500);

        verify(provider).getConnectionFactory();

        listener.stopJms();
    }
}
