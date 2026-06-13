package core.ui;

public enum Align {
    CENTER,
    BOTTOM,
    TOP,
    LEFT,
    RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_LEFT,
    TOP_RIGHT;

    public static final Align[] TABLE = values();

    public byte id() { return (byte) ordinal(); }
}
