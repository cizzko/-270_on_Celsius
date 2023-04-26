package core.World.Textures;

import core.EventHandling.MouseScrollCallback;
import core.Window;
import core.World.WorldGenerator;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import static core.GUI.CreateElement.elements;
import static org.lwjgl.opengl.GL13.*;

public class TextureDrawing {
    private static int accumulator = 0;
    public static StaticWorldObjects[][] StaticObjects = WorldGenerator.StaticObjects;
    public static DynamicWorldObjects[] DynamicObjects = WorldGenerator.DynamicObjects;

    public static void drawTexture(String path, int x, int y, float zoom) {
        glPushMatrix();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glLoadIdentity();
        glTranslatef(-DynamicObjects[0].x * zoom + Window.get().width / 2 - 32, -DynamicObjects[0].y, 0);
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

    public static void updateStaticObj() {
        float left = DynamicObjects[0].x - 1920 / 3;
        float right = DynamicObjects[0].x + 1920 / 3;
        float top = DynamicObjects[0].y - 1080 / 3;
        float bottom = DynamicObjects[0].x + 1080 / 3;

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
        Enumeration<String> keys = elements.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            if (elements.get(key).visible && elements.get(key).isButton) {
                drawTexture(elements.get(key).path, elements.get(key).x, elements.get(key).y, 1);
            }
        }
    }
}