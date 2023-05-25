package core.World.Textures;

import core.EventHandling.MouseScrollCallback;
import core.UI.GUI.Fonts;
import core.UI.GUI.objects.ButtonObject;
import core.UI.GUI.objects.PanelObject;
import core.UI.GUI.objects.SliderObject;
import core.UI.GUI.Video;
import core.EventHandling.Logging.config;
import core.Window;
import core.World.WorldGenerator;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import static core.UI.GUI.CreateElement.*;
import static core.UI.GUI.Video.*;
import static core.Window.defPath;
import static core.World.Textures.TextureLoader.BufferedImageEncoder;
import static core.World.Textures.TextureLoader.ByteBufferEncoder;
import static org.lwjgl.opengl.GL13.*;

public class TextureDrawing {
    private static int accumulator = 0;
    private static final int spacingBetweenLetters = Integer.parseInt(config.jetFromConfig("SpacingBetweenLetters"));
    private static Map<Integer, TextureData> textures = new HashMap<>();
    public static StaticWorldObjects[][] StaticObjects;
    public static DynamicWorldObjects[] DynamicObjects;

    public static void loadObjects() {
        StaticObjects = WorldGenerator.StaticObjects;
        DynamicObjects = WorldGenerator.DynamicObjects;
    }

    //for textures (world)
    private static void drawTexture(String path, int x, int y, float zoom) {
        int textureId = path.hashCode();
        TextureData textureData = textures.get(textureId);

        if (textureData == null) {
            ByteBuffer buffer = ByteBufferEncoder(path);
            BufferedImage image = BufferedImageEncoder(path);

            int width = image.getWidth();
            int height = image.getHeight();

            if (width < height) {
                width = image.getHeight();
                height = image.getWidth();
            }

            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);

            glColor4f(1f, 1f, 1f, 1f);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

            textureData = new TextureData(id, width, height);
            textures.put(textureId, textureData);
        }

        int width = textureData.width;
        int height = textureData.height;

        glBindTexture(GL_TEXTURE_2D, textureData.id);

        glPushMatrix();
        glEnable(GL_TEXTURE_2D);

        glEnable(GL_BLEND);
        glLoadIdentity();

