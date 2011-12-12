package edu.gemini.aspen.gmp.status.simulator;

import edu.gemini.jms.api.JmsProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import javax.jms.*;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class StatusSimulatorTest {
    private StatusSimulator component;
    protected ConnectionFactory connectionFactory;
    protected Session session;
    protected JmsProvider provider;
    protected MessageProducer producer;
    protected MessageConsumer consumer;
    protected BytesMessage bytesMessage;
    private String file;

    @Before
    public void loadFile() {
        file = StatusSimulator.class.getResource("status-simulator.xml").getFile();
    }

    @Test
    public void testCreation() throws InterruptedException, JAXBException, FileNotFoundException, JMSException {
        provider = mock(JmsProvider.class);
        mockSessionProducerAndConsumer();

        component = new StatusSimulator(provider, file);
        assertNotNull(component);
    }

    @Test
    public void testStartJMSProvider() throws InterruptedException, JAXBException, JMSException, FileNotFoundException {
        provider = mock(JmsProvider.class);
        mockSessionProducerAndConsumer();

        component = new StatusSimulator(provider, file);
        component.startJms(provider);
        verify(session, times(2)).createProducer(any(Destination.class));
    }

    @Test
    public void testStopJMSProvider() throws InterruptedException, JAXBException, JMSException, FileNotFoundException {
        provider = mock(JmsProvider.class);
        mockSessionProducerAndConsumer();

        component = new StatusSimulator(provider, file);
        component.startJms(provider);
        component.stopJms();
        verify(session, times(2)).close();
    }

    protected void mockSessionProducerAndConsumer() throws JMSException {
        session = mockSessionCreation();

        producer = mock(MessageProducer.class);
        when(session.createProducer(Matchers.<Destination>anyObject())).thenReturn(producer);
        consumer = mock(MessageConsumer.class);
        when(session.createConsumer(Matchers.<Destination>anyObject())).thenReturn(consumer);

        Queue queue = mock(Queue.class);
        when(session.createQueue(anyString())).thenReturn(queue);

        Topic topic = mock(Topic.class);
        when(session.createTopic(anyString())).thenReturn(topic);

        bytesMessage = mock(BytesMessage.class);
        when(session.createBytesMessage()).thenReturn(bytesMessage);

        when(provider.getConnectionFactory()).thenReturn(connectionFactory);
    }

    private Session mockSessionCreation() throws JMSException {
        Session session = mock(Session.class);
        // Mock connection factory
        connectionFactory = mock(ConnectionFactory.class);

        // Mock connection
        Connection connection = mock(Connection.class);
        when(connectionFactory.createConnection()).thenReturn(connection);

        // Mock session
        when(connection.createSession(Matchers.anyBoolean(), Matchers.anyInt())).thenReturn(session);
        return session;
    }

}