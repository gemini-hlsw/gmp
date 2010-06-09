package edu.gemini.giapi.tool.commands;

import javax.jms.*;

import edu.gemini.aspen.giapi.commands.*;
import edu.gemini.aspen.gmp.util.jms.MessageBuilder;
import edu.gemini.aspen.gmp.util.jms.GmpKeys;
import edu.gemini.giapi.tool.jms.BrokerConnection;
import edu.gemini.giapi.tool.TesterException;

/**
 * This class sends sequence commands to the GMP using the gateway
 * interface.
 */
public class CommandSender {


    private Session _session;
    private Destination _destination;
    private MessageProducer _producer;

    private Queue _replyQueue;
    private MessageConsumer _replyConsumer;

    public CommandSender(BrokerConnection connection) throws TesterException {
        try {
            _session = connection.getSession();
            _destination = _session.createTopic(GmpKeys.GW_COMMAND_TOPIC);
            _producer = _session.createProducer(_destination);
            _replyQueue = _session.createTemporaryQueue();
            _replyConsumer = _session.createConsumer(_replyQueue);

        } catch (JMSException ex) {
            throw new TesterException(ex);
        }
    }


    public HandlerResponse send(SequenceCommand command,
                     Activity activity,
                     Configuration config
                     ) throws TesterException {
        try {
            MapMessage m = _session.createMapMessage();
            m.setStringProperty(GmpKeys.GMP_SEQUENCE_COMMAND_KEY, command.name());
            m.setStringProperty(GmpKeys.GMP_ACTIVITY_KEY, activity.name());

            if (config != null && config.getKeys() != null) {
                for (ConfigPath path : config.getKeys()) {
                    m.setString(path.getName(), config.getValue(path));
                }
            }

            m.setJMSReplyTo(_replyQueue);
            _producer.send(_destination, m);

            //synchronously get the reply
            MapMessage reply = (MapMessage) _replyConsumer.receive();

            //There is a trick case when the completion listener finishes
            //before the first request. In that case, rather than
            //receiving a handler response, we get completion information.
            //In such case, we return the handler response that is
            //contained in the completion information
            if (reply.getString(GmpKeys.GMP_HANDLER_RESPONSE_KEY) != null) {
                //is a Handler Response message, normal case
                return MessageBuilder.buildHandlerResponse(reply);
            } else {
                //it's a completion listener message, this means the
                //completion info finished before the actual send call. Extract the Handler
                //out of it.
                CompletionInformation info = MessageBuilder.buildCompletionInformation(reply);
                return info.getHandlerResponse();
            }
        } catch (JMSException e) {
            throw new TesterException(e);
        }
    }



    public CompletionInformation receiveCompletionInformation(long timeout)
            throws TesterException {

        try {
            Message m = _replyConsumer.receive(timeout);
            if (m == null) {
                throw new TesterException("Timed out while waiting for completion information");
            }
            return MessageBuilder.buildCompletionInformation(m);
        } catch (JMSException e) {
            throw new TesterException(e);
        }
    }


}
