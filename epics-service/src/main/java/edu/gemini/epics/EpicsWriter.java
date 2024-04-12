package edu.gemini.epics;

/**
 * Interface EpicsWriter
 *
 * @author Nicolas A. Barriga
 *         Date: 11/9/11
 */
public interface EpicsWriter extends EpicsReader {
    @Override
    ReadWriteClientEpicsChannel<Double> getDoubleChannel(String channelName);

    @Override
    ReadWriteClientEpicsChannel<Integer> getIntegerChannel(String channelName);

    @Override
    ReadWriteClientEpicsChannel<Short> getShortChannel(String channelName);

    @Override
    ReadWriteClientEpicsChannel<Float> getFloatChannel(String channelName);

    @Override
    ReadWriteClientEpicsChannel<String> getStringChannel(String channelName);

    @Override
    <T extends Enum<T>> ReadWriteClientEpicsChannel<T> getEnumChannel(String channelName, Class<T> enumClass);

    @Override
    ReadWriteClientEpicsChannel<Short> getEnumAsShortChannel(String channelName);

    @Override
    ReadWriteClientEpicsChannel<?> getChannelAsync(String channelName);
}
