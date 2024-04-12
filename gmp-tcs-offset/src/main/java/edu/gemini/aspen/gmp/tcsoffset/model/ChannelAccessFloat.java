package edu.gemini.aspen.gmp.tcsoffset.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import edu.gemini.epics.EpicsException;
import edu.gemini.epics.EpicsWriter;
import edu.gemini.epics.ReadWriteClientEpicsChannel;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

import java.util.logging.Logger;

public class ChannelAccessFloat extends ChannelAccess<Float>
{
    private static final Logger LOG = Logger.getLogger(ChannelAccessFloat.class.getName());

    @Override
    public void connect() {
        try {
            _epicsChannel = (ReadWriteClientEpicsChannel<Float>) _ew1.getFloatChannel(_caname);
        } catch (IllegalArgumentException | EpicsException e) {
            // This exception shouldn't affect the service to be deployed.
            LOG.warning("Epics Channel Access was not created. Name: "+ _caname +
                    ". Please check if the IOC is running or the EPICS server list");
            _epicsChannel=null;
        }
    }

    public ChannelAccessFloat(String caname, EpicsWriter ew1, int type) {
        super(caname, ew1, type);
    }

    
    @Override
    public boolean check(JsonElement val) {
        try {
            return val.getAsFloat() == _epicsChannel.getFirst();
        } catch (CAException | TimeoutException e) {
            LOG.warning("Error getting the value of " +_caname + " epics channel access");
        }
        return false;
    }

    @Override
    public void setValue (JsonElement val) throws CAException, TimeoutException {
        _epicsChannel.setValue(val.getAsFloat());
    }

}
