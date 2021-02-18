package edu.gemini.aspen.gmp.epics.impl;

import edu.gemini.epics.EpicsReader;
import edu.gemini.jms.api.JmsProvider;
import org.junit.Before;
import org.junit.Test;

import javax.jms.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EpicsRequestHandlerImplTest {
    private JmsProvider provider;
    private Session session;

    @Before
    public void setupMocks() throws JMSException {
        provider = mock(JmsProvider.class);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(provider.getConnectionFactory()).thenReturn(connectionFactory);
        Connection connection = mock(Connection.class);
        when(connectionFactory.createConnection()).thenReturn(connection);
        session = mock(Session.class);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);

        Queue destination = mock(Queue.class);
        when(session.createQueue(anyString())).thenReturn(destination);
        MessageConsumer messageConsumer = mock(MessageConsumer.class);
        when(session.createConsumer(destination)).thenReturn(messageConsumer);
    }

    @Test
    public void create() {
        EpicsReader epicsReader = mock(EpicsReader.class);
        assertNotNull(new EpicsRequestHandlerImpl(epicsReader));
    }

    @Test
    public void startJms() throws JMSException {
        EpicsReader epicsReader = mock(EpicsReader.class);
        EpicsRequestHandlerImpl epicsRequestHandler = new EpicsRequestHandlerImpl(epicsReader);
        epicsRequestHandler.startJms(provider);
    }
}
