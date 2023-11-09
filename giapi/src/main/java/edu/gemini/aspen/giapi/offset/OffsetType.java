package edu.gemini.aspen.giapi.offset;

public enum OffsetType {
    ACQ(0, "acquisition"),
    SLOWGUIDING(1, "slowguiding");
    private final int _type;
    private String _text;

    private OffsetType(int type, String text ) {
        _type = type;
        _text = text;
    }

    public int getType() {return _type;}

    public String getText() { return _text;}
    public static OffsetType getFromInt(int nType) {
        System.out.println("OffseTypeeeeeeeeeeeeeeeeeee: "+nType);
        for (OffsetType e : OffsetType.values()) {
            if (e.getType() == nType) {
                System.out.println(e.getType() + " ==  " + nType);
                return e;
            }
        }
        throw new IllegalArgumentException("No Offset type with the id "+ nType);
    }

    public static boolean checkStr(String key) {
        for (OffsetType e : OffsetType.values())
            if (e.getText().equals(key))
                return true;
        return false;
    }
}
