package edu.gemini.epics.impl;

import edu.gemini.epics.EpicsReader;
import edu.gemini.epics.JCAContextController;
import edu.gemini.epics.ReadOnlyClientEpicsChannel;
import gov.aps.jca.CAException;

/**
 * Class EpicsReaderImpl
 *
 * @author Nicolas A. Barriga
 * Date: 11/7/11
 */
public class EpicsReaderImpl extends EpicsChannelFactory implements EpicsReader {

    public EpicsReaderImpl(JCAContextController epicsService) {
        super(epicsService);
    }

    @Override
    public ReadOnlyClientEpicsChannel<Double> getDoubleChannel(String channelName) {
        return _getDoubleChannel(channelName);
    }

    @Override
    public ReadOnlyClientEpicsChannel<Integer> getIntegerChannel(String channelName) {
        return _getIntegerChannel(channelName);
    }

    @Override
    public ReadOnlyClientEpicsChannel<Short> getShortChannel(String channelName) {
        return _getShortChannel(channelName);
    }

    @Override
    public ReadOnlyClientEpicsChannel<Float> getFloatChannel(String channelName) {
        return _getFloatChannel(channelName);

    }

    @Override
    public ReadOnlyClientEpicsChannel<String> getStringChannel(String channelName) {
        return _getStringChannel(channelName);

    }

    @Override
    public <T extends Enum<T>> ReadOnlyClientEpicsChannel<T> getEnumChannel(String channelName, Class<T> enumClass) {
        return _getEnumChannel(channelName, enumClass);
    }

    @Override
    public ReadOnlyClientEpicsChannel<Short> getEnumAsShortChannel(String channelName) {
        return _getEnumAsShortChannel(channelName);
    }

    @Override
    public ReadOnlyClientEpicsChannel<?> getChannelAsync(String channelName) {
        return _getChannelAsync(channelName);

    }

    @Override
    public void destroyChannel(ReadOnlyClientEpicsChannel<?> channel) throws CAException {
        _destroyChannel(channel);
    }

}
