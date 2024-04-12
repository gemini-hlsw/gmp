package edu.gemini.aspen.gmp.tcsoffset.model;

import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

public interface GenericChannelAccess<T> {
    void setValues(T values) throws CAException, TimeoutException;
    T getValues() throws CAException, TimeoutException;
}
