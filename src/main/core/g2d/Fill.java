package core.g2d;

import core.Global;
import core.Utils.SimpleColor;

import static core.Global.atlas;
import static core.Global.batch;
import static org.lwjgl.opengl.GL11.glLineWidth;

public final class Fill {
    private Fill() {
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
        // TODO а вот нету. Надо писать спец. буфер для линий или изобретать отрисовку прямоугольников под углом
    }

    public static void rectangleBorder(int x, int y, int width, int height, int thickness, SimpleColor color) {
        glLineWidth(thickness);

        // Upper border
        Fill.rect(x, y, width, thickness, color);
        // Right border
        Fill.rect(x + width - thickness, y + thickness, thickness, height - thickness * 2, color);
        // Down border
        Fill.rect(x, y + height - thickness, width, thickness, color);
        // Left border
        Fill.rect(x, y + thickness, thickness, height - thickness * 2, color);
    }

    public static void rect(float x, float y, float width, float height, SimpleColor color) {
        batch.color(color);
        batch.draw(Global.atlas.byPath("World/white.png"), x, y, width, height);
        batch.resetColor();
    }

    public static void circle(float x, float y, float radius) {
        batch.draw(atlas.byPath("World/circle.png"), x, y, radius, radius);
    }
}
