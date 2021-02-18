package edu.gemini.aspen.giapi.statusservice.jms;

import edu.gemini.aspen.giapi.status.StatusHandler;
import edu.gemini.aspen.giapi.status.StatusItem;
import org.junit.Test;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Simple test ensuring JMS Messages are passed to StatusHandlers
 *
 */
public class JmsStatusListenerTest {

    @Test
    public void testOnMessage() throws Exception {
        StatusHandler handler = mock(StatusHandler.class);
        JmsStatusListener listener = new JmsStatusListener(handler);

        Message message = constructMessageMock();
        listener.onMessage(message);
        verify(handler).update(any(StatusItem.class));
    }

    private BytesMessage constructMessageMock() throws JMSException {
        BytesMessage message = mock(BytesMessage.class);

        when(message.getBodyLength()).thenReturn(1L);
        // 0 for int code
        when(message.readUnsignedByte()).thenReturn(0);
        when(message.readUTF()).thenReturn("item");
        // 0 for status value
        when(message.readInt()).thenReturn(0);
        return message;
    }
}
