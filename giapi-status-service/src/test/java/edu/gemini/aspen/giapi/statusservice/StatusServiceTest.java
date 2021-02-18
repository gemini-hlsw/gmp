package edu.gemini.aspen.giapi.statusservice;

import edu.gemini.jms.api.JmsProvider;
import org.junit.Before;
import org.junit.Test;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Status Service
 *
 * @cquiroz
 */
public class StatusServiceTest {


    private StatusService service;
    private JmsProvider provider;

    @Before
    public void setUp()  throws JMSException {
        StatusHandlerAggregate aggregate = new StatusHandlerAggregate();
        String serviceName = "Service Name";
        String serviceStatus = ">";
        provider = buildJMSProviderMock();
        service = new StatusService(aggregate, serviceName, serviceStatus);
    }

    @Test
    public void testStartJms() throws JMSException, InterruptedException {
        service.startJms(provider);
        verify(provider).getConnectionFactory();
    }

    private JmsProvider buildJMSProviderMock() throws JMSException {
        // Build the mock of the JMSProvider and its interactions
        JmsProvider provider = mock(JmsProvider.class);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(provider.getConnectionFactory()).thenReturn(connectionFactory);

        Connection connection = mock(Connection.class);
        when(connectionFactory.createConnection()).thenReturn(connection);

        Session session = mock(Session.class);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        return provider;
    }

    @Test
    public void testUnbinding() throws JMSException, InterruptedException {
        service.startJms(provider);
        TimeUnit.MILLISECONDS.sleep(300);

        verify(provider).getConnectionFactory();
        service.stopJms();

        verify(provider).getConnectionFactory();
    }

}