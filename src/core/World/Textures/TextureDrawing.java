package core.World.Textures;

import core.EventHandling.MouseScrollCallback;
import core.GUI.ButtonObject;
import core.GUI.SliderObject;
import core.GUI.Video;
import core.Window;
import core.World.WorldGenerator;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Map;
import static core.GUI.CreateElement.buttons;
import static core.GUI.CreateElement.sliders;
import static core.GUI.Video.*;
import static org.lwjgl.opengl.GL13.*;

public class TextureDrawing {
    private static int accumulator = 0;
    public static boolean staticObjUpdated, dynamicObjUpdated, guiUpdated, videoUpdated;
    public static StaticWorldObjects[][] StaticObjects = WorldGenerator.StaticObjects;
    public static DynamicWorldObjects[] DynamicObjects = WorldGenerator.DynamicObjects;

    public static void drawTexture(String path, int x, int y, float zoom) {
        glPushMatrix();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glLoadIdentity();
        glTranslatef(-DynamicObjects[0].x * zoom + Window.get().width / 2f - 32, -DynamicObjects[0].y, 0);
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

    public static void drawTexture(int x, int y, ByteBuffer buffer, BufferedImage image) {
        glPushMatrix();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glLoadIdentity();

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

    public static void drawRectangle(int x, int y, int width, int height, float r, float g, float b) {
        if (r < 0 || r > 255) r = 0;
        if (g < 0 || g > 255) g = 0;
        if (b < 0 || b > 255) b = 0;

        glPushMatrix();
        glBegin(GL_QUADS);
        glEnable(GL_TEXTURE_2D);

        glColor3f(r / 255, g / 255, b / 255);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glColor3f(1, 1, 1);

        glEnd();
        glPopMatrix();
    }

    public static void drawCircle(int x, int y, float radius, float r, float g, float b) {
        if (r < 0 || r > 255) r = 0;
        if (g < 0 || g > 255) g = 0;
        if (b < 0 || b > 255) b = 0;

        int samples = 64;
        glPushMatrix();
        glBegin(GL_TRIANGLE_FAN);
        glEnable(GL_TEXTURE_2D);

        glColor3f(r / 255, g / 255, b / 255);
        glVertex2f(x, y);

        for (int i = 0; i <= samples; i++) {
            float angle = (float) (i * 2 * Math.PI / samples);
            float dx = (float) (radius * Math.cos(angle));
            float dy = (float) (radius * Math.sin(angle));
            glVertex2f(x + dx, y + dy);
        }
        glColor3f(1, 1, 1);

        glEnd();
        glPopMatrix();
    }

    public static void updateVideo() {
        for (Map.Entry<String, Video> entry : video.entrySet()) {
            String name = entry.getKey();
            int timeSinceLastFrame = (int) (System.currentTimeMillis() - video.get(name).lastFrameTime);
            int targetTimePerFrame = 1000 / video.get(name).fps;
            ByteBuffer[] buffer = byteBuffers.get(name);
            BufferedImage[] image = bufferedImages.get(name);

            if (timeSinceLastFrame >= targetTimePerFrame) {
                video.get(name).lastFrameTime = (int) System.currentTimeMillis();
                video.get(name).frame++;
                if (video.get(name).frame > byteBuffers.get(name).length) {
                    video.get(name).isPlayed = false;
                }
            }
            if (video.get(name).isPlayed) {
                drawTexture(video.get(name).x, video.get(name).y, buffer[video.get(name).frame], image[video.get(name).frame]);
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
        for (Map.Entry<String, ButtonObject> entry : buttons.entrySet()) {
            String button = entry.getKey();

            drawRectangle(buttons.get(button).x, buttons.get(button).y, buttons.get(button).width, buttons.get(button).height, 200, 200, 1);
        }
        for (Map.Entry<String, SliderObject> entry : sliders.entrySet()) {
            String slider = entry.getKey();

            drawRectangle(sliders.get(slider).x, sliders.get(slider).y, sliders.get(slider).width - sliders.get(slider).x, sliders.get(slider).height, 200, 1, 1);
            drawCircle(sliders.get(slider).sliderPos, sliders.get(slider).y + sliders.get(slider).height / 2, sliders.get(slider).height / 1.1f, 1, 1, 200);
        }
    }
}