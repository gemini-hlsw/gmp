package edu.gemini.jms.api;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Topic;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JmsMapMessageSenderReplyTest extends JmsArtifactTestBase {

    protected DestinationData destinationData = new DestinationData("GMP.TOPIC", DestinationType.TOPIC);
    protected final String replyTo = "GMP.REPLY_TOPIC";
    protected Map<String, String> messageBody;
    protected Map<String, String> messageProperties;
    protected AtomicBoolean called;
    protected MapMessageBuilder mapMessageBuilder;

    @Before
    public void setUp() throws Exception {
        messageBody = ImmutableMap.of("x:A", "1");
        messageProperties = ImmutableMap.of("HANDLER_RESPONSE", "COMPLETED");
        called = new AtomicBoolean(false);
        mapMessageBuilder = new AbstractMapMessageBuilder() {
            @Override
            public MapMessage constructMessageBody(MapMessage message) throws JMSException {
                super.setStringBasedMessageBody(message, messageBody);
                super.setStringBasedMessageBody(message, messageProperties);

                // Check that this is called
                called.set(true);
                return message;
            }
        };
    }

    @Test(expected = MessagingException.class)
    public void testSendMapMessageWhenNotConnected() {
        MapMessageBuilder mapMessageBuilder = mock(MapMessageBuilder.class);
        JmsMapMessageSenderReply<String> sender = new StringSenderReply("GMP.TOPIC");

        sender.sendMapMessage(destinationData, mapMessageBuilder);
    }

    @Test
    public void testSendStringBasedMapMessage() throws JMSException {
        mockSessionProducerAndConsumer();

        JmsMapMessageSenderReply<String> sender = new StringSenderReply("GMP.TOPIC");
        sender.startJms(provider);

        String reply = sender.sendMessageWithReply(destinationData, mapMessageBuilder, 1000);

        assertEquals("GMP.TOPIC", reply);
        verify(producer).send(any(Topic.class), eq(mapMessage));

        assertTrue(called.get());
    }

    /**
     * Class that extends JmsMapMessageSenderReply for testing
     */
    private class StringSenderReply extends JmsMapMessageSenderReply<String> {

        public StringSenderReply(String clientName) {
            super(clientName);
        }

        @Override
        protected MessageConsumer createReplyConsumer(Message requestMessage) throws JMSException {
            return super._session.createConsumer(requestMessage.getJMSReplyTo());
        }

        @Override
        protected String buildResponse(Message reply) throws JMSException {
            return super._clientName;
        }
    }

}
