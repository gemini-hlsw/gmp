package edu.gemini.aspen.gmp.tcsoffset.model;

/*
 * This enum CARSTATE make reference to the states of a CAR record. 
 * 
 */

public enum CARSTATE {
        IDLE(0,"IDLE"),
        PAUSED(1, "PAUSED"),
        BUSY(2, "BUSY"),
        ERROR(3, "ERROR");

        private final int _val;
        
	private final String _str;
        
	private CARSTATE(int val, String str) {_val = val; _str = str;}
        
	public int getVal() {return _val;}
        
	public String getStr() {return _str;}

        public static edu.gemini.aspen.gmp.tcsoffset.model.CARSTATE getFromInt(int val) {
            for (edu.gemini.aspen.gmp.tcsoffset.model.CARSTATE e : edu.gemini.aspen.gmp.tcsoffset.model.CARSTATE.values()) {
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
