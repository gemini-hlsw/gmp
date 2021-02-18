package edu.gemini.jms.api;

import org.junit.Before;
import org.mockito.Mockito;

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

/**
 * Basically a duplicate of MockedJmsArtifactsTestBase but cannot be the same class to avoid
 * introducing a circular dependency
 */
public class JmsArtifactTestBase {
    protected ConnectionFactory connectionFactory;
    protected Session session;
    protected MessageProducer producer;
    protected JmsProvider provider;
    protected MapMessage mapMessage;

    @Before
    public void setupSession() {
        try {
            mockSessionProducerAndConsumer();
        } catch (JMSException e) {
            // Shouldn't happen as we are mocking
            e.printStackTrace();
        }
    }

    protected void mockSessionProducerAndConsumer() throws JMSException {
        session = mockSessionCreation();

        producer = Mockito.mock(MessageProducer.class);
        Mockito.when(session.createProducer(or(any(Destination.class), isNull()))).thenReturn(producer);
        MessageConsumer consumer = Mockito.mock(MessageConsumer.class);
        Mockito.when(session.createConsumer(any(Destination.class))).thenReturn(consumer);

        Queue queue = mock(Queue.class);
        when(session.createQueue(anyString())).thenReturn(queue);

        Topic topic = mock(Topic.class);
        when(session.createTopic(anyString())).thenReturn(topic);

        mapMessage = Mockito.mock(MapMessage.class);
        Destination destination = mock(Destination.class);
        when(mapMessage.getJMSReplyTo()).thenReturn(destination);
        when(session.createMapMessage()).thenReturn(mapMessage);
    }

    private Session mockSessionCreation() throws JMSException {
        Session session = Mockito.mock(Session.class);
        // Mock connection factory
        connectionFactory = Mockito.mock(ConnectionFactory.class);
        provider = mock(JmsProvider.class);
        when(provider.getConnectionFactory()).thenReturn(connectionFactory);

        // Mock connection
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connectionFactory.createConnection()).thenReturn(connection);

        // Mock session
        Mockito.when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        return session;
    }
}
