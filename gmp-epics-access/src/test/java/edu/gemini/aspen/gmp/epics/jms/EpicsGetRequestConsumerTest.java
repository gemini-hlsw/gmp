package edu.gemini.aspen.gmp.epics.jms;

import edu.gemini.aspen.giapi.util.jms.JmsKeys;
import edu.gemini.epics.EpicsReader;
import edu.gemini.epics.ReadOnlyClientEpicsChannel;
import edu.gemini.epics.api.ChannelAlarmListener;
import edu.gemini.epics.api.ChannelListener;
import edu.gemini.jms.api.JmsProvider;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.DBR;
import gov.aps.jca.dbr.DBRType;
import gov.aps.jca.dbr.DBR_String;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.jms.*;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EpicsGetRequestConsumerTest {
    private JmsProvider provider;
    private Session session;
    private MessageProducer producer;

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
        Topic topic = mock(Topic.class);
        when(session.createTopic(anyString())).thenReturn(topic);
        producer = mock(MessageProducer.class);
        when(session.createProducer(or(any(Destination.class), isNull()))).thenReturn(producer);

        BytesMessage mapMessage = mock(BytesMessage.class);
        when(session.createBytesMessage()).thenReturn(mapMessage);
        MessageConsumer messageConsumer = mock(MessageConsumer.class);
        when(session.createConsumer(any(Destination.class))).thenReturn(messageConsumer);
    }

    @Test
    public void create() {
        EpicsReader epicsReader = mock(EpicsReader.class);
        assertNotNull(new EpicsGetRequestConsumer(provider, epicsReader));
    }

    @Test
    public void requestNotFound() throws JMSException {
        EpicsReader epicsReader = mock(EpicsReader.class);
        EpicsGetRequestConsumer epicsGetRequestConsumer = new EpicsGetRequestConsumer(provider, epicsReader);
        Message msg = mock(Message.class);
        when(msg.getStringProperty(anyString())).thenReturn(null);
        epicsGetRequestConsumer.onMessage(msg);
        // does nothing
    }

    @Test
    public void requestValidForUnknownChannel() throws JMSException {
        EpicsReader epicsReader = mock(EpicsReader.class);
        EpicsGetRequestConsumer epicsGetRequestConsumer = new EpicsGetRequestConsumer(provider, epicsReader);
        Message msg = mock(Message.class);
        String channelName = "tst:a";
        when(msg.getStringProperty(JmsKeys.GMP_GEMINI_EPICS_CHANNEL_PROPERTY)).thenReturn(channelName);

        Destination destination = mock(Destination.class);

        when(msg.getJMSReplyTo()).thenReturn(destination);
        epicsGetRequestConsumer.onMessage(msg);
        // does nothing
    }

    @Test
    public void requestValidForKnownChannel() throws JMSException {
        EpicsReader epicsReader = mock(EpicsReader.class);
        EpicsGetRequestConsumer epicsGetRequestConsumer = new EpicsGetRequestConsumer(provider, epicsReader);
        Message msg = mock(Message.class);
        String channelName = "tst:a";
        when(msg.getStringProperty(JmsKeys.GMP_GEMINI_EPICS_CHANNEL_PROPERTY)).thenReturn(channelName);

        Destination destination = mock(Destination.class);

        when(msg.getJMSReplyTo()).thenReturn(destination);

        final ReadOnlyClientEpicsChannel<String> readOnlyClientEpicsChannel = new ReadOnlyClientEpicsChannel<String>() {
            @Override
            public void destroy() throws CAException {

            }

            @Override
            public DBR getDBR() throws CAException, TimeoutException {
                DBR_String dbr = new DBR_String("value");
                return dbr;
            }

            @Override
            public List<String> getAll() throws CAException, TimeoutException {
                return null;
            }

            @Override
            public String getFirst() throws CAException, TimeoutException {
                return null;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void registerListener(ChannelListener<String> listener) throws CAException {

            }

            @Override
            public void unRegisterListener(ChannelListener<String> listener) throws CAException {

            }

            @Override
            public void registerListener(ChannelAlarmListener<String> listener) throws CAException {

            }

            @Override
            public void unRegisterListener(ChannelAlarmListener<String> listener) throws CAException {

            }

            @Override
            public boolean isValid() {
                return false;
            }

            @Override
            public DBRType getType() {
                return null;
            }
        };

        when(epicsReader.getChannelAsync(channelName)).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return readOnlyClientEpicsChannel;
            }
        });

        epicsGetRequestConsumer.onMessage(msg);
        verify(producer).send(any(Destination.class), any(Message.class));
    }

}
