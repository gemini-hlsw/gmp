package edu.gemini.aspen.gmp.tcsoffset.model;

import edu.gemini.epics.ReadWriteClientEpicsChannel;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

public class EpicsCA<T> {
    private ReadWriteClientEpicsChannel<T> epicsChannel_;


    public EpicsCA(ReadWriteClientEpicsChannel<T> epicsCA) {
        epicsChannel_ = epicsCA;
    }


    public ReadWriteClientEpicsChannel<T> getEpicsChannel_() {
        return epicsChannel_;
    }

    public void setEpicsChannel_(ReadWriteClientEpicsChannel<T> epicsChannel_) {
        this.epicsChannel_ = epicsChannel_;
    }

    public void setValue(T val) {
        try {
            epicsChannel_.setValue(val);
        } catch (CAException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public T getFirst() throws CAException, TimeoutException {
        return epicsChannel_.getFirst();
    }
}
