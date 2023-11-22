package edu.gemini.aspen.gmp.tcsoffset.model;

import com.google.gson.JsonObject;
import edu.gemini.aspen.gmp.tcsoffset.epics.EpicsTcsOffsetIOC;
import edu.gemini.aspen.gmp.tcsoffset.jms.JmsTcsOffsetDispatcher;
import edu.gemini.aspen.gmp.tcsoffset.jms.TcsOffsetRequestListener;
import edu.gemini.epics.EpicsObserver;
import edu.gemini.jms.api.*;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

import javax.jms.JMSException;
import java.util.logging.Logger;
import edu.gemini.epics.EpicsWriter;
/**
 * Interface to define a composite of several TCS Offset objects
 */
public class TcsOffsetComponent implements JmsArtifact {
    public static final Logger LOG = Logger.getLogger(TcsOffsetComponent.class.getName());

    private final Boolean simulation;

    private final EpicsObserver _eo;
    private final JsonObject _tcsChLoops;


    /**
     * The JMS Offset Dispatcher is a JMS Producer message
     * that will send the TCS Offset to the requester
     */
    private JmsTcsOffsetDispatcher _dispatcher;

    /**
     * JMS Listener to process the TCS Offset requests.
     */
    private TcsOffsetRequestListener _listener;

    /**
     * Message consumer used to receive TCS Offset requests
     */
    private BaseMessageConsumer _messageConsumer;
    private final EpicsWriter _ew1;

    private TcsOffsetIOC _tcsOffsetIOC;

    public final static String TCS_TOP_PROPERTY = "tcsTop";

    public final static String TCS_OFFSET_CONFIG = "offsetConfig";

    public final static String TCS_ChannelLoops = "tcsChLoops";

    public final static String SIMULATION = "simulation";

    public final static String OFFSETCONFIG = "offsetConfig";

    public JsonObject _offsetConfig;


    /**
     * TcsOffsetComponent constructor. 
     * @param ew1           : EpicsWriter service which is used to create Epics Channels with write permissions.
     * @param eo            : EpicsObserver service which is used to create monitors on Epics Channels records.
     * @param simulation    : Used to deploy the component in simulation mode.
     * @param offsetConfig  : JsonObject with the size of P and Q offset allowed.
     * @param jsonTcsChLoops: JsonObject with the sequence action to execute to open and close the loop.
     */
    public TcsOffsetComponent(EpicsWriter ew1, EpicsObserver eo,
                              Boolean simulation, JsonObject offsetConfig,
                              JsonObject jsonTcsChLoops) {

        this.simulation = simulation;
        _ew1 = ew1;
        _eo = eo;
        _dispatcher = new JmsTcsOffsetDispatcher("TCS Offset Replier");
        _offsetConfig = offsetConfig;
        _tcsChLoops = jsonTcsChLoops;
        _listener = new TcsOffsetRequestListener(_dispatcher, _offsetConfig, simulation);
        //Creates the TCS Offset Request Consumer
        _messageConsumer = new BaseMessageConsumer("JMS TCS Offset Request Consumer",
                                                    new DestinationData(TcsOffsetRequestListener.DESTINATION_NAME, DestinationType.TOPIC),
                                                    _listener
        );

    }

    public void start() throws JMSException, CAException, TimeoutException {
        LOG.info("Starting service, simulation is: " + simulation);
        if (!simulation) {
            _tcsOffsetIOC = new EpicsTcsOffsetIOC(_ew1, _eo, _tcsChLoops);
            _listener.registerTcsOffset(_tcsOffsetIOC);
        } else {
            LOG.warning("TCS in simulation mode not implemented yet");
        }
    }


    public void stop() {
        if (!simulation) {
            removeOldTcsOffset();
        }
    }

    private void removeOldTcsOffset() {
        if (_tcsOffsetIOC != null) {
            LOG.info("Removed old instance of EPICS writer");
            _listener.registerTcsOffset(null);
        }
    }

    @Override
    public void startJms(JmsProvider provider) throws JMSException {
        LOG.info("TCS Offset validated, starting... ");
        _dispatcher.startJms(provider);
        _messageConsumer.startJms(provider);
        LOG.info("TCS Offset Service started");
    }

    @Override
    public void stopJms() {
        LOG.info("TCS Offset stopped, disconnecting jms... ");
        _dispatcher.stopJms();
        _messageConsumer.stopJms();
        LOG.info("TCS Offset Service Stopped");
    }
}
