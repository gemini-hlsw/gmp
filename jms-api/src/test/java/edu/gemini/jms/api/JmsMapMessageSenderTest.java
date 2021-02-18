package edu.gemini.jms.api;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Topic;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JmsMapMessageSenderTest extends JmsArtifactTestBase {

    protected DestinationData destinationData = new DestinationData("GMP.TOPIC", DestinationType.TOPIC);
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
        JmsMapMessageSender sender = new JmsMapMessageSender("GMP.TOPIC");

        sender.sendMapMessage(destinationData, mapMessageBuilder);
    }

    @Test
    public void testSendStringBasedMapMessage() throws JMSException {
        mockSessionProducerAndConsumer();

        JmsMapMessageSender sender = new JmsMapMessageSender("GMP.TOPIC");
        sender.startJms(provider);

        // Attempt to send the message
        MapMessage sentMessage = sender.sendMapMessage(destinationData, mapMessageBuilder);

        // Verify the message has been sent
        assertNotNull(sentMessage);
        verify(producer).send(any(Topic.class), eq(mapMessage));
        assertTrue(called.get());

        sender.stopJms();
    }

}
