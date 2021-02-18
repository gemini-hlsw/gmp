package edu.gemini.jms.api;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import javax.jms.JMSException;
import javax.jms.MapMessage;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AbstractMapMessageBuilderTest {

    private AbstractMapMessageBuilder messageBuilder = new AbstractMapMessageBuilder() {

        @Override
        public MapMessage constructMessageBody(MapMessage message) {
            // Not used in this test
            return null;
        }
    };
    private MapMessage message = mock(MapMessage.class);
    private Map<String, Object> messageContent = ImmutableMap.of("item1", (Object) "value1");
    private Map<String, Object> messageProperties = ImmutableMap.of("item1", (Object) "value1");
    private Map<String, String> stringMessageProperties = ImmutableMap.of("item1", "value1");
    private Map<String, String> stringMessageContent = ImmutableMap.of("item1", "value1");

    @Test
    public void testFillStringBasedMessage() throws JMSException {
        doThrow(new JMSException("Exception")).when(message).setObject(anyString(), any());
        messageBuilder.setStringBasedMessageBody(message, stringMessageContent);

        verify(message, times(messageContent.size())).setString(anyString(), anyString());
    }

    @Test(expected = MessagingException.class)
    public void testFillStringBasedMessageWithAnException() throws JMSException {
        doThrow(new JMSException("Exception")).when(message).setString(anyString(), anyString());
        messageBuilder.setStringBasedMessageBody(message, stringMessageContent);
    }

    @Test
    public void testSetStringMessageProperties() throws JMSException {
        messageBuilder.setStringMessageProperties(message, stringMessageProperties);

        verify(message, times(messageProperties.size())).setStringProperty(anyString(), anyString());
    }

    @Test(expected = MessagingException.class)
    public void testSetStringMessagePropertiesWithException() throws JMSException {
        doThrow(new JMSException("Exception")).when(message).setStringProperty(anyString(), anyString());

        messageBuilder.setStringMessageProperties(message, stringMessageProperties);
    }

    @Test
    public void testSetMessageProperties() throws JMSException {
        messageBuilder.setMessageProperties(message, messageProperties);

        verify(message, times(messageProperties.size())).setObjectProperty(anyString(), any());
    }

    @Test(expected = MessagingException.class)
    public void testSetMessagePropertiesWithException() throws JMSException {
        doThrow(new JMSException("Exception")).when(message).setObjectProperty(anyString(), any());

        messageBuilder.setMessageProperties(message, messageProperties);
    }

    @Test
    public void testBuildMessage() throws JMSException {
        messageBuilder.setMessageBody(message, messageContent);

        verify(message, times(messageContent.size())).setObject(anyString(), any());
    }

    @Test(expected = MessagingException.class)
    public void testBuildMessageWithException() throws JMSException {
        doThrow(new JMSException("Exception")).when(message).setObject(anyString(), any());

        messageBuilder.setMessageBody(message, messageContent);
    }
}
