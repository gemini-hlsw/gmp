package edu.gemini.epics.impl;

import com.cosylab.epics.caj.CAJChannel;
import com.google.common.collect.ImmutableList;
import edu.gemini.epics.ReadWriteClientEpicsChannel;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

import java.util.ArrayList;
import java.util.List;

/**
 * Class ReadWriteEpicsChannelImpl
 *
 * @author Nicolas A. Barriga
 *         Date: 11/9/11
 */
public class ReadWriteEpicsChannelImpl<T> extends ReadOnlyEpicsChannelImpl<T> implements ReadWriteClientEpicsChannel<T> {
    public ReadWriteEpicsChannelImpl(CAJChannel channel, double timeout, int retries) {
        super(channel, timeout, retries);
    }

    @Override
    public void setValue(T value) throws CAException, TimeoutException {
        setValue(ImmutableList.of(value));
    }

    @Override
    public void setValue(List<T> values) throws CAException, TimeoutException {
        synchronized (channel.getContext()) {
            if (getType().isDOUBLE()) {
                List<Double> list = (List<Double>) values;
                double arr[] = new double[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    arr[i] = list.get(i);
                }
                channel.put(arr);
            } else if (getType().isFLOAT()) {
                List<Float> list = (List<Float>) values;
                float arr[] = new float[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    arr[i] = list.get(i);
                }
                channel.put(arr);
            } else if (getType().isINT()) {
                List<Integer> list = (List<Integer>) values;
                int arr[] = new int[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    arr[i] = list.get(i);
                }
                channel.put(arr);
            } else if (getType().isSHORT()) {
                List<Short> list = (List<Short>) values;
                short arr[] = new short[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    arr[i] = list.get(i);
                }
                channel.put(arr);
            } else if (getType().isSTRING()) {
                channel.put(values.toArray(new String[values.size()]));
            } else {
                throw new UnsupportedOperationException("Only EPICS channels of types Integer, Float, Double and String are supported at this time.");
            }

            channel.getContext().flushIO();
        }
    }
}
