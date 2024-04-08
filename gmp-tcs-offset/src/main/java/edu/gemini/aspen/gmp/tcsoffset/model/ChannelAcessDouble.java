package edu.gemini.aspen.gmp.tcsoffset.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import edu.gemini.epics.EpicsException;
import edu.gemini.epics.EpicsWriter;
import edu.gemini.epics.ReadWriteClientEpicsChannel;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

import java.util.logging.Logger;

public class ChannelAcessDouble extends ChannelAccess<Double>{

    private static final Logger LOG = Logger.getLogger(ChannelAcessDouble.class.getName());

    @Override
    public void connect() {
        try {
            _epicsChannel = (ReadWriteClientEpicsChannel<Double>) _ew1.getDoubleChannel(_caname);
        }
        catch (IllegalArgumentException | EpicsException e) {
            // This exception shouldn't affect the service to be deployed.
            LOG.warning("Epics Channel Access was not created. Name: "+ _caname +
                    ". Please check if the IOC is running or the EPICS server list");
            _epicsChannel=null;
        }
    }

    public ChannelAcessDouble(String caname, EpicsWriter ew1, int type) {
        super(caname, ew1, type);

    }

    @Override
    public void setValue (JsonElement val) throws CAException, TimeoutException {
        _epicsChannel.setValue(val.getAsDouble());
    }

    @Override
    public boolean check(JsonElement val) {
        try {
            return val.getAsDouble() == _epicsChannel.getFirst();
        } catch (CAException | TimeoutException e) {
            LOG.warning("Error getting the value of " +_caname + " epics channel access");
        }
        return false;
    }



    @Override
    public Double getValues () throws CAException, TimeoutException {
        System.out.println("Getting short value ");
        return _epicsChannel.getFirst();
    }

}
