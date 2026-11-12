package edu.gemini.jms.api;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.TemporaryQueue;

/**
 * Base class to model a request/reply communication using JMS.
 * <br>
 * Implementations of this class need to define how to reconstruct the
 * reply message in the communication as an Object with the method buildResponse
 * <br>
 * Also they need to customize what kind of consumer will be used to get the reply
 * overriding the method createReplyConsumer. For example the reply could be a temporary
 * queue or a fixed queue with a correlationID selector
 */
public abstract class JmsMapMessageSenderReply<T> extends JmsMapMessageSender
        implements MapMessageSenderReply<T> {

    public JmsMapMessageSenderReply(String clientName) {
        super(clientName);
    }

    @Override
    public T sendMessageWithReply(DestinationData destination,
                                  MapMessageBuilder messageBuilder,
                                  long timeout) throws MessagingException {
        long startTime = System.currentTimeMillis();
        try {
            Destination jmsDestination = createDestination(destination);
            MapMessage requestMessage = MapMessageCreator.ReplyCreator.createMapMessage(_session);

            messageBuilder.constructMessageBody(requestMessage);

            // The reply consumer must exist before sending the request. Fast STOMP clients
            // can reply immediately to JMSReplyTo; if no consumer exists yet, ActiveMQ may
            // reject the temporary destination before GMP can receive the initial response.
            MessageConsumer replyConsumer = createReplyConsumer(requestMessage);

            _producer.send(jmsDestination, requestMessage);

            T replyObject = waitForReply(requestMessage, replyConsumer, timeout);
            long endTime = System.currentTimeMillis();
            LOG.fine("Sending/reply on " + destination + " took " + (endTime - startTime) + "   [ms]");
            return replyObject;
        } catch (JMSException e) {
            throw new MessagingException("Unable to send message", e);
        }
    }

    private T waitForReply(Message requestMessage, MessageConsumer replyConsumer, long timeout) {
        try {
            return waitForReplyMessage(requestMessage, replyConsumer, timeout);
        } catch (JMSException e) {
            throw new MessagingException("Problem receiving reply", e);
        }
    }

    protected T waitForReplyMessage(Message requestMessage, long timeout) throws JMSException {
        return waitForReplyMessage(requestMessage, createReplyConsumer(requestMessage), timeout);
    }

    private T waitForReplyMessage(Message requestMessage, MessageConsumer tempConsumer, long timeout) throws JMSException {
        Message reply = tempConsumer.receive(timeout);
        tempConsumer.close();

        if (requestMessage.getJMSReplyTo() instanceof TemporaryQueue) {
            TemporaryQueue temporaryQueue = (TemporaryQueue) requestMessage.getJMSReplyTo();
            temporaryQueue.delete();
        }
        if (reply == null) {
            LOG.severe("Reply awaited on " + tempConsumer + " is null, probably the response timed-out after " + timeout + " [ms]");
            //throw new JMSException("Expected reply for a request");
        }
        return buildResponse(reply);
    }

    /**
     * Creates a consumer that is able to accept replies to the request message
     *
     * @param requestMessage
     * @return a Message Consumer listening for replies to the request message
     * @throws JMSException
     */
    protected abstract MessageConsumer createReplyConsumer(Message requestMessage) throws JMSException;

    /**
     * Reconstruct the reply object from the reply message
     *
     * @param reply message representing the answer to the request
     * @return object representing the reply.
     * @throws JMSException if there is a problem decoding the
     *                      reply
     */
    protected abstract T buildResponse(Message reply) throws JMSException;
}
