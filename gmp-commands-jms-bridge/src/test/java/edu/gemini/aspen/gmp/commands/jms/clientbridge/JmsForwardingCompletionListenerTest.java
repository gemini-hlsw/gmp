package edu.gemini.aspen.gmp.commands.jms.clientbridge;

import edu.gemini.aspen.giapi.commands.Activity;
import edu.gemini.aspen.giapi.commands.Command;
import edu.gemini.aspen.giapi.commands.Configuration;
import edu.gemini.aspen.giapi.commands.HandlerResponse;
import edu.gemini.aspen.giapi.commands.SequenceCommand;
import edu.gemini.aspen.gmp.commands.jms.MockedJmsArtifactsTestBase;
import edu.gemini.jms.api.DestinationData;
import edu.gemini.jms.api.DestinationType;
import org.junit.Before;
import org.junit.Test;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import static org.mockito.ArgumentMatchers.*;

import static edu.gemini.aspen.giapi.commands.ConfigPath.configPath;
import static edu.gemini.aspen.giapi.commands.DefaultConfiguration.configurationBuilder;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class JmsForwardingCompletionListenerTest extends MockedJmsArtifactsTestBase {
    protected DestinationData destination;
    private String correlationID = "1";

    @Before
    public void setUp() throws Exception {
        destination = new DestinationData("name", DestinationType.TOPIC);
    }

    @Test
    public void testOnHandlerResponse() throws JMSException {
        super.createMockedObjects();

        JmsForwardingCompletionListener completionListener = new JmsForwardingCompletionListener(destination, correlationID);

        completionListener.startJms(provider);

        Configuration referenceConfiguration = configurationBuilder()
                .withPath(configPath("x:A"), "1")
                .withPath(configPath("x:B"), "2")
                .build();

        Command referenceCommand = new Command(SequenceCommand.APPLY, Activity.PRESET, referenceConfiguration);
        completionListener.onHandlerResponse(HandlerResponse.ACCEPTED, referenceCommand);

        verify(producer).send(any(Destination.class), any(MapMessage.class));
    }

    @Test
    public void testOnHandlerErrorResponse() throws JMSException {
        super.createMockedObjects();

        JmsForwardingCompletionListener completionListener = new JmsForwardingCompletionListener(destination, correlationID);

        completionListener.startJms(provider);

        Configuration referenceConfiguration = configurationBuilder()
                .withPath(configPath("x:A"), "1")
                .withPath(configPath("x:B"), "2")
                .build();

        Command referenceCommand = new Command(SequenceCommand.APPLY, Activity.PRESET, referenceConfiguration);
        completionListener.onHandlerResponse(HandlerResponse.createError("Error Message"), referenceCommand);

       verify(producer).send(any(Destination.class), any(MapMessage.class));
    }

    @Test
    public void testSendImmediateHandlerResponse() throws JMSException {
        super.createMockedObjects();

        JmsForwardingCompletionListener completionListener = new JmsForwardingCompletionListener(destination, correlationID);

        completionListener.startJms(provider);

        HandlerResponse response = HandlerResponse.get(HandlerResponse.Response.COMPLETED);
        completionListener.sendInitialResponseToClient(response);

        verify(producer).send(any(Destination.class), any(MapMessage.class));
        // Verify that 1 string has been set in the reply message
        verify(mapMessage, times(1)).setString(anyString(), anyString());

        // Verify that no properties have been set in the reply message
        verify(mapMessage, never()).setStringProperty(anyString(), anyString());
    }

    @Test
    public void testSendImmediateErrorHandlerResponse() throws JMSException {
        super.createMockedObjects();

        JmsForwardingCompletionListener completionListener = new JmsForwardingCompletionListener(destination, correlationID);

        completionListener.startJms(provider);

        HandlerResponse response = HandlerResponse.createError("Error Message");
        completionListener.sendInitialResponseToClient(response);

        verify(producer).send(any(Destination.class), any(MapMessage.class));
        // Verify that 2 strings has been set in the reply message, including response and error message
        verify(mapMessage, times(2)).setString(anyString(), anyString());

        // Verify that no properties have been set in the reply message
        verify(mapMessage, never()).setStringProperty(anyString(), anyString());
    }

    @Test
    public void testToString() throws JMSException {
        super.createMockedObjects();

        JmsForwardingCompletionListener completionListener = new JmsForwardingCompletionListener(destination, correlationID);
        assertNotNull(completionListener.toString());
    }

}
