package edu.gemini.aspen.gmp.epicstostatus;

import edu.gemini.aspen.giapi.status.AlarmCause;
import edu.gemini.aspen.giapi.status.AlarmSeverity;
import edu.gemini.aspen.giapi.status.AlarmState;
import edu.gemini.aspen.giapi.status.Health;
import edu.gemini.aspen.giapi.status.impl.AlarmStatus;
import edu.gemini.aspen.giapi.status.impl.BasicStatus;
import edu.gemini.aspen.giapi.status.impl.HealthStatus;
import edu.gemini.aspen.giapi.status.setter.StatusSetterImpl;
import edu.gemini.aspen.gmp.epicstostatus.generated.AlarmChannelType;
import edu.gemini.aspen.gmp.epicstostatus.generated.Channels;
import edu.gemini.aspen.gmp.epicstostatus.generated.HealthChannelType;
import edu.gemini.aspen.gmp.epicstostatus.generated.SimpleChannelType;
import edu.gemini.epics.EpicsException;
import edu.gemini.epics.EpicsReader;
import edu.gemini.epics.ReadOnlyClientEpicsChannel;
import edu.gemini.epics.api.ChannelAlarmListener;
import edu.gemini.epics.api.ChannelListener;
import edu.gemini.jms.api.JmsProvider;
import gov.aps.jca.CAException;
import gov.aps.jca.dbr.Severity;
import gov.aps.jca.dbr.Status;
import org.xml.sax.SAXException;

import javax.jms.JMSException;
import jakarta.xml.bind.JAXBException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class EpicsToStatusComponent
 *
 * @author Nicolas A. Barriga
 *         Date: 1/24/12
 */
public class EpicsToStatusComponent {
    private static final Logger LOG = Logger.getLogger(EpicsToStatusComponent.class.getName());
    private static final Map<Status, AlarmCause> CAUSE_MAP = new HashMap<Status, AlarmCause>();
    private static final Map<Severity, AlarmSeverity> SEVERITY_MAP = new HashMap<Severity, AlarmSeverity>();

    static {
        CAUSE_MAP.put(Status.NO_ALARM, AlarmCause.ALARM_CAUSE_OK);
        CAUSE_MAP.put(Status.HIHI_ALARM, AlarmCause.ALARM_CAUSE_HIHI);
        CAUSE_MAP.put(Status.HIGH_ALARM, AlarmCause.ALARM_CAUSE_HI);
        CAUSE_MAP.put(Status.LOW_ALARM, AlarmCause.ALARM_CAUSE_LO);
        CAUSE_MAP.put(Status.LOLO_ALARM, AlarmCause.ALARM_CAUSE_LOLO);

        SEVERITY_MAP.put(Severity.NO_ALARM, AlarmSeverity.ALARM_OK);
        SEVERITY_MAP.put(Severity.MINOR_ALARM, AlarmSeverity.ALARM_WARNING);
        SEVERITY_MAP.put(Severity.MAJOR_ALARM, AlarmSeverity.ALARM_FAILURE);
    }

    /**
     * Structure mapping Giapi Status Item -> Epics PV
     */
    private final Map<String, StatusChannelPair> channelMap = new HashMap<String, StatusChannelPair>();

    private final EpicsReader _reader;
    private final String xmlFileName;
    private final JmsProvider provider;

    private static final String NAME = "GMP_EPICS_TO_STATUS";
    public static final String PROP = "xmlFileName";

    public EpicsToStatusComponent(EpicsReader reader,
                                  JmsProvider provider,
                                  String xmlFileName) {
        _reader = reader;
        this.xmlFileName = xmlFileName;
        this.provider = provider;
    }

    public void initialize() throws JAXBException, SAXException {
        EpicsToStatusConfiguration conf = new EpicsToStatusConfiguration(xmlFileName);
        initialize(conf.getSimulatedChannels());
    }

