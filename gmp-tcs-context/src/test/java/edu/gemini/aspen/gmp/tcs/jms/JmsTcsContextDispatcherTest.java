package edu.gemini.aspen.gmp.tcs.jms;

import edu.gemini.aspen.gmp.tcs.jms.JmsTcsContextDispatcher;
import edu.gemini.jms.api.JmsProvider;
import org.junit.Test;

import javax.jms.*;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Mockito.*;

public class JmsTcsContextDispatcherTest {
    private double[] context;

    @Test
    public void testSendContext() throws JMSException {
        JmsTcsContextDispatcher tcsContextDispatcher = new JmsTcsContextDispatcher("tcs:sad:astCtx");
        JmsProvider provider = mock(JmsProvider.class);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(provider.getConnectionFactory()).thenReturn(connectionFactory);
        Connection connection = mock(Connection.class);
        when(connectionFactory.createConnection()).thenReturn(connection);
        Session session = mock(Session.class);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        BytesMessage bytesMessage = mock(BytesMessage.class);
        when(session.createBytesMessage()).thenReturn(bytesMessage);
        MessageProducer producer = mock(MessageProducer.class);
        when(session.createProducer(or(any(Destination.class), isNull()))).thenReturn(producer);

        tcsContextDispatcher.startJms(provider);

        Destination destination = mock(Destination.class);
        context = new double[] {1, 2, 3};
        tcsContextDispatcher.send(context, destination);

        verify(producer).send(destination, bytesMessage);
    }

    @Test
    public void testSendNullContext() throws JMSException {
        JmsTcsContextDispatcher tcsContextDispatcher = new JmsTcsContextDispatcher("tcs:sad:astCtx");
        Destination destination = mock(Destination.class);
        tcsContextDispatcher.send(context, destination);

        verifyNoInteractions(destination);
    }
}
