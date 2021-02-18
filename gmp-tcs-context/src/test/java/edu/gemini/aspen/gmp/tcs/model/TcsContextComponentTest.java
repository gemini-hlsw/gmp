package edu.gemini.aspen.gmp.tcs.model;

import edu.gemini.epics.EpicsReader;
import edu.gemini.jms.api.JmsProvider;
import org.junit.Before;
import org.junit.Test;

import javax.jms.*;

import static edu.gemini.aspen.gmp.tcs.model.EpicsTcsContextFetcher.TCS_CONTEXT_CHANNEL;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Mockito.*;

public class TcsContextComponentTest {

    private JmsProvider provider;
    private Session session;
    private EpicsReader reader;

    @Before
    public void setUp() throws Exception {
        provider = mock(JmsProvider.class);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        when(provider.getConnectionFactory()).thenReturn(connectionFactory);
        Connection connection = mock(Connection.class);
        when(connectionFactory.createConnection()).thenReturn(connection);
        session = mock(Session.class);
        when(connection.createSession(anyBoolean(), anyInt())).thenReturn(session);
        BytesMessage bytesMessage = mock(BytesMessage.class);
        when(session.createBytesMessage()).thenReturn(bytesMessage);
        MessageProducer producer = mock(MessageProducer.class);
        when(session.createProducer(or(any(Destination.class), isNull()))).thenReturn(producer);

        reader = mock(EpicsReader.class);
    }

    @Test
    public void testValidation() throws JMSException {
        TcsContextComponent component = new TcsContextComponent(reader, TCS_CONTEXT_CHANNEL, false, "");
        component.startJms(provider);

        verify(provider, times(2)).getConnectionFactory();
        verifyNoInteractions(reader);
    }

    @Test
    public void testInvalidation() throws JMSException {
        TcsContextComponent component = new TcsContextComponent(reader, TCS_CONTEXT_CHANNEL, false, "");
        component.startJms(provider);
        component.stopJms();
        verify(session, times(2)).close();
    }

}
