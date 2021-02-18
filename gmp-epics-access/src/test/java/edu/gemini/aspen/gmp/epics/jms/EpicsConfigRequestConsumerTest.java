package edu.gemini.aspen.gmp.epics.jms;

import com.google.common.collect.ImmutableSet;
import edu.gemini.aspen.giapi.util.jms.JmsKeys;
import edu.gemini.aspen.gmp.epics.EpicsConfiguration;
import edu.gemini.jms.api.JmsProvider;
import org.junit.Before;
import org.junit.Test;

import javax.jms.*;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class EpicsConfigRequestConsumerTest {

    private JmsProvider provider;
    private EpicsConfiguration configuration;
    private Session session;
    private String channelName = "X.val1";

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

        configuration = new EpicsConfiguration() {
            @Override
            public Set<String> getValidChannelsNames() {
                return ImmutableSet.of(channelName);
            }
        };
    }

    @Test
    public void testConstruction() throws JMSException {
        EpicsConfigRequestConsumer consumer = new EpicsConfigRequestConsumer(provider, configuration);
        assertNotNull(consumer);
    }

    @Test
    public void onMessage() throws JMSException {
        MessageProducer messageProducer = mock(MessageProducer.class);
        when(session.createProducer(null)).thenReturn(messageProducer);
        MapMessage replyMessage = mock(MapMessage.class);
        when(session.createMapMessage()).thenReturn(replyMessage);

        EpicsConfigRequestConsumer consumer = new EpicsConfigRequestConsumer(provider, configuration);
        Message message = mock(Message.class);

        when(message.getBooleanProperty(JmsKeys.GMP_GEMINI_EPICS_CHANNEL_PROPERTY)).thenReturn(true);
        Destination destination = mock(Destination.class);
        when(message.getJMSReplyTo()).thenReturn(destination);



        consumer.onMessage(message);

        verify(replyMessage).setBoolean(channelName, true);
        verify(messageProducer).send(destination,replyMessage);
    }

    @Test
    public void onMessageWithNoReplyAddress() throws JMSException {
        EpicsConfigRequestConsumer consumer = new EpicsConfigRequestConsumer(provider, configuration);
        Message message = mock(Message.class);

        when(message.getBooleanProperty(JmsKeys.GMP_GEMINI_EPICS_CHANNEL_PROPERTY)).thenReturn(true);
        when(message.getJMSReplyTo()).thenReturn(null);
        consumer.onMessage(message);

        verify(message).getBooleanProperty(JmsKeys.GMP_GEMINI_EPICS_CHANNEL_PROPERTY);
        verify(message).getJMSReplyTo();
        verifyNoMoreInteractions(message);
    }

    @Test
    public void onMessageWithoutChannelProperty() throws JMSException {
        EpicsConfigRequestConsumer consumer = new EpicsConfigRequestConsumer(provider, configuration);
        Message message = mock(Message.class);

        when(message.getBooleanProperty(JmsKeys.GMP_GEMINI_EPICS_CHANNEL_PROPERTY)).thenReturn(false);
        consumer.onMessage(message);

        verify(message).getBooleanProperty(JmsKeys.GMP_GEMINI_EPICS_CHANNEL_PROPERTY);
        verifyNoMoreInteractions(message);
    }

}
