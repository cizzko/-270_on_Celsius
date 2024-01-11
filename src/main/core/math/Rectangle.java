package core.math;

public final class Rectangle {
    // TODO кода нет, да и не надо пока. Лень писать

    public static boolean contains(int x, int y, int width, int height,
                                   int px, int py) {
        return x <= px && x + width >= px && y <= py && y + height >= py;
    }

    public static boolean contains(int x, int y, int width, int height, Point2i point) {
        return contains(x, y, width, height, point.x, point.y);
    }
}
