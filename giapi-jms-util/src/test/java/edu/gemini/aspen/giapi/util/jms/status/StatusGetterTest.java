package edu.gemini.aspen.giapi.util.jms.status;

import edu.gemini.jms.api.JmsProvider;
import org.junit.Test;

import javax.jms.*;

import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Mockito.*;

public class StatusGetterTest {
    @Test
    public void testGetStatusNames() throws JMSException {
        StatusGetter statusGetter = new StatusGetter("Test getter");
        JmsProvider provider = mock(JmsProvider.class);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(provider.getConnectionFactory()).thenReturn(connectionFactory);

        Connection connection = mock(Connection.class);
        when(connectionFactory.createConnection()).thenReturn(connection);

        Session session = mock(Session.class);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);

        Queue queue = mock(Queue.class);
        when(session.createQueue(anyString())).thenReturn(queue);
        TemporaryQueue tempQueue = mock(TemporaryQueue.class);
        when(session.createTemporaryQueue()).thenReturn(tempQueue);
        Topic topic = mock(Topic.class);
        when(session.createTopic(anyString())).thenReturn(topic);

        Message message = mock(Message.class);
        when(session.createMessage()).thenReturn(message);
        MessageProducer producer = mock(MessageProducer.class);
        when(session.createProducer(or(any(Destination.class), isNull()))).thenReturn(producer);
        MessageConsumer consumer = mock(MessageConsumer.class);
        when(session.createConsumer(any(Destination.class))).thenReturn(consumer);

        statusGetter.startJms(provider);

        assertTrue(statusGetter.getStatusNames().isEmpty());
    }
}