    /**
     * Initialize the EpicsStatusService. Creates appropriate channels in an ChannelAccessServer.
     *
     * @param items channels to create and listen to.
     */
    private void initialize(Channels items) {
        for (final SimpleChannelType item : items.getSimpleChannelOrAlarmChannelOrHealthChannel()) {
            try {
                ReadOnlyClientEpicsChannel ch = _reader.getChannelAsync(item.getEpicschannel());
                StatusSetterImpl ss = new StatusSetterImpl(NAME + item.getStatusitem(), item.getStatusitem());
                try {
                    ss.startJms(provider);
                } catch (JMSException e) {
                    LOG.log(Level.SEVERE, "Won't be able to publish status updates for channel: " + item.getEpicschannel() + ", " + e.getMessage(), e);
                    ch.destroy();
                    continue;
                }
                channelMap.put(item.getEpicschannel(), new StatusChannelPair(ss, ch));
                try {
                    try {
                        //To give time for the channel to connect before registering the listener
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        LOG.log(Level.SEVERE, e.getMessage(), e);
                    }
                    if (item instanceof HealthChannelType) {
                        ch.registerListener(new ChannelListener<String>() {
                            @Override
                            public void valueChanged(String channelName, List<String> values) {
                                try {
                                    channelMap.get(channelName).statusSetter.setStatusItem(new HealthStatus(item.getStatusitem(), Health.valueOf(values.get(item.getIndex() != null ? item.getIndex() : 0))));
                                } catch (JMSException e) {
                                    LOG.log(Level.SEVERE, e.getMessage(), e);
                                } catch (IllegalArgumentException e) {
                                    LOG.log(Level.SEVERE, e.getMessage(), e);
                                }
                            }
                        });
                    } else if (item instanceof AlarmChannelType) {
                        ch.registerListener(new ChannelAlarmListener() {
                            @Override
                            public void valueChanged(String channelName, List values, Status status, Severity severity) {
                                try {
                                    AlarmSeverity sev = SEVERITY_MAP.get(severity);
                                    AlarmCause cause = CAUSE_MAP.get(status);
                                    channelMap.get(channelName).statusSetter.setStatusItem(new AlarmStatus(item.getStatusitem(),
                                            values.get(item.getIndex() != null ? item.getIndex() : 0),
                                            new AlarmState(sev != null ? sev : AlarmSeverity.ALARM_WARNING, cause != null ? cause : AlarmCause.ALARM_CAUSE_OTHER, status.getName())));
                                } catch (JMSException e) {
                                    LOG.log(Level.SEVERE, e.getMessage(), e);
                                }
                            }
                        });
                    } else/* (item instanceof SimpleChannelType)*/ {
                        ch.registerListener(new ChannelListener() {
                            @Override
                            public void valueChanged(String channelName, List values) {
                                try {
                                    channelMap.get(channelName).statusSetter.setStatusItem(new BasicStatus(item.getStatusitem(), values.get(item.getIndex() != null ? item.getIndex() : 0)));
                                } catch (JMSException e) {
                                    LOG.log(Level.SEVERE, e.getMessage(), e);
                                }
                            }
                        });
                    }
                    LOG.info("Successfully created status item publisher for EPICS channel: " + item.getEpicschannel());
                } catch (IllegalStateException ex) {
                    LOG.log(Level.SEVERE, "Couldn't register listener for channel: " + item.getEpicschannel() + ", " + ex.getMessage(), ex);
                    ch.destroy();
                    ss.stopJms();
                    channelMap.remove(item.getEpicschannel());
                }
            } catch (CAException ex) {
                LOG.log(Level.SEVERE, "Couldn't connect to channel: " + item.getEpicschannel() + ", " + ex.getMessage(), ex);
            } catch (EpicsException ex) {
                LOG.log(Level.SEVERE, "Couldn't connect to channel: " + item.getEpicschannel() + ", " + ex.getMessage(), ex);
            }
        }
    }


    public void shutdown() {
        for (StatusChannelPair pair : channelMap.values()) {
            pair.statusSetter.stopJms();
            try {
                pair.channel.destroy();
            } catch (CAException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
            } catch (IllegalStateException e) {
                LOG.log(Level.SEVERE, e.getMessage(), e);
            }
        }
        channelMap.clear();
    }
}
