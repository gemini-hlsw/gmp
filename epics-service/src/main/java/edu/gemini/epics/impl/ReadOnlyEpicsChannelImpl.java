package edu.gemini.epics.impl;

import com.cosylab.epics.caj.CAJChannel;
import edu.gemini.epics.ReadOnlyClientEpicsChannel;
import edu.gemini.epics.api.ChannelAlarmListener;
import edu.gemini.epics.api.ChannelListener;
import edu.gemini.epics.api.DbrUtil;
import edu.gemini.epics.api.EpicsListener;
import gov.aps.jca.CAException;
import gov.aps.jca.Channel;
import gov.aps.jca.Monitor;
import gov.aps.jca.TimeoutException;
import gov.aps.jca.dbr.*;
import gov.aps.jca.event.MonitorEvent;
import gov.aps.jca.event.MonitorListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Class ReadOnlyEpicsChannelImpl
 *
 * @author Nicolas A. Barriga
 *         Date: 11/7/11
 */
public class ReadOnlyEpicsChannelImpl<T> implements ReadOnlyClientEpicsChannel<T> {
    private static final Logger LOG = Logger.getLogger(ReadOnlyEpicsChannelImpl.class.getName());
    protected final CAJChannel channel;
    protected double timeout;
    protected int retries;
    private final Map<EpicsListener<?>, MonitorListenerPair> listeners = new HashMap<EpicsListener<?>, MonitorListenerPair>();

    public ReadOnlyEpicsChannelImpl(CAJChannel channel, double timeout, int retries) {
        this.channel = channel;
        this.timeout = timeout;
        this.retries = retries;
    }

    @Override
    public DBR getDBR() throws CAException, TimeoutException {
        DBR dbr = null;
        boolean success = false;
        for(int i = 0; !success && i<=retries; i++) {
            try {
                synchronized (channel.getContext()) {
                    dbr = channel.get();
                    channel.getContext().pendIO(timeout);
                }
                success = true;
            } catch(CAException e) {
                if (i == retries) {
                    LOG.warning("Channel " + channel.getName() + " read failed, pendIO timeout");
                    throw e;
                }
                else Thread.yield();
            }
            if(success && i > 0)
                LOG.warning("Channel " + channel.getName() + " read after " + i + " retries");
        }
        return dbr;
    }

    @Override
    public List<T> getAll() throws CAException, TimeoutException {
        return mapValues(DbrUtil.extractValues(getDBR()));
    }

    @Override
    public T getFirst() throws CAException, TimeoutException {
        return getAll().get(0);
    }

    @Override
    public String getName() {
        return channel.getName();
    }

    @Override
    public synchronized void registerListener(final ChannelListener<T> tChannelListener) throws CAException {
        MonitorListener ml = new MonitorListener() {
            @Override
            public void monitorChanged(MonitorEvent ev) {
                tChannelListener.valueChanged(channel.getName(), mapValues(DbrUtil.extractValues(ev.getDBR())));
            }
        };
        listeners.put(tChannelListener, new MonitorListenerPair(channel.addMonitor(Monitor.VALUE, ml), ml));
        channel.getContext().flushIO();
    }

    @Override
    public synchronized void unRegisterListener(ChannelListener<T> tChannelListener) throws CAException {
        if (listeners.containsKey(tChannelListener)) {
            Monitor mon = listeners.get(tChannelListener).monitor;
            MonitorListener ml = listeners.get(tChannelListener).listener;
            mon.removeMonitorListener(ml);
            if (mon.getMonitorListeners().length == 0) {
                mon.clear();
            }
        }
    }

    @Override
    public void registerListener(final ChannelAlarmListener<T> tChannelAlarmListener) throws CAException {
        MonitorListener ml = new MonitorListener() {
            @Override
            public void monitorChanged(MonitorEvent ev) {
                tChannelAlarmListener.valueChanged(channel.getName(),
                        mapValues(DbrUtil.extractValues(ev.getDBR())),
                        ev.getDBR().isSTS() ? ((STS) ev.getDBR()).getStatus() : Status.NO_ALARM,
                        ev.getDBR().isSTS() ? ((STS) ev.getDBR()).getSeverity() : Severity.NO_ALARM);
            }
        };
        listeners.put(tChannelAlarmListener, new MonitorListenerPair(channel.addMonitor(Monitor.VALUE, ml), ml));
        channel.getContext().flushIO();
    }

    @Override
    public void unRegisterListener(ChannelAlarmListener<T> tChannelAlarmListener) throws CAException {
        if (listeners.containsKey(tChannelAlarmListener)) {
            Monitor mon = listeners.get(tChannelAlarmListener).monitor;
            MonitorListener ml = listeners.get(tChannelAlarmListener).listener;
            mon.removeMonitorListener(ml);
            if (mon.getMonitorListeners().length == 0) {
                mon.clear();
            }
        }
    }

    @Override
    public boolean isValid() {
        return channel.getConnectionState() == Channel.ConnectionState.CONNECTED;
    }

    @Override
    public DBRType getType() {
        return channel.getFieldType();
    }

    @Override
    public void destroy() throws CAException {
        channel.destroy();
        listeners.clear();
    }

    protected List<T> mapValues(List<?> vals) {
        return (List<T>) vals;
    }
}
