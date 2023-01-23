package edu.gemini.aspen.gmp.statusservice;

import edu.gemini.aspen.giapi.status.*;
import edu.gemini.aspen.gmp.statusservice.generated.*;
import edu.gemini.gmp.top.Top;
import edu.gemini.cas.AlarmChannel;
import edu.gemini.cas.ChannelAccessServer;
import edu.gemini.epics.api.Channel;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class EpicsStatusService, uses an ChannelAccessServer to create channels, listens
 * to the corresponding StatusItem updates and informs the server.
 *
 * @author Nicolas A. Barriga
 *         Date: Nov 9, 2010
 */
//TODO:support multiple values (array)
public class EpicsStatusService implements StatusHandler {
    private static final Logger LOG = Logger.getLogger(EpicsStatusService.class.getName());
    private static final Map<AlarmCause, Status> CAUSE_MAP = new EnumMap<AlarmCause, Status>(AlarmCause.class);
    private static final Map<AlarmSeverity, Severity> SEVERITY_MAP = new EnumMap<AlarmSeverity, Severity>(AlarmSeverity.class);

    static {
        CAUSE_MAP.put(AlarmCause.ALARM_CAUSE_OK, Status.NO_ALARM);
        CAUSE_MAP.put(AlarmCause.ALARM_CAUSE_HIHI, Status.HIHI_ALARM);
        CAUSE_MAP.put(AlarmCause.ALARM_CAUSE_HI, Status.HIGH_ALARM);
        CAUSE_MAP.put(AlarmCause.ALARM_CAUSE_LO, Status.LOW_ALARM);
        CAUSE_MAP.put(AlarmCause.ALARM_CAUSE_LOLO, Status.LOLO_ALARM);
        CAUSE_MAP.put(AlarmCause.ALARM_CAUSE_OTHER, Status.SOFT_ALARM);//TODO: fix this mapping

        SEVERITY_MAP.put(AlarmSeverity.ALARM_OK, Severity.NO_ALARM);
        SEVERITY_MAP.put(AlarmSeverity.ALARM_WARNING, Severity.MINOR_ALARM);
        SEVERITY_MAP.put(AlarmSeverity.ALARM_FAILURE, Severity.MAJOR_ALARM);
    }

    private static final String NAME = "GMP_EPICS_STATUS_SERVICE";
    /**
     * Structure mapping Giapi Status Item -> Epics PV
     */
    private final Map<String, Channel<?>> channelMap = new HashMap<String, Channel<?>>();
    private final Map<String, AlarmChannel<?>> alarmChannelMap = new HashMap<String, AlarmChannel<?>>();
    private final Map<String, Channel<String>> healthChannelMap = new HashMap<String, Channel<String>>();

    private final ChannelAccessServer _channelAccessServer;
    private final String xmlFileName;
    private final Top top;


    public EpicsStatusService(ChannelAccessServer cas,
            Top top,
            String xmlFileName) {
        _channelAccessServer = cas;
        this.xmlFileName = xmlFileName;
        this.top = top;
    }


    public void initialize() throws JAXBException, SAXException {
        EpicsStatusServiceConfiguration conf = new EpicsStatusServiceConfiguration(xmlFileName);
        initialize(conf.getSimulatedChannels());
    }

