package edu.gemini.aspen.gmp.tcsoffset.jms;


import com.google.gson.JsonObject;
import edu.gemini.aspen.giapi.util.jms.JmsKeys;
import edu.gemini.aspen.giapi.offset.OffsetType;
import edu.gemini.aspen.gmp.tcsoffset.model.TcsOffsetException;
import edu.gemini.aspen.gmp.tcsoffset.model.TcsOffsetIOC;

import javax.jms.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This class is responsible for receiving requests from clients (Instruments) to send offsets to the TCS. 
 * The type of offset that is implemented is "Instrument Offset". 
 * Therefore, the RA and DEC of the target are not changed.
 */
public class TcsOffsetRequestListener implements MessageListener {
    private static final Logger LOG = Logger.getLogger(TcsOffsetRequestListener.class.getName());
    public static final String DESTINATION_NAME = JmsKeys.GMP_TCS_OFFSET_DESTINATION;

    /**
     * Message producer used to send the TCS Offset back to the requester
     */
    private  JmsTcsOffsetDispatcher _dispatcher;
    private  JsonObject _offsetConfig;

    private  Boolean _simulation;

    /**
     * Apply the offsets to the TCS. 
     */
    private TcsOffsetIOC _epicsTcsOffsetIOC;

    /**
     * Constructor. Takes as an argument the JMS dispatcher that will
     * be used to reply back to the requester.
     *
     * @param dispatcher JMS Dispatcher that sends back the TCS Offseta
     * @param offsetConfig json object describing the threshold of each 
     *        observation phase and the sequence management loops (closing and opening).
     */
    public TcsOffsetRequestListener(JmsTcsOffsetDispatcher dispatcher, JsonObject offsetConfig) {
        setAttributes(dispatcher, offsetConfig, false);
    }

    public TcsOffsetRequestListener(JmsTcsOffsetDispatcher dispatcher, JsonObject offsetConfig, Boolean simulation) {
        setAttributes(dispatcher, offsetConfig, simulation);
    }

    private void setAttributes (JmsTcsOffsetDispatcher dispatcher, JsonObject offsetConfig, Boolean simulation ) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("Cannot construct TcsOffsetRequestListener with a null dispatcher");
        }
        _dispatcher = dispatcher;
        _offsetConfig = offsetConfig;
        _simulation = simulation;
    }

    /**
     * Register the TCS Offset IOC instance which will apply the offsets to TCS IOC. 
     * @param epicsTcsOffsetIOC applies the TCS offsets. 
     *
     */
    public void registerTcsOffset(TcsOffsetIOC epicsTcsOffsetIOC) {
        _epicsTcsOffsetIOC = epicsTcsOffsetIOC;
    }

    /**
     * Receives the request. Gets the destination to reply,
     * obtains the TCS Offset (if possible) and send it
     * back to the requester.
     *
     * @param message Client message with the TCS offset information to apply. .
     */
    public void onMessage(Message message) {

        LOG.log(Level.FINER, "Message received");
        String offsetResult=null;
        try {
            offsetResult = processApplyOffset(message)+"|";
        } catch (TcsOffsetException e) {
            offsetResult = 0 + "|" + e.getMessage();
        }

        try {
            LOG.log(Level.FINER, "Sending reapply");
            _dispatcher.sendOffsetResult(message.getJMSReplyTo(), offsetResult);
        }catch (JMSException e) {
            LOG.log(Level.WARNING, "Error sending the response to the client", e);
        }
    }

    /**
     * Process message sent by client (Instrument)
     * @param message  Binary client message with the following structure "p q typeOffset"
     *                 P and Q are the offset sizes. Units arcseconds.
     *                 typeOffset: Indicate the observation stage Acquisition or Slow Guiding Correciton.
     */

    private int processApplyOffset(Message message) throws TcsOffsetException {
        try {
            Destination replyDestination = message.getJMSReplyTo();
            BytesMessage msg = (BytesMessage) message;

            // Apply the offset to the TCS
            sendOffset( msg.readDouble(),
                        msg.readDouble(),
                        OffsetType.getFromInt(msg.readInt()),
                        message.getStringProperty("instName"));

        } catch (JMSException e) {
            LOG.log(Level.WARNING, "Error reading the msg", e);
            throw new TcsOffsetException(TcsOffsetException.Error.READING_JMS_MESSAGE,
                                              "Error reading the JMS message ", e);
        } catch (TcsOffsetException e) {
            LOG.log(Level.WARNING, "Problem applying TCS Offset", e);
            throw e;
        }
        return 1;
    }

    /** 
     * Check the P and Q offset limits. 
     * @param obj   : Json Configuration object with the max and min allowd limits
     * @param value : Value to be checked.
     */

    private boolean checkLimits(JsonObject obj, double value)  {
        double max = obj.get("max").getAsDouble();
        double min = obj.get("min").getAsDouble();
        LOG.fine("maxC: "+ max + " minC: "+ min + " value: "+ value);
        if (  Math.abs(value) > Math.abs(max) || Math.abs(value) < Math.abs(min) )
            return false;
        return true;
    }


    /**
     * Check that the offsets are within the allowed limits for each client (Instrument), 
     * and then send the offset to be applied to the TcsOffsetIOC. 
     * @param p: P offset value. Units arcsenconds. 
     * @param q: Q offset value. Units arcsenconds. 
     * @param instName: Instrument name which want to apply the offset. 
     * @throws TcsOffsetException: If the offset can be applied send a TcsOffsetException which 
     *                             has the error code, the Exception trace and the message for the client. 
     */

    private void sendOffset(double p, double q, OffsetType offsetType, String instName) throws TcsOffsetException {
        JsonObject obj = _offsetConfig.getAsJsonObject(instName).getAsJsonObject(offsetType.getText()).getAsJsonObject("offset");
        boolean limitP = checkLimits(obj.getAsJsonObject("p"), p);
        boolean limitQ = checkLimits(obj.getAsJsonObject("q"), q);
        LOG.fine("Limit1 : " + limitP + " limit2: " + limitQ);
        if (!limitP && !limitQ)
            throw new TcsOffsetException(TcsOffsetException.Error.OUT_OF_LIMIT,
                    "The offset is out the limit defined for the instrument");

        double pVal = (limitP) ? p : 0;
        double qVal = (limitQ) ? q : 0;
        try {
            _epicsTcsOffsetIOC.setTcsOffset(pVal,qVal);
        } catch (TcsOffsetException e) {
            e.printStackTrace();
            if (e.getTypeError() == TcsOffsetException.Error.TCS_WAS_REBOOTED)
                _epicsTcsOffsetIOC.setTcsOffset(p,q);
            else
                throw e;
        }
    }

    private boolean canDispatchOffset(Destination d) throws TcsOffsetException {
        return _epicsTcsOffsetIOC != null && d!= null;
    }
}
