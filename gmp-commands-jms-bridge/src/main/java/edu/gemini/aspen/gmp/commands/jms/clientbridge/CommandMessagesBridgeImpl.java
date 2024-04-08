package edu.gemini.aspen.gmp.commands.jms.clientbridge;

import com.google.common.base.Preconditions;
import edu.gemini.aspen.giapi.commands.Command;
import edu.gemini.aspen.giapi.commands.CommandSender;
import edu.gemini.aspen.giapi.commands.HandlerResponse;
import edu.gemini.aspen.giapi.util.jms.JmsKeys;
import edu.gemini.jms.api.*;

import javax.jms.JMSException;
import javax.jms.Message;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This component listens to commands sent over JMS and forwards them to CommandSender, basically
 * acting as a Bridge between clients and the CommandSender
 * <br>
 * It also creates listeners to track responses to the commands
 */
public class CommandMessagesBridgeImpl implements CommandMessagesBridge {
    private static final Logger LOG = Logger.getLogger(CommandMessagesBridgeImpl.class.getName());
    private static final DestinationData REPLY_DESTINATION = new DestinationData(JmsKeys.GW_COMMAND_REPLY_QUEUE, DestinationType.QUEUE);

    private final JmsProvider _jmsProvider;
    private final CommandSender _commandSender;

    /**
     * Creates a new CommandMessageBridge
     *
     * @param jmsProvider The JmsProvider required to listen for incoming requests and send the replies
     * @param commandSender The CommandSender to forward valid requests
     */
    public CommandMessagesBridgeImpl(JmsProvider jmsProvider, CommandSender commandSender) {
        Preconditions.checkArgument(jmsProvider != null, "JMS Provider cannot be null");
        Preconditions.checkArgument(commandSender != null, "CommandSender cannot be null");
        _jmsProvider = jmsProvider;
        _commandSender = commandSender;
    }

    @Override
    public void onMessage(Message message) {
        try {
            Command command = decodeMessage(message);
            sendCommandAndEvaluateResponse(command, message.getJMSCorrelationID());
        } catch (FormatException e) {
            LOG.log(Level.WARNING, "Message did not contain a command message: ", e);
        } catch (JMSException e) {
            //this is produced when sending initial reply back to the client
            LOG.log(Level.SEVERE, "Problem sending response back to client ", e);
        } catch (MessagingException e) {
            // This exception could happen when the Client sent first the completed
            // before sending the STARTED. The exception is triggered in the
            // JmsMapMessageSender.sendMapMessage(JmsMapMessageSender.java:53)
            LOG.log(Level.SEVERE, "It was not possible to send a response " +
                                       "to client because the sender is not ready", e);

        }
    }

    private Command decodeMessage(Message message) throws JMSException {
        CommandMessageParser messageParser = new CommandMessageParser(message);
        // For all kind of non valid messages readCommand will throw a FormatExcption
        Command command = messageParser.readCommand();
        LOG.fine("New command arrived: " + command + " with correlationID " + message.getJMSCorrelationID());
        return command;
    }

    private void sendCommandAndEvaluateResponse(Command command, String correlationID) throws JMSException {
        JmsForwardingCompletionListener listener = setupCompletionListener(correlationID);

        // Forward the command
        HandlerResponse response = _commandSender.sendCommand(command, listener);

        listener.sendInitialResponseToClient(response);

        if (!shouldWaitForMoreResponses(response)) {
           listener.stopListeningForResponses();
        }
    }

    private boolean shouldWaitForMoreResponses(HandlerResponse response) {
        return response.getResponse() == HandlerResponse.Response.STARTED;
    }

    private JmsForwardingCompletionListener setupCompletionListener(String correlationID) throws JMSException {
        JmsForwardingCompletionListener listener = new JmsForwardingCompletionListener(REPLY_DESTINATION, correlationID);

        listener.startJms(_jmsProvider);
        return listener;
    }

}
