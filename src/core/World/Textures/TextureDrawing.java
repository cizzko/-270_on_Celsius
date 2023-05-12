package core.World.Textures;

import core.EventHandling.MouseScrollCallback;
import core.GUI.Fonts;
import core.GUI.objects.ButtonObject;
import core.GUI.objects.PanelObject;
import core.GUI.objects.SliderObject;
import core.GUI.Video;
import core.Logging.config;
import core.Window;
import core.World.WorldGenerator;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Map;
import static core.GUI.CreateElement.*;
import static core.GUI.Video.*;
import static org.lwjgl.opengl.GL13.*;

public class TextureDrawing {
    private static int accumulator = 0;
    private static int spacingBetweenLetters = Integer.parseInt(config.jetFromConfig("SpacingBetweenLetters"));
    public static StaticWorldObjects[][] StaticObjects = WorldGenerator.StaticObjects;
    public static DynamicWorldObjects[] DynamicObjects = WorldGenerator.DynamicObjects;

    public static void drawTexture(String path, int x, int y, float zoom) {
        glPushMatrix();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glLoadIdentity();
        glTranslatef(-DynamicObjects[0].x * zoom + Window.width / 2f - 32, -DynamicObjects[0].y, 0);
        glMultMatrixf(new float[] {zoom + (float)(zoom + MouseScrollCallback.getScroll()) / 10, 0, 0, 0, 0, zoom + (float)(zoom + MouseScrollCallback.getScroll()) / 10, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1});

        ByteBuffer buffer = TextureLoader.ByteBufferEncoder(path);
        BufferedImage image = TextureLoader.BufferedImageEncoder(path);

        // параметры, бинд текстур, и прочее
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        int width = image.getWidth();
        int height = image.getHeight();

        if (width < height) {
            width = image.getHeight();
            height = image.getWidth();
        }

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        // верхний левый угол
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1);
        glVertex2f(x, y);
        // верхний правый угол
        glTexCoord2f(1, 1);
        glVertex2f(x + width, y);
        // нижний правый угол
        glTexCoord2f(1, 0);
        glVertex2f(x + width, y + height);
        // нижний левый угол
        glTexCoord2f(0, 0);
        glVertex2f(x, y + height);

        //glVertex2i Задает вершины
        //glTexCoord2i Задает текущие координаты текстуры

