package edu.gemini.aspen.gmp.tcsoffset.jms;

import edu.gemini.jms.api.BaseMessageProducer;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.util.logging.Logger;

/**
 * This class is responsible to send the response to the client (Instrument) after
 * executed an offset. 
 */
public class JmsTcsOffsetDispatcher extends BaseMessageProducer {
    private static final Logger LOG = Logger.getLogger(JmsTcsOffsetDispatcher.class.getName());

    public JmsTcsOffsetDispatcher(String clientName) {
        super(clientName, null);
    }

    /**
     * Send the message to the client. 
     * @param destination  : Client destination to send the offset response. 
     * @param offsetApplied: Message response with the offset result. 
     */

    public void sendOffsetResult(Destination destination, String offsetApplied) throws JMSException {
        TextMessage msg = _session.createTextMessage();
        msg.setText(offsetApplied);
        _producer.send(destination, msg);
    }
}
