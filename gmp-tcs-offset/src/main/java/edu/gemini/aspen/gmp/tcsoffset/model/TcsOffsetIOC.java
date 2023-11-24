package edu.gemini.aspen.gmp.tcsoffset.model;

import edu.gemini.aspen.giapi.offset.OffsetType;
import gov.aps.jca.CAException;
import gov.aps.jca.TimeoutException;

import javax.jms.Destination;

/**
 * Interface which defines the setTcsOffset which is used to apply
 * a P and Q offsets on the TCS.
 */
public interface TcsOffsetIOC {

    void setTcsOffset(double p, double q, OffsetType offsetType) throws TcsOffsetException;

}
