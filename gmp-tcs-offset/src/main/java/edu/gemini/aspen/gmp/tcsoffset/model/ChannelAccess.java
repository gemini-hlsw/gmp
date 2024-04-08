package edu.gemini.aspen.gmp.tcsoffset.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import edu.gemini.epics.EpicsWriter;
import edu.gemini.epics.ReadWriteClientEpicsChannel;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

import java.util.logging.Logger;

public abstract class ChannelAccess<T>  {
    
    private static final Logger LOG = Logger.getLogger(ChannelAccess.class.getName());
    
    protected String _caname;

    protected ReadWriteClientEpicsChannel<T> _epicsChannel = null;

    // type
    //   - 0: Short, 1: Integer, 2: Float, 3: Double, 4: String, 5: Enum
    private int _type;

    private boolean _mark;

    protected EpicsWriter _ew1;

    public abstract void connect();

    public ChannelAccess(String caname, EpicsWriter ew1, int type) {
        _caname = caname;
        _type   = type;
        _mark   = false;
        _ew1    = ew1;
    }

    public String getCAname() {
        return _caname;
    }

    public void setCAname(String _caname) {
        this._caname = _caname;
    }

    public int getType() {
        return _type;
    }

    public void setType(short _type) {
        this._type = _type;
    }

    public boolean isMark() {
        return _mark;
    }

    public void setMark(boolean _mark) {
        this._mark = _mark;
    }

    public void setValue (T val) throws CAException, TimeoutException {
        _epicsChannel.setValue(val);
    }

    public abstract void setValue (JsonElement val) throws CAException, TimeoutException;

    public abstract boolean check(JsonElement val);

    public <T> boolean check(T value)  {
        try {
            return value == _epicsChannel.getFirst();
        } catch (CAException | TimeoutException e) {
            LOG.warning("Error getting the value of " +_caname + " epics channel access");
        }
        return false;
    }

    public void setValues(T values) throws CAException, TimeoutException {
        _epicsChannel.setValue(values);
    }

    public T getValues () throws CAException, TimeoutException {
        System.out.println("General ChannelAccess");
        return _epicsChannel.getFirst();
    }

}
