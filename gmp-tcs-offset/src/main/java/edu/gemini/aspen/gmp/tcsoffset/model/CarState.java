package edu.gemini.aspen.gmp.tcsoffset.model;
/*
 * This enum CarState make reference to the states of a CAR record.
 *
 */

public enum CarState {
    IDLE(0,"IDLE"),
    PAUSED(1, "PAUSED"),
    BUSY(2, "BUSY"),
    ERROR(3, "ERROR");

    private final int _val;

    private final String _str;

    private CarState(int val, String str) {_val = val; _str = str;}

    public int getVal() {return _val;}

    public String getStr() {return _str;}

    public static edu.gemini.aspen.gmp.tcsoffset.model.CarState getFromInt(int val) {
        for (edu.gemini.aspen.gmp.tcsoffset.model.CarState e : edu.gemini.aspen.gmp.tcsoffset.model.CarState.values()) {
            if (e.getVal() == val)
                return e;
        }
        throw new IllegalArgumentException("No Offset type with the id "+ val);
    }

    @Override
    public String toString() {
        return _str;
    }

}