package edu.gemini.epics;

import gov.aps.jca.CAException;

/**
 * Interface to access an epics channel
 * <p/>
 * The implementation takes care of bind/unbind of the channel
 */
public interface EpicsReader {
    /**
     * Returns a representation of a channel. It is recommended not to hold instances of
     * ReadOnlyEpicsChannelImpl as channels can disappear anytime
     *
     * @param channelName The name of the channel
     * @return An ReadOnlyEpicsChannelImpl representation of a channel or a NullEpicsChannel if the channel is not available
     */
    ReadOnlyClientEpicsChannel<Double> getDoubleChannel(String channelName);

    ReadOnlyClientEpicsChannel<Integer> getIntegerChannel(String channelName);

    ReadOnlyClientEpicsChannel<Short> getShortChannel(String channelName);

    ReadOnlyClientEpicsChannel<Float> getFloatChannel(String channelName);

    ReadOnlyClientEpicsChannel<String> getStringChannel(String channelName);

    <T extends Enum<T>> ReadOnlyClientEpicsChannel<T> getEnumChannel(String channelName, Class<T> enumClass);

    ReadOnlyClientEpicsChannel<Short> getEnumAsShortChannel(String channelName);

    ReadOnlyClientEpicsChannel<?> getChannelAsync(String channelName);

    void destroyChannel(ReadOnlyClientEpicsChannel<?> channel) throws CAException;
}