    /**
     * Initialize the EpicsStatusService. Creates appropriate channels in an ChannelAccessServer.
     *
     * @param items channels to create and listen to.
     */
    public void initialize(Channels items) {
        for (BaseChannelType item : items.getSimpleChannelOrAlarmChannelOrHealthChannel()) {
            try {
                if (item instanceof HealthChannelType) {
                    addHealthVariable(top.buildStatusItemName(item.getGiapiname()), top.buildEpicsChannelName(item.getEpicsname()), Health.valueOf("BAD"));
                } else if (item instanceof AlarmChannelType) {
                    addAlarmVariable(top.buildStatusItemName(item.getGiapiname()), top.buildEpicsChannelName(item.getEpicsname()), ChannelsHelper.getInitial((SimpleChannelType) item));
                } else if (item instanceof SimpleChannelType) {
                    addVariable(top.buildStatusItemName(item.getGiapiname()), top.buildEpicsChannelName(item.getEpicsname()), ChannelsHelper.getInitial((SimpleChannelType) item));
                }
            } catch (CAException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
    }


    /**
     * Destroys the registered channels in the ChannelAccessServer
     */
    public void shutdown() {
        try {
            for (String name : channelMap.keySet().toArray(new String[0])) {
                removeVariable(name);
            }
            for (String name : alarmChannelMap.keySet().toArray(new String[0])) {
                removeVariable(name);
            }
            for (String name : healthChannelMap.keySet().toArray(new String[0])) {
                removeVariable(name);
            }
        } catch (Exception e) {
            LOG.warning("Exception shutting down EpicsStatusService " + e);
        }
    }

    private <T> void addVariable(String giapiName, String epicsName, T value) throws CAException {
        if (_channelAccessServer != null) {
            Channel<T> ch = _channelAccessServer.createChannel(epicsName, value);
            channelMap.put(giapiName, ch);
        } else {
            throw new IllegalStateException("The cas service is unavailable");
        }
        LOG.info("Added variable GIAPI: " + giapiName + " EPICS: " + epicsName);
    }

    private <T> void addAlarmVariable(String giapiName, String epicsName, T value) throws CAException {
        if (_channelAccessServer != null) {
            AlarmChannel<T> ch = _channelAccessServer.createAlarmChannel(epicsName, value);
            alarmChannelMap.put(giapiName, ch);
        } else {
            throw new IllegalStateException("The cas service is unavailable");
        }
        LOG.info("Added alarm variable GIAPI: " + giapiName + " EPICS: " + epicsName);
    }

    private void addHealthVariable(String giapiName, String epicsName, Health health) throws CAException {
        if (_channelAccessServer != null) {
            Channel<String> ch = _channelAccessServer.createChannel(epicsName, health.name());
            healthChannelMap.put(giapiName, ch);
        } else {
            throw new IllegalStateException("The cas service is unavailable");
        }
        LOG.info("Added health variable GIAPI: " + giapiName + " EPICS: " + epicsName);
    }

    private void removeVariable(String giapiName) throws IllegalStateException {
        Channel ch = null;
        if (channelMap.containsKey(giapiName)) {
            ch = channelMap.remove(giapiName);
        } else if (alarmChannelMap.containsKey(giapiName)) {
            ch = alarmChannelMap.remove(giapiName);
        } else if (healthChannelMap.containsKey(giapiName)) {
            ch = healthChannelMap.remove(giapiName);
        }
        if (ch != null) {
            if (_channelAccessServer != null) {
                _channelAccessServer.destroyChannel(ch);
            } else {
                throw new IllegalStateException("The cas service is unavailable");
            }
        } else {
            LOG.warning("Channel " + giapiName + " is not registered.");
        }
    }

    /**
     * Just for testing
     *
     * @return unmodifiable map
     */
    Map<String, Channel<?>> getChannels() {
        return Collections.unmodifiableMap(channelMap);
    }

    /**
     * Just for testing
     *
     * @return unmodifiable map
     */
    Map<String, AlarmChannel<?>> getAlarmChannels() {
        return Collections.unmodifiableMap(alarmChannelMap);
    }

    /**
     * Just for testing
     *
     * @return unmodifiable map
     */
    Map<String, Channel<String>> getHealthChannels() {
        return Collections.unmodifiableMap(healthChannelMap);
    }

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public <T> void update(StatusItem<T> item) {
        String itemName = item.getName();
        try {
            if (channelMap.containsKey(item.getName())) {
                Channel ch = channelMap.get(item.getName());
                ch.setValue(item.getValue());
            } else if (alarmChannelMap.containsKey(itemName)) {
                if (item instanceof AlarmStatusItem) {
                    AlarmChannel ch = alarmChannelMap.get(itemName);
                    ch.setValue(item.getValue());
                    setAlarm((AlarmStatusItem<T>) item, ch);
                } else {
                    LOG.warning("Received StatusItem that is not an AlarmStatusItem for " + item.getName());
                }
            } else if (healthChannelMap.containsKey(item.getName())) {
                if (item instanceof HealthStatusItem) {
                    Channel<String> ch = healthChannelMap.get(item.getName());
                    ch.setValue(((HealthStatusItem) item).getHealth().name());
                } else {
                    LOG.warning("Received StatusItem that is not a HealthStatusItem for " + item.getName());
                }
            } else {
                //received a status item not on the config file
                //ignoring it
                LOG.finer("Unknown item " + item.getName());
            }
        } catch (CAException ex) {
            LOG.log(Level.SEVERE, "Error setting status item " + item);
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.SEVERE, "Error setting status item " + item);
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        } catch (TimeoutException e) {
            LOG.log(Level.SEVERE, "Error setting status item " + item);
            LOG.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private <T> void setAlarm(AlarmStatusItem<T> item, AlarmChannel<T> channel) {
        AlarmState state = item.getAlarmState();
        try {
            channel.setAlarm(CAUSE_MAP.get(state.getCause()), SEVERITY_MAP.get(state.getSeverity()), state.getMessage());
        } catch (CAException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);

        }
    }

}
