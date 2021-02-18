package edu.gemini.aspen.gmp.commands.jms;

import edu.gemini.jms.api.JmsProvider;
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

public class MockedJmsArtifactsTestBase {
    protected ConnectionFactory connectionFactory;
    protected Session session;
    protected JmsProvider provider;
    protected MessageProducer producer;
    protected MessageConsumer consumer;
    protected MapMessage mapMessage;

    public void createMockedObjects() {
        provider = Mockito.mock(JmsProvider.class);
        try {
            mockSessionProducerAndConsumer();
            when(provider.getConnectionFactory()).thenReturn(connectionFactory);
        } catch (JMSException e) {
            // Shouldn't happen as we are mocking
            e.printStackTrace();
        }
    }

    public void createMockedObjectsThatThrowJMSException() {
        provider = Mockito.mock(JmsProvider.class);
        try {
            connectionFactory = Mockito.mock(ConnectionFactory.class);

            // Mock connection
            when(connectionFactory.createConnection()).thenThrow(new JMSException(""));
        } catch (JMSException e) {
            // Shouldn't happen as we are mocking
            e.printStackTrace();
        }
    }

    protected void mockSessionProducerAndConsumer() throws JMSException {
        session = mockSessionCreation();

        producer = Mockito.mock(MessageProducer.class);
        when(session.createProducer(or(any(Destination.class), isNull()))).thenReturn(producer);
        consumer = Mockito.mock(MessageConsumer.class);
        when(session.createConsumer(any(Destination.class))).thenReturn(consumer);

        Queue queue = mock(Queue.class);
        when(session.createQueue(anyString())).thenReturn(queue);

        Topic topic = mock(Topic.class);
        when(session.createTopic(anyString())).thenReturn(topic);

        mapMessage = Mockito.mock(MapMessage.class);
        Destination destination = Mockito.mock(Destination.class);
        when(mapMessage.getJMSReplyTo()).thenReturn(destination);
        when(session.createMapMessage()).thenReturn(mapMessage);
    }

    private Session mockSessionCreation() throws JMSException {
        Session session = Mockito.mock(Session.class);
        // Mock connection factory
        connectionFactory = Mockito.mock(ConnectionFactory.class);

        // Mock connection
        Connection connection = Mockito.mock(Connection.class);
        when(connectionFactory.createConnection()).thenReturn(connection);

        // Mock session
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        return session;
    }
}