        glEnd();
        glDisable(GL_TEXTURE_2D);
        glPopMatrix();
    }

    public static void drawTexture(int x, int y, int width, int height, ByteBuffer buffer, Color color) {
        glPushMatrix();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glLoadIdentity();

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

//        if (width < height) {
//            int buff;
//            buff = width;
//            width = height;
//            height = buff;
//        }

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        glBegin(GL_QUADS);
        glTexCoord2f(0, 1);
        glVertex2f(x, y);

        glTexCoord2f(1, 1);
        glVertex2f(x + width, y);

        glTexCoord2f(1, 0);
        glVertex2f(x + width, y + height);

        glTexCoord2f(0, 0);
        glVertex2f(x, y + height);

        glEnd();
        glDisable(GL_TEXTURE_2D);
        glPopMatrix();
    }

    public static void drawText(int x, int y, String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ') {
                // ширина 'A' (eng, caps) принята за ширину пробела
                x += Fonts.letterSize.get('A').width;
                continue;
            }
            TextureDrawing.drawTexture(x, y, Fonts.letterSize.get(ch).width, Fonts.letterSize.get(ch).height, Fonts.chars.get(ch), new Color(210, 210, 210, 255));
            x += Fonts.letterSize.get(ch).width * (spacingBetweenLetters / 10);
        }
    }

    public static void drawRectangle(int x, int y, int width, int height, Color color) {
        glPushMatrix();
        glBegin(GL_QUADS);
        glEnable(GL_TEXTURE_2D);

        glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glColor3f(1, 1, 1);

        glEnd();
        glPopMatrix();
    }

    public static void drawCutRectangle(float x, float y, float width, float height, float cutSize, Color color) {
        float d = Math.min(cutSize, Math.min(width, height));
        float dx = d * (float) Math.cos(Math.PI / 4.0);
        float dy = d * (float) Math.sin(Math.PI / 4.0);

        glBegin(GL_POLYGON);
        glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        glVertex2f(x + dx, y);
        glVertex2f(x + width - dx, y);

        glVertex2f(x + width - dx, y);
        glVertex2f(x + width, y + dy);

        glVertex2f(x + width, y + dy);
        glVertex2f(x + width, y + height - dy);

        glVertex2f(x + width, y + height - dy);
        glVertex2f(x + width - dx, y + height);

        glVertex2f(x + width - dx, y + height);
        glVertex2f(x + dx, y + height);

        glVertex2f(x + dx, y + height);
        glVertex2f(x, y + height - dy);

        glVertex2f(x, y + height - dy);
        glVertex2f(x, y + dy);

        glVertex2f(x, y + dy);
        glVertex2f(x + dx, y);
        glColor4f(1, 1, 1, 1);

        glEnd();
    }

    public static void drawRoundedRectangle(int x, int y, int width, int height, Color color) {
        int radius = height / 2;
        int SEGMENTS = 16;
        float ANGLE_INCREMENT = (float) (2.0 * Math.PI / SEGMENTS);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_TEXTURE_2D);

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

    public static void drawCircle(int x, int y, float radius, Color color) {
        int samples = 64;
        glPushMatrix();
        glBegin(GL_TRIANGLE_FAN);
        glEnable(GL_TEXTURE_2D);

        glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        glVertex2f(x, y);

        for (int i = 0; i <= samples; i++) {
            float angle = (float) (i * 2 * Math.PI / samples);
            float dx = (float) (radius * Math.cos(angle));
            float dy = (float) (radius * Math.sin(angle));
            glVertex2f(x + dx, y + dy);
        }
        glColor4f(1f, 1f, 1f, 1f);

        glEnd();
        glPopMatrix();
    }

    public static void updateVideo() {
        ByteBuffer buff = null;
        for (Map.Entry<String, Video> entry : video.entrySet()) {
            String name = entry.getKey();
            Video video = entry.getValue();

            if (video != null && video.isPlaying) {
                if (video.frame == video.totalFrames) {
                    video.frame = 1;
                }
                if (byteBuffer.get(name) != null && !byteBuffer.get(name).equals(buff)) {
                    drawTexture(video.x, video.y, video.width, video.height, byteBuffer.get(name), new Color(255, 255, 255, 255));
                    buff = byteBuffer.get(name);
                }
            }
        }
    }

    public static void updateStaticObj() {
        float left = DynamicObjects[0].x - 1920 / 3f;
        float right = DynamicObjects[0].x + 1920 / 3f;
        float top = DynamicObjects[0].y - 1080 / 3f;
        float bottom = DynamicObjects[0].x + 1080 / 3f;

        for (int x = 0; x < StaticObjects.length - 1; x++) {
            for (int y = 0; y < StaticObjects[x].length - 1; y++) {
                StaticObjects[x][y].onCamera = !(StaticObjects[x][y].x < left) && !(StaticObjects[x][y].x > right) && !(StaticObjects[x][y].y < top) && !(StaticObjects[x][y].y > bottom);
                if (StaticObjects[x][y].onCamera) {
                    drawTexture(StaticObjects[x][y].path, (int) StaticObjects[x][y].x, (int) StaticObjects[x][y].y, 3);
                }
            }
        }
    }

    public static void updateDynamicObj() {
        accumulator += Window.deltaTime;

        for (int i = 0; i < DynamicObjects.length; i++) {
            if (DynamicObjects[i] != null && DynamicObjects[i].onCamera && DynamicObjects[i].framesCount == 1) {
                drawTexture(DynamicObjects[i].path, (int) DynamicObjects[i].x, (int) DynamicObjects[i].y, 3);
            }
            if (DynamicObjects[i] != null && DynamicObjects[i].onCamera && DynamicObjects[i].framesCount != 1 && DynamicObjects[i].animSpeed != 0) {
                int animTime = (int) (DynamicObjects[i].animSpeed * 1000); // время на анимацию одного кадра
                int framesTime = (DynamicObjects[i].framesCount - 1) * animTime; // время на все кадры анимации (исключая последний)
                int loopTime = framesTime + animTime; // время на один полный цикл анимации
                int frameIndex = ((accumulator % loopTime) / animTime) + 1; // индекс текущего кадра

                DynamicObjects[i].currentFrame = frameIndex;
                drawTexture(DynamicObjects[i].path + frameIndex + ".png", (int) DynamicObjects[i].x, (int) DynamicObjects[i].y, 3);
            }
            else if (DynamicObjects[i] != null && DynamicObjects[i].onCamera && DynamicObjects[i].framesCount != 1 && DynamicObjects[i].animSpeed == 0) {
                drawTexture(DynamicObjects[i].path + DynamicObjects[i].currentFrame + ".png", (int) DynamicObjects[i].x, (int) DynamicObjects[i].y, 3);
            }
        }
    }

    public static void updateGUI() {
        for (Map.Entry<String, PanelObject> entry : panels.entrySet()) {
            String panel = entry.getKey();
            if (!panels.get(panel).visible) {
                continue;
            }

            if (!panels.get(panel).simple) {
                float centerX = panels.get(panel).x + panels.get(panel).width / 2.0f;
                float centerY = panels.get(panel).y + panels.get(panel).height / 2.0f;
                float newWidth = panels.get(panel).width / 1.1f;
                float newHeight = panels.get(panel).height / 1.1f;
                float newX = centerX - newWidth / 2.0f;
                float newY = centerY - newHeight / 2.0f;

                drawCutRectangle(panels.get(panel).x, panels.get(panel).y, panels.get(panel).width, panels.get(panel).height, panels.get(panel).height / 15f, new Color(40, 40, 40, 240));
                drawCutRectangle(newX, newY, newWidth, newHeight, panels.get(panel).height / 15f, new Color(85, 85, 85, 230));
            } else {
                drawRectangle(panels.get(panel).x, panels.get(panel).y, panels.get(panel).width, panels.get(panel).height, new Color(40, 40, 40, 240));
            }
        }

        for (Map.Entry<String, ButtonObject> entry : buttons.entrySet()) {
            String button = entry.getKey();
            if (!buttons.get(button).visible) {
                continue;
            }

            drawRectangle(buttons.get(button).x, buttons.get(button).y, buttons.get(button).width, buttons.get(button).height, buttons.get(button).color);
            drawText((int) (buttons.get(button).x * 1.01f), (int) (buttons.get(button).y + buttons.get(button).height / 2.8f), buttons.get(button).name);
        }
        for (Map.Entry<String, SliderObject> entry : sliders.entrySet()) {
            String slider = entry.getKey();
            if (!sliders.get(slider).visible) {
                continue;
            }

            drawRoundedRectangle(sliders.get(slider).x, sliders.get(slider).y, sliders.get(slider).width - sliders.get(slider).x, sliders.get(slider).height, new Color(200, 1, 1, 255));
            drawCircle(sliders.get(slider).sliderPos, sliders.get(slider).y + sliders.get(slider).height / 2, sliders.get(slider).height / 1.1f, new Color(1, 1, 200, 255));
        }
    }
}