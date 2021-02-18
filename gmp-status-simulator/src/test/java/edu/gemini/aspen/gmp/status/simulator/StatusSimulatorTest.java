package edu.gemini.aspen.gmp.status.simulator;

import edu.gemini.gmp.top.Top;
import edu.gemini.jms.api.JmsProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.jms.*;
import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.AdditionalMatchers.or;
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
    private Top top;

    @Before
    public void loadFile() {
        file = StatusSimulator.class.getResource("status-simulator.xml").getFile();
        top= mock(Top.class);
        when(top.buildStatusItemName(Mockito.anyString())).thenReturn("test");}

    @Test
    public void testCreation() throws InterruptedException, JAXBException, FileNotFoundException, JMSException {
        provider = mock(JmsProvider.class);
        mockSessionProducerAndConsumer();
        component = new StatusSimulator(file,top);
        assertNotNull(component);
        verify(top, times(3)).buildStatusItemName(anyString());
    }

    @Test
    public void testStartJMSProvider() throws InterruptedException, JAXBException, JMSException, FileNotFoundException {
        provider = mock(JmsProvider.class);
        mockSessionProducerAndConsumer();

        component = new StatusSimulator(file, top);
        component.startJms(provider);

        TimeUnit.MILLISECONDS.sleep(300);

        verify(session, times(3)).createProducer(or(any(Destination.class), isNull()));
    }

    @Test
    public void testStopJMSProvider() throws InterruptedException, JAXBException, JMSException, FileNotFoundException {
        provider = mock(JmsProvider.class);
        mockSessionProducerAndConsumer();

        component = new StatusSimulator(file, top);
        component.startJms(provider);
        TimeUnit.MILLISECONDS.sleep(300);

        component.stopJms();
        TimeUnit.MILLISECONDS.sleep(300);

        verify(session, times(3)).close();
    }

    protected void mockSessionProducerAndConsumer() throws JMSException {
        session = mockSessionCreation();

        producer = mock(MessageProducer.class);
        when(session.createProducer(or(any(Destination.class), isNull()))).thenReturn(producer);
        consumer = mock(MessageConsumer.class);
        when(session.createConsumer(any(Destination.class))).thenReturn(consumer);

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
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        return session;
    }

}