package core.g2d;

public final class BytePack {
    private BytePack() {}

    public static int packShortToInt(short a, short b) {
        return ((a & 0xFFFF) << 16) | (b & 0xFFFF);
    }

    public static short unpackLeft(int packed) {
        return (short) (packed >> 16);
    }

    public static short unpackRight(int packed) {
        return (short) packed;
    }

    public static short toB16(float val) {return (short) (val * 65535.0f); }

    public static float fromB16toFloat32(short val) {
        return (Short.toUnsignedInt(val) / 65535.0f);
    }

    public static int packB16toInt32(short a, short b) {
        int uInt = Short.toUnsignedInt(a);
        int vInt = Short.toUnsignedInt(b);
        return ((vInt << 16) | uInt);
    }
}
