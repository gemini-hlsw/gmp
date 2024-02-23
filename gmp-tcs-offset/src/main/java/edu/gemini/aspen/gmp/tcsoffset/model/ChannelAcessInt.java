package edu.gemini.aspen.gmp.tcsoffset.model;

import com.google.gson.JsonElement;
import edu.gemini.epics.EpicsException;
import edu.gemini.epics.EpicsWriter;
import edu.gemini.epics.ReadWriteClientEpicsChannel;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

import java.util.logging.Logger;

public class ChannelAcessInt extends ChannelAccess<Integer>{

    private static final Logger LOG = Logger.getLogger(ChannelAcessInt.class.getName());

    @Override
    public void connect() {
        try {
            _epicsChannel = (ReadWriteClientEpicsChannel<Integer>) _ew1.getIntegerChannel(_caname);
        } catch (IllegalArgumentException | EpicsException e) {
            // This exception shouldn't affect the service to be deployed.
            LOG.warning("Epics Channel Access was not created. Name: "+ _caname +
                ". Please check if the IOC is running or the EPICS server list");
            _epicsChannel=null;
        }
    }

    public ChannelAcessInt(String caname, EpicsWriter ew1, int type) {
        super(caname, ew1, type);

    }

    @Override
    public void setValue (JsonElement val) throws CAException, TimeoutException {
        System.out.println("Setting (JsonPrimitive) as float " + val + " value in "+_caname );
        _epicsChannel.setValue(val.getAsInt());
    }

    @Override
    public boolean check(JsonElement val) {
        try {
            return val.getAsShort() == _epicsChannel.getFirst();
        } catch (CAException | TimeoutException e) {
            LOG.warning("Error getting the value of " +_caname + " epics channel access");
        }
        return false;
    }


    @Override
    public Integer getValues () throws CAException, TimeoutException {
        System.out.println("Getting short value ");
        return _epicsChannel.getFirst();
    }

}
