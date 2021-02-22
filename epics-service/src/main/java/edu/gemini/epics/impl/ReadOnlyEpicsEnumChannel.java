package edu.gemini.epics.impl;

import com.cosylab.epics.caj.CAJChannel;
import edu.gemini.epics.api.DbrUtil;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jluhrs on 10/23/14.
 */
public class ReadOnlyEpicsEnumChannel<T extends Enum<T>> extends ReadOnlyEpicsChannelImpl<T> {
    public ReadOnlyEpicsEnumChannel(CAJChannel channel, Class<T> enumType, double timeout, int retries) {
        super(channel, timeout, retries);

        this.enumType = enumType;
    }

    private Class<T> enumType;

    @Override
    public List<T> mapValues(List<?> vals) {
        List<Short> valuesAsShorts = (List<Short>) vals;

        List<T> values =  new ArrayList<T>();
        // The enum type has the same constants that the EPICS enum channel ans in the same order, as verified in the
        // factory method.
        for (Short val : valuesAsShorts) {
            values.add(enumType.getEnumConstants()[val]);
        }

        return values;
    }
}
