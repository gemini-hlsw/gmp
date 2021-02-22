package edu.gemini.epics.impl;

import com.cosylab.epics.caj.CAJChannel;
import com.google.common.collect.ImmutableList;
import edu.gemini.epics.ReadWriteClientEpicsChannel;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

import java.util.List;

/**
 * Created by jluhrs on 10/23/14.
 */
public class ReadWriteEpicsEnumChannel<T extends Enum<T>> extends ReadOnlyEpicsEnumChannel<T>
        implements ReadWriteClientEpicsChannel<T> {

    public ReadWriteEpicsEnumChannel(CAJChannel channel, Class<T> enumType, double timeout, int readRetries) {
        super(channel, enumType, timeout, readRetries);
    }

    @Override
    public void setValue(T value) throws CAException, TimeoutException {
        setValue(ImmutableList.of(value));
    }

    @Override
    public void setValue(List<T> values) throws CAException, TimeoutException {
        String arr[] = new String[values.size()];
        for (int i = 0; i < values.size(); i++) {
            arr[i] = values.get(i).toString();
        }
        synchronized (channel.getContext()) {
            channel.put(arr);

            channel.getContext().flushIO();
        }
    }
}
