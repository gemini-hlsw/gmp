package edu.gemini.epics.impl;

import edu.gemini.epics.EpicsWriter;
import edu.gemini.epics.JCAContextController;
import edu.gemini.epics.ReadOnlyClientEpicsChannel;
import edu.gemini.epics.ReadWriteClientEpicsChannel;
import gov.aps.jca.CAException;

/**
 * Class EpicsWriterImpl
 *
 * @author Nicolas A. Barriga
 *         Date: 11/9/11
 */
public class EpicsWriterImpl extends EpicsChannelFactory implements EpicsWriter {

    public EpicsWriterImpl(JCAContextController epicsService) {
        super(epicsService);
    }

    @Override
    public ReadWriteClientEpicsChannel<Double> getDoubleChannel(String channelName) {
        return _getDoubleChannel(channelName);
    }

    @Override
    public ReadWriteClientEpicsChannel<Integer> getIntegerChannel(String channelName) {
        return _getIntegerChannel(channelName);
    }

    @Override
    public ReadWriteClientEpicsChannel<Short> getShortChannel(String channelName) {
        return _getShortChannel(channelName);
    }

    @Override
    public ReadWriteClientEpicsChannel<Float> getFloatChannel(String channelName) {
        return _getFloatChannel(channelName);
    }

    @Override
    public ReadWriteClientEpicsChannel<String> getStringChannel(String channelName) {
        return _getStringChannel(channelName);
    }

    @Override
    public <T extends Enum<T>> ReadWriteClientEpicsChannel<T> getEnumChannel(String channelName, Class<T> enumClass) {
        return _getEnumChannel(channelName, enumClass);
    }

    @Override
    public ReadWriteClientEpicsChannel<Short> getEnumAsShortChannel(String channelName) {
        return _getEnumAsShortChannel(channelName);
    }

    @Override
    public ReadWriteClientEpicsChannel<?> getChannelAsync(String channelName) {
        return _getChannelAsync(channelName);
    }

    @Override
    public void destroyChannel(ReadOnlyClientEpicsChannel<?> channel) throws CAException {
        _destroyChannel(channel);
    }

}