        if (Window.start) {
            glTranslatef(-DynamicObjects[0].x * zoom + Window.width / 2f - 32, -DynamicObjects[0].y, 0);
            glMultMatrixf(new float[]{zoom + (float) (zoom + MouseScrollCallback.getScroll()) / 10, 0, 0, 0, 0, zoom + (float) (zoom + MouseScrollCallback.getScroll()) / 10, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1});
        } else {
            glMultMatrixf(new float[]{zoom, 0, 0, 0, 0, zoom, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1});
        }

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

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    //for video, text, etc
    public static void drawTexture(int x, int y, int width, int height, ByteBuffer buffer, Color color, float zoom) {
        glPushMatrix();
        glEnable(GL_TEXTURE_2D);

        glEnable(GL_BLEND);
        glLoadIdentity();

        glMultMatrixf(new float[]{zoom, 0, 0, 0, 0, zoom, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1});

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

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
            TextureDrawing.drawTexture(x, y, Fonts.letterSize.get(ch).width, Fonts.letterSize.get(ch).height, Fonts.chars.get(ch), new Color(210, 210, 210, 255), 1);
            x += Fonts.letterSize.get(ch).width * (spacingBetweenLetters / 10);
        }
    }

    public static void drawRectangleBorder(int x, int y, int width, int height, int thickness, Color color) {
        glPushMatrix();
        glLineWidth(thickness);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        drawRectangle(x, y, width, thickness, color); // Верхняя граница
        drawRectangle(x + width - thickness, y + thickness, thickness, height - thickness * 2, color); // Правая граница
        drawRectangle(x, y + height - thickness, width, thickness, color); // Нижняя граница
        drawRectangle(x, y + thickness, thickness, height - thickness * 2, color); // Левая граница

        glEnd();
        glPopMatrix();
    }

    public static void drawRectangle(int x, int y, int width, int height, Color color) {
        glPushMatrix();
        glBegin(GL_QUADS);

        glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        glVertex2f(x, y);
        glVertex2f(x + width, y);
        glVertex2f(x + width, y + height);
        glVertex2f(x, y + height);
        glColor3f(1, 1, 1);

        glEnd();
        glPopMatrix();
    }

    public static void drawRoundedRectangle(int x, int y, int width, int height, Color color) {
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

    public static void drawCircle(int x, int y, float radius, Color color) {
        int samples = 64;
        glPushMatrix();
        glBegin(GL_TRIANGLE_FAN);

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
                    drawTexture(video.x, video.y, video.width, video.height, byteBuffer.get(name), new Color(255, 255, 255, 255), 1);
                    buff = byteBuffer.get(name);
                }
            }
        }
    }


    public static void updateStaticObj() {
        float left = DynamicObjects[0].x - 1920 / 5.5f;
        float right = DynamicObjects[0].x + 1920 / 5.5f;
        float top = DynamicObjects[0].y - 1080 / 5.5f;
        float bottom = DynamicObjects[0].x + 1080 / 5.5f;

        for (int x = 0; x < StaticObjects.length - 1; x++) {
            for (int y = 0; y < StaticObjects[x].length - 1; y++) {
                StaticObjects[x][y].onCamera = !(StaticObjects[x][y].x < left) && !(StaticObjects[x][y].x > right) && !(StaticObjects[x][y].y < top) && !(StaticObjects[x][y].y > bottom);
                if (StaticObjects[x][y].onCamera && !StaticObjects[x][y].notForDrawing) {
                    drawTexture(StaticObjects[x][y].path, (int) StaticObjects[x][y].x, (int) StaticObjects[x][y].y, 3);
                }
            }
        }
    }

    public static void updateDynamicObj() {
        accumulator += Window.deltaTime;

        for (DynamicWorldObjects dynamicObject : DynamicObjects) {
            if (dynamicObject != null && dynamicObject.onCamera && dynamicObject.framesCount == 1) {
                drawTexture(dynamicObject.path, (int) dynamicObject.x, (int) dynamicObject.y, 3);
            }
            if (dynamicObject != null && dynamicObject.onCamera && dynamicObject.framesCount != 1 && dynamicObject.animSpeed != 0) {
                int animTime = (int) (dynamicObject.animSpeed * 1000); // время на анимацию одного кадра
                int framesTime = (dynamicObject.framesCount - 1) * animTime; // время на все кадры анимации (исключая последний)
                int loopTime = framesTime + animTime; // время на один полный цикл анимации
                int frameIndex = ((accumulator % loopTime) / animTime) + 1; // индекс текущего кадра

                dynamicObject.currentFrame = frameIndex;
                drawTexture(dynamicObject.path + frameIndex + ".png", (int) dynamicObject.x, (int) dynamicObject.y, 3);
            } else if (dynamicObject != null && dynamicObject.onCamera && dynamicObject.framesCount != 1) {
                drawTexture(dynamicObject.path + dynamicObject.currentFrame + ".png", (int) dynamicObject.x, (int) dynamicObject.y, 3);
            }
        }
    }

    public static void updateGUI() {
        updatePanels();
        updateSwapButtons();
        updateButtons();
        updateDropMenu();
        updateSliders();
    }

    private static void updatePanels() {
        for (PanelObject panel : panels.values()) {
            if (!panel.visible) {
                continue;
            }

            if (panel.options != null) {
                List<Integer> layers = panels.values().stream().map(p -> p.layer).distinct().sorted().toList();

                for (int layer : layers) {
                    for (PanelObject panelObj : panels.values()) {
                        if (panelObj.layer == layer) {
                            if (panelObj.options != null) {
                                drawTexture(panelObj.options, panelObj.x, panelObj.y, 1);
                            }
                        }
                    }
                }
                continue;
            }

            if (!panel.simple) {
                drawRectangle(panel.x, panel.y, panel.width, panel.height, new Color(40, 40, 40, 240));
                drawRectangleBorder(panel.x, panel.y, panel.width, panel.height, 20, new Color(20, 20, 20, 40));
            } else {
                drawRectangle(panel.x, panel.y, panel.width, panel.height, new Color(40, 40, 40, 240));
            }
        }
    }

    private static void updateDropMenu() {
        for (Map.Entry<String, ButtonObject> entry : buttons.entrySet()) {
            ButtonObject button = entry.getValue();
            if (!button.visible || dropMenu.get(button.name) == null) {
                continue;
            }
            drawRectangle(button.x, button.y, button.width, button.height, button.color);
            drawText((int) (button.x * 1.1f), button.y + button.height / 3, button.name);

            if (button.isClicked) {
                ButtonObject[] dropButtons = dropMenu.get(button.name);

                for (int i = 0; i < dropButtons.length; i++) {
                    ButtonObject dropButton = dropButtons[i];

                    if (dropButton.simple && dropButton.swapButton && dropButton.isClicked) {
                        drawRectangle(dropButton.x, dropButton.y, dropButton.width, dropButton.height, dropButton.color);
                        drawTexture(defPath + "\\src\\assets\\GUI\\checkMarkTrue.png", (int) (dropButton.x + dropButton.width / 1.3f), dropButton.y + dropButton.height / 3, 1);
                        drawText((int) (dropButton.x * 1.1f), dropButton.y + dropButton.height / 3, dropButton.name);
                    } else if (dropButton.simple && dropButton.swapButton) {
                        drawRectangle(dropButton.x, dropButton.y, dropButton.width, dropButton.height, dropButton.color);
                        drawText((int) (dropButton.x * 1.1f), dropButton.y + dropButton.height / 3, dropButton.name);
                    }
                }
            }
        }
    }

    private static void updateSwapButtons() {
        for (Map.Entry<String, ButtonObject> entry : buttons.entrySet()) {
            ButtonObject button = entry.getValue();
            if (!button.visible) {
                continue;
            }

            if (button.simple && button.swapButton && button.isClicked) {
                drawRectangle(button.x, button.y, button.width, button.height, button.color);
                drawTexture(defPath + "\\src\\assets\\GUI\\checkMarkTrue.png", (int) (button.x + button.width / 1.3f), button.y + button.height / 3, 1);
                drawText((int) (button.x * 1.1f), button.y + button.height / 3, button.name);
            } else if (button.simple && button.swapButton) {
                drawRectangle(button.x, button.y, button.width, button.height, button.color);
                drawText((int) (button.x * 1.1f), button.y + button.height / 3, button.name);
            } else {

                //if swap and not simple
                if (button.swapButton && button.isClicked) {
                    drawRectangleBorder(button.x - 6, button.y - 6, button.width, button.height, 6, button.color);
                    drawTexture(defPath + "\\src\\assets\\GUI\\checkMarkTrue.png", button.x, button.y, 1);
                    drawText(button.width + button.x + 24, button.y, button.name);
                } else if (button.swapButton) {
                    drawRectangleBorder(button.x - 6, button.y - 6, button.width, button.height, 6, button.color);
                    drawTexture(defPath + "\\src\\assets\\GUI\\checkMarkFalse.png", button.x, button.y, 1);
                    drawText(button.width + button.x + 24, button.y, button.name);
                }
            }
        }
    }

    private static void updateButtons() {
        for (Map.Entry<String, ButtonObject> entry : buttons.entrySet()) {
            ButtonObject button = entry.getValue();
            if (!button.visible) {
                continue;
            }

            if (button.simple && !button.swapButton) {
                drawRectangle(button.x, button.y, button.width, button.height, button.color);
                drawText(button.x + 20, (int) (button.y + button.height / 2.8f), button.name);
            } else if (!button.swapButton) {
                drawRectangleBorder(button.x, button.y, button.width, button.height, 6, button.color);
                drawText(button.x + 20, (int) (button.y + button.height / 2.8f), button.name);
            }
            if (!button.isClickable && !button.swapButton) {
                drawRectangle(button.x, button.y, button.width, button.height, new Color(0, 0, 0, 123));
            } else if (!button.isClickable) {
                drawRectangle(button.x - 6, button.y - 6, button.width, button.height, new Color(0, 0, 0, 123));
            }
        }
    }


    private static void updateSliders() {
        for (SliderObject slider : sliders.values()) {
            if (!slider.visible) {
                continue;
            }
            drawRectangle(slider.x, slider.y, slider.width, slider.height, slider.sliderColor);
            drawCircle(slider.sliderPos, slider.y + slider.height / 2, slider.height / 1.1f, slider.dotColor);
        }
    }
}