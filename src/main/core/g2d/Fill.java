package core.g2d;

import core.util.Color;
import core.math.MathUtil;

import static core.Global.atlas;
import static core.Global.batch;

public final class Fill {
    private Fill() {
    }

    private static float lineWidth = 1f;

    private static float prevLineWidth = lineWidth;

    public static void lineWidth(float w) {
        prevLineWidth = lineWidth;
        lineWidth = w;
    }

    public static void resetLineWidth() {
        lineWidth = prevLineWidth;
    }

    public static void line(float x, float y, float x2, float y2, Color color) {
        line(x, y, x2, y2, lineWidth, color);
    }

    public static void line(float x, float y, float x2, float y2, float lineWidth, Color color) {

        float dx = x2 - x;
        float dy = y2 - y;
        float len = MathUtil.len(dx, dy);
        float kx = dx / len * lineWidth;
        float ky = dy / len * lineWidth;

        batch.rect(atlas.byPath("World/white.png"), color.rgba8888(), x - ky, y + kx, x + kx, y - kx, x2 + kx, y2 - kx, x2 - kx, y2 + kx);
    }

    public static void rectangleBorder(float x, float y, float width, float height, Color color) {
        rectangleBorder(x, y, width, height, lineWidth, color);
    }

    public static void rectangleBorder(float x, float y, float width, float height, float lineWidth, Color color) {
        // Upper border
        Fill.rect(x, y, width, lineWidth, color);
        // Right border
        Fill.rect(x + width - lineWidth, y + lineWidth, lineWidth, height - lineWidth * 2, color);
        // Down border
        Fill.rect(x, y + height - lineWidth, width, lineWidth, color);
        // Left border
        Fill.rect(x, y + lineWidth, lineWidth, height - lineWidth * 2, color);
    }

    public static void rect(float x, float y, float width, float height, Color color) {
        rect(x, y, width, height, color.rgba8888());
    }

    public static void rect(float x, float y, float width, float height, int rgba8888) {
        batch.draw(atlas.byPath("World/white"), rgba8888, x, y, width, height);
    }

    public static void circle(float x, float y, float radius, Color color) {
        batch.draw(atlas.byPath("World/circle"), color.rgba8888(), x, y, radius, radius);
    }
}
