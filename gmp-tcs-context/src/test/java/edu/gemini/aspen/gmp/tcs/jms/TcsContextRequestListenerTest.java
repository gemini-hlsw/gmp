package edu.gemini.aspen.gmp.tcs.jms;

import edu.gemini.aspen.gmp.tcs.jms.JmsTcsContextDispatcher;
import edu.gemini.aspen.gmp.tcs.jms.TcsContextRequestListener;
import edu.gemini.aspen.gmp.tcs.model.TcsContextException;
import edu.gemini.aspen.gmp.tcs.model.TcsContextFetcher;
import org.junit.Before;
import org.junit.Test;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TcsContextRequestListenerTest {
    private double[] context = new double[]{1, 2, 3};
    private JmsTcsContextDispatcher contextDispatcher;
    private TcsContextFetcher fetcher;
    private Message message;
    private Destination destination;
    private TcsContextRequestListener listener;

    @Before
    public void setUp() throws Exception {
        contextDispatcher = mock(JmsTcsContextDispatcher.class);
        fetcher = mock(TcsContextFetcher.class);
        message = mock(Message.class);
        destination = mock(Destination.class);
        when(message.getJMSReplyTo()).thenReturn(destination);
        listener = new TcsContextRequestListener(contextDispatcher);
    }

    @Test
    public void testOnMessage() throws TcsContextException, JMSException {
        listener.registerTcsContextFetcher(fetcher);

        when(fetcher.getTcsContext()).thenReturn(context);
        listener.onMessage(message);

        verify(fetcher, atLeastOnce()).getTcsContext();
        verify(contextDispatcher).send(eq(context), eq(destination));
    }

    @Test
    public void testOnMessageWithoutFetcher() throws TcsContextException, JMSException {
        when(fetcher.getTcsContext()).thenReturn(context);

        listener.onMessage(message);

        verifyNoInteractions(contextDispatcher);
    }

    @Test
    public void testOnMessageWithNullContext() throws TcsContextException, JMSException {
        when(fetcher.getTcsContext()).thenReturn(null);
        listener.registerTcsContextFetcher(fetcher);

        listener.onMessage(message);

        verifyNoInteractions(contextDispatcher);
    }

    @Test
    public void testOnMessageWithJMSException() throws TcsContextException, JMSException {
        when(fetcher.getTcsContext()).thenThrow(new TcsContextException("Exception"));
        listener.registerTcsContextFetcher(fetcher);

        listener.onMessage(message);

        verifyNoInteractions(contextDispatcher);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOnMessageWithNoDispatcher() throws TcsContextException, JMSException {
        new TcsContextRequestListener(null);
    }

}
