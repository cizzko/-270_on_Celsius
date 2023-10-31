package core.Utils;

public class SimpleColor {
    int value;

    public SimpleColor(int r, int g, int b, int a) {
        value = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | ((b & 0xFF));
    }

    public int getRed() {
        return (value >> 16) & 0xFF;
    }

    public int getGreen() {
        return (value >> 8) & 0xFF;
    }

    public int getBlue() {
        return (value) & 0xFF;
    }

    public int getAlpha() {
        return (value >> 24) & 0xff;
    }
}
