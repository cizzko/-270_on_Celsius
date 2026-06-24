package core.g2d;

import core.graphic.Color;
import core.math.MathUtil;

import static core.Global.atlas;

public final class Fill {
    private Fill() {
    }

    public static final String WHITE_RECT   = "World/white";
    public static final String WHITE_CIRCLE = "World/circle";

    private static float lineWidth = 1f;

    private static float prevLineWidth = lineWidth;

    public static final Atlas.Region cachedRect = atlas.get(WHITE_RECT), cachedCircle = atlas.get(WHITE_CIRCLE);

    public static void lineWidth(float w) {
        prevLineWidth = lineWidth;
        lineWidth = w;
    }

    public static void resetLineWidth() {
        lineWidth = prevLineWidth;
    }

    public static void line(float x, float y, float x2, float y2, Color color) {
        line(x, y, x2, y2, lineWidth, color.rgba8888());
    }

    public static void line(float x, float y, float x2, float y2, int rgba8888) {
        line(x, y, x2, y2, lineWidth, rgba8888);
    }

    public static void line(float x, float y, float x2, float y2, float lineWidth, int rgba8888) {

        float dx = x2 - x;
        float dy = y2 - y;
        float len = MathUtil.len(dx, dy);
        float kx = dx / len * lineWidth;
        float ky = dy / len * lineWidth;

        StackfulRender.rect(cachedRect, rgba8888,
                x - ky, y + kx,
                x + kx, y - kx,
                x2 + kx, y2 - kx,
                x2 - kx, y2 + kx);
    }

    public static void rectangleBorder(float x, float y, float width, float height, Color color) {
        rectangleBorder(x, y, width, height, lineWidth, color.rgba8888());
    }

    public static void rectangleBorder(float x, float y, float width, float height, int rgba8888) {
        rectangleBorder(x, y, width, height, lineWidth, rgba8888);
    }

    public static void rectangleBorder(float x, float y, float width, float height, float lineWidth, int rgba8888) {
        // Upper border
        Fill.rect(x, y, width, lineWidth, rgba8888);
        // Right border
        Fill.rect(x + width - lineWidth, y + lineWidth, lineWidth, height - lineWidth * 2, rgba8888);
        // Down border
        Fill.rect(x, y + height - lineWidth, width, lineWidth, rgba8888);
        // Left border
        Fill.rect(x, y + lineWidth, lineWidth, height - lineWidth * 2, rgba8888);
    }

    public static void rect(float x, float y, float width, float height, Color color) {
        rect(x, y, width, height, color.rgba8888());
    }

    public static void rect(float x, float y, float width, float height, int rgba8888) {
        StackfulRender.draw(cachedRect, rgba8888, x, y, width, height);
    }

    public static void circle(float x, float y, float radius, Color color) {
        StackfulRender.draw(cachedCircle, color.rgba8888(), x, y, radius, radius);
    }
}
