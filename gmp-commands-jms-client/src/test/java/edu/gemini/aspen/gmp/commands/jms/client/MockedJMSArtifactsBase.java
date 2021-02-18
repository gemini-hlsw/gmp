package edu.gemini.aspen.gmp.commands.jms.client;

import edu.gemini.aspen.giapi.util.jms.test.MapMessageMock;
import edu.gemini.jms.api.JmsProvider;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockedJMSArtifactsBase {
    protected ConnectionFactory connectionFactory;
    protected Session session;
    protected JmsProvider provider;
    protected MessageProducer producer;
    protected MessageConsumer consumer;
    protected MapMessageMock mapMessage;

    public void createMockedObjects() throws JMSException {
        provider = mock(JmsProvider.class);
        mockSessionProducerAndConsumer();
        when(provider.getConnectionFactory()).thenReturn(connectionFactory);
    }

    protected void mockSessionProducerAndConsumer() throws JMSException {
        session = mockSessionCreation();

        producer = mock(MessageProducer.class);
        when(session.createProducer(or(any(Destination.class), isNull()))).thenReturn(producer);
        consumer = mock(MessageConsumer.class);
        when(session.createConsumer(any(Destination.class), anyString())).thenReturn(consumer);

        Queue queue = mock(Queue.class);
        when(session.createQueue(anyString())).thenReturn(queue);

        Topic topic = mock(Topic.class);
        when(session.createTopic(anyString())).thenReturn(topic);

        mapMessage = new MapMessageMock();
        when(session.createMapMessage()).thenReturn(mapMessage);
    }

    private Session mockSessionCreation() throws JMSException {
        Session session = mock(Session.class);
        // Mock connection factory
        connectionFactory = mock(ConnectionFactory.class);

        // Mock connection
        Connection connection = mock(Connection.class);
        when(connectionFactory.createConnection()).thenReturn(connection);

        // Mock session
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        return session;
    }

    /**
     * This method allows tests to define what reply message a consumer will return
     * <br>
     * It is useful to simulate responses to a given command
     * <br>
     * Call it after you have called the createMockedObjects
     *
     * @param message
     * @throws JMSException
     */
    protected void mockReplyMessage(MapMessage message) throws JMSException {
        if (consumer != null) {
            when(consumer.receive(anyLong())).thenReturn(message);
        }
    }
}
