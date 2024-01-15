package core.g2d;

import core.Utils.SimpleColor;
import core.math.MathUtil;

import static core.Global.atlas;
import static core.Global.batch;
import static org.lwjgl.opengl.GL11.glLineWidth;

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

    /* TODO не портировал, но и не используется
    public static void roundedRectangle(int x, int y, int width, int height, SimpleColor color) {
        int radius = height / 2;
        int SEGMENTS = 16;
        float ANGLE_INCREMENT = (float) (2.0 * Math.PI / SEGMENTS);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        glBegin(GL_TRIANGLE_FAN);

        glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        glVertex2f(x + radius, y + radius);

        float theta = (float) (Math.PI / 4.0);
        for (int i = 0; i <= SEGMENTS; i++) {
            float dx = radius * (float) Math.cos(theta);
            float dy = radius * (float) Math.sin(theta);
            glVertex2f(x + radius + dx, y + radius - dy);
            theta += ANGLE_INCREMENT;
        }
        glEnd();

        glBegin(GL_QUADS);
        glVertex2f(x + radius, y);
        glVertex2f(x + width - radius, y);
        glVertex2f(x + width - radius, y + height);
        glVertex2f(x + radius, y + height);
        glEnd();

        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(x + width - radius, y + radius);

        theta = (float) (Math.PI / 4.0);
        for (int i = 0; i <= SEGMENTS; i++) {
            float dx = radius * (float) Math.sin(theta);
            float dy = radius * (float) Math.cos(theta);
            glVertex2f(x + width - radius + dx, y + radius + dy);
            theta += ANGLE_INCREMENT;
        }
        glEnd();

        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(x + width - radius, y + height - radius);

        theta = (float) (Math.PI / 4.0);
        for (int i = 0; i <= SEGMENTS; i++) {
            float dx = radius * (float) Math.sin(theta);
            float dy = radius * (float) Math.cos(theta);
            glVertex2f(x + width - radius + dx, y + height - radius - dy);
            theta += ANGLE_INCREMENT;
        }
        glEnd();

        glBegin(GL_TRIANGLE_FAN);
        glVertex2f(x + radius, y + height - radius);

        theta = (float) (Math.PI / 4.0);
        for (int i = 0; i <= SEGMENTS; i++) {
            float dx = radius * (float) Math.cos(theta);
            float dy = radius * (float) Math.sin(theta);
            glVertex2f(x + radius - dx, y + height - radius - dy);
            theta += ANGLE_INCREMENT;
        }

        glColor4f(1, 1, 1, 1);
        glEnd();
    }
     */

    public static void line(float x, float y, float x2, float y2) {
        line(x, y, x2, y2, lineWidth);
    }

    public static void line(float x, float y, float x2, float y2, float lineWidth) {

        float dx = x2 - x;
        float dy = y2 - y;
        float len = MathUtil.len(dx, dy);
        float kx = dx / len * lineWidth;
        float ky = dy / len * lineWidth;

        batch.rect(atlas.byPath("World/white.png"), x - ky, y + kx, x + kx, y - kx, x2 + kx, y2 - kx, x2 - kx, y2 + kx);
    }

    public static void rectangleBorder(float x, float y, float width, float height, SimpleColor color) {
        rectangleBorder(x, y, width, height, lineWidth, color);
    }

    public static void rectangleBorder(float x, float y, float width, float height, float lineWidth, SimpleColor color) {
        // Upper border
        Fill.rect(x, y, width, lineWidth, color);
        // Right border
        Fill.rect(x + width - lineWidth, y + lineWidth, lineWidth, height - lineWidth * 2, color);
        // Down border
        Fill.rect(x, y + height - lineWidth, width, lineWidth, color);
        // Left border
        Fill.rect(x, y + lineWidth, lineWidth, height - lineWidth * 2, color);
    }

    public static void rect(float x, float y, float width, float height, SimpleColor color) {
        batch.color(color);
        batch.draw(atlas.byPath("World/white.png"), x, y, width, height);
        batch.resetColor();
    }

    public static void circle(float x, float y, float radius) {
        batch.draw(atlas.byPath("World/circle.png"), x, y, radius, radius);
    }
}
