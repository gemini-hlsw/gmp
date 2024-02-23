package edu.gemini.aspen.gmp.tcsoffset.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import edu.gemini.epics.EpicsException;
import edu.gemini.epics.EpicsWriter;
import edu.gemini.epics.ReadWriteClientEpicsChannel;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

import java.util.logging.Logger;

public class ChannelAccessString extends ChannelAccess<String>
{
    private static final Logger LOG = Logger.getLogger(ChannelAccessString.class.getName());
    @Override
    public void connect() {
        try{
            _epicsChannel = (ReadWriteClientEpicsChannel<String>) _ew1.getStringChannel(_caname);
        } catch (IllegalArgumentException | EpicsException e) {
            // This exception shouldn't affect the service to be deployed.
            LOG.warning("Epics Channel Access was not created. Name: "+ _caname +
                    ". Please check if the IOC is running or the EPICS server list");
            _epicsChannel=null;
        }
    }

    @Override
    public boolean check(JsonElement value) {
        return check(value.getAsString());

    }

    public boolean check (String value) {
        try {
            System.out.println("comparingString. " + _caname + ", value: " + _epicsChannel.getFirst() +
                    " - " + value + " - " + _epicsChannel.getFirst().equals(value));
            return _epicsChannel.getFirst().equals(value);
        } catch (CAException | TimeoutException e) {
            LOG.warning("Error getting the value of " +_caname + " epics channel access");
        }
        return false;
    }

    public ChannelAccessString(String caname, EpicsWriter ew1, int type) {
        super(caname, ew1, type);

    }

    @Override
    public void setValue (JsonElement val) throws CAException, TimeoutException {
        System.out.println("Setting (JsonPrimitive) as float " + val + " value in "+_caname );
        _epicsChannel.setValue(val.getAsString());
    }


    @Override
    public String getValues () throws CAException, TimeoutException {
        System.out.println("Getting string type ");
        return _epicsChannel.getFirst();
    }

}
