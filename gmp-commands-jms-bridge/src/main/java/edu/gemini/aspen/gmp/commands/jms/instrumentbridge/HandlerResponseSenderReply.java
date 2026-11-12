package edu.gemini.aspen.gmp.commands.jms.instrumentbridge;

import edu.gemini.aspen.giapi.commands.HandlerResponse;
import edu.gemini.aspen.giapi.util.jms.HandlerResponseMessageParser;
import edu.gemini.jms.api.JmsMapMessageSenderReply;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import edu.gemini.aspen.giapi.util.jms.JmsKeys;
import java.util.logging.Logger;

/**
 * Extension of {@link edu.gemini.jms.api.JmsMapMessageSenderReply<T>} that can understand and decode a
 * reply of a {@link edu.gemini.aspen.giapi.commands.SequenceCommand} in the form
 * of a {@link edu.gemini.aspen.giapi.commands.HandlerResponse}
 */
class HandlerResponseSenderReply extends JmsMapMessageSenderReply<HandlerResponse> {
    private static final Logger LOG = Logger.getLogger(HandlerResponseSenderReply.class.getName());

    public HandlerResponseSenderReply(String topicName) {
        super(topicName);
    }

    @Override
    public HandlerResponse buildResponse(Message reply) throws JMSException {
        // A null reply means the instrument did not answer before the timeout. Return NOANSWER,
        // as before; without this check the non-MapMessage branch below would throw a
        // NullPointerException, which escapes CommandMessagesBridgeImpl.onMessage and makes
        // ActiveMQ redeliver the gateway command, sending it to the instrument again.
        if (reply == null) {
            LOG.warning("Null reply received from instrument handler; returning NOANSWER");
            return HandlerResponse.NOANSWER;
        }
        if (reply instanceof MapMessage) {
            HandlerResponseMessageParser messageParser = new HandlerResponseMessageParser(reply);
            return messageParser.readResponse();
        }
        String responseType = reply.getStringProperty(JmsKeys.GMP_HANDLER_RESPONSE_KEY);
        if (responseType == null) {
            LOG.warning("Non-MapMessage reply missing GMP_HANDLER_RESPONSE property; returning NOANSWER");
            return HandlerResponse.NOANSWER;
        }
        LOG.fine("Non-MapMessage reply with GMP_HANDLER_RESPONSE=" + responseType);
        HandlerResponse.Response response;
        try {
            response = HandlerResponse.Response.valueOf(responseType);
        } catch (IllegalArgumentException e) {
            LOG.warning("Non-MapMessage reply with invalid GMP_HANDLER_RESPONSE=" + responseType + "; returning NOANSWER");
            return HandlerResponse.NOANSWER;
        }
        if (response == HandlerResponse.Response.ERROR) {
            String errorMsg = reply.getStringProperty(JmsKeys.GMP_HANDLER_RESPONSE_ERROR_KEY);
            return HandlerResponse.createError(errorMsg);
        }
        return HandlerResponse.get(response);
    }

    /**
     * This SenderReply object will listen based only on the temporary topic created to get responses
     * and set in the getJmsReplyTo method
     */
    @Override
    protected MessageConsumer createReplyConsumer(Message requestMessage) throws JMSException {
        return _session.createConsumer(requestMessage.getJMSReplyTo());
    }

}
