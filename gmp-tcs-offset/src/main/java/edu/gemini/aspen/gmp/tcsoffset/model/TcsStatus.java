package edu.gemini.aspen.gmp.tcsoffset.model;

/*
 * The TCSSTATUS enum make reference to the binary output record
 * on the TCS (<tcsTop>:ErrorVal.VAL) 
 */

public enum TcsStatus {
    OK(0,"OK"),
    ERR(1, "ERROR");

    private final int _val;

    private final String _str;

    private TcsStatus(int val, String str) {_val = val; _str = str;}

    public int getVal() {return _val;}

    public String getStr() {return _str;}

    public static TcsStatus getFromInt(int val) {
        for (TcsStatus e : TcsStatus.values()) {
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
