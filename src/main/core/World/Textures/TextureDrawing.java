package core.World.Textures;

import core.Utils.Commandline;
import core.EventHandling.EventHandler;
import core.UI.GUI.Objects.ButtonObject;
import core.UI.GUI.Objects.PanelObject;
import core.UI.GUI.Objects.SliderObject;
import core.UI.GUI.Objects.TextObject;
import core.UI.GUI.Video;
import core.Utils.SimpleColor;
import core.Window;
import core.World.Creatures.DynamicWorldObjects;
import core.World.StaticWorldObjects.StaticWAnimations;
import core.World.StaticWorldObjects.Structures.ElectricCables;
import core.World.StaticWorldObjects.Structures.Factories;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.WorldUtils;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import static core.EventHandling.EventHandler.getMousePos;
import static core.EventHandling.Logging.Config.getFromConfig;
import static core.UI.GUI.CreateElement.*;
import static core.UI.GUI.Fonts.*;
import static core.UI.GUI.Video.*;
import static core.Window.*;
import static core.World.Creatures.Player.Player.*;
import static core.World.Creatures.Player.Player.currentInteraction;
import static core.World.StaticWorldObjects.StaticWorldObjects.*;
import static core.World.Textures.TextureLoader.ByteBufferEncoder;
import static core.World.Weather.Sun.updateSun;
import static core.World.WorldGenerator.*;
import static org.lwjgl.opengl.GL13.*;

public class TextureDrawing {
    public static final int blockSize = 48;
    private static float playerX, playerY;
    private static final HashMap<Integer, Integer> textures = new HashMap<>();

    public static void drawTexture(float x, float y, float zoom, boolean isStatic, boolean mirrorVertical, String path, SimpleColor color) {
        if (path == null || (color != null && color.getAlpha() == 0)) {
            return;
        } else if (color == null) {
            color = SimpleColor.WHITE;
        }

        int textureId = path.hashCode();
        if (textures.get(textureId) == null) {
            bindTexture(path);
        }

        int id = textures.get(textureId);
        int width = TextureLoader.getSizeStatic(path).width();
        int height = TextureLoader.getSizeStatic(path).height();

        glBindTexture(GL_TEXTURE_2D, id);

        glPushMatrix();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glLoadIdentity();

        glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);

        if (start && !isStatic) {
            glTranslatef(-playerX * zoom + Window.width / 2f - 32, -playerY * zoom + Window.height / 2f - 200, 0);
        }
//            glTranslatef(x * zoom + (width / 2f), y * zoom + (height / 2f), 0.0f);
//            glRotatef(0f, 0.0f, 0.0f, 1.0f);
//            glTranslatef(-(x * zoom + (width / 2f)), -(y * zoom + (height / 2f)), 0.0f);

        glMultMatrixf(new float[]{zoom, 0, 0, 0, 0, zoom, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1});
        glBegin(GL_QUADS);

        setCoords(x, y, height, width, mirrorVertical);

        glColor4f(1f, 1f, 1f, 1f);
        glEnd();

        glDisable(GL_TEXTURE_2D);
        glPopMatrix();

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public static void drawTexture(float x, float y, boolean isStatic, String path) {
        drawTexture(x, y, 1, isStatic, false, path, SimpleColor.WHITE);
    }

    public static void drawMultiTexture(float x, float y, float zoom, boolean isStatic, boolean mirrorVertical, String pathMain, String pathSecond, SimpleColor color) {
        if (color != null && color.getAlpha() == 0) {
            return;
        } else if (color == null) {
            color = SimpleColor.WHITE;
        }

        int textureId = (pathMain + pathSecond).hashCode();
        if (textures.get(textureId) == null) {
            bindTexture(TextureLoader.uniteTextures(pathMain, pathSecond), textureId, TextureLoader.getSize(pathMain).width(), TextureLoader.getSize(pathMain).height());
        }

        int id = textures.get(textureId);
        int width = TextureLoader.getSizeStatic(pathMain).width();
        int height = TextureLoader.getSizeStatic(pathMain).height();

        glBindTexture(GL_TEXTURE_2D, id);

        glPushMatrix();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glLoadIdentity();

        glColor4f(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f);

        if (start && !isStatic) {
            glTranslatef(-playerX * zoom + Window.width / 2f - 32, -playerY * zoom + Window.height / 2f - 200, 0);
        }

        glMultMatrixf(new float[]{zoom, 0, 0, 0, 0, zoom, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1});
        glBegin(GL_QUADS);

        setCoords(x, y, height, width, mirrorVertical);

        glColor4f(1f, 1f, 1f, 1f);
        glEnd();

        glDisable(GL_TEXTURE_2D);
        glPopMatrix();

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public static void drawTexture(float x, float y, int width, int height, float zoom, boolean isStatic, int id, ByteBuffer buffer, SimpleColor color) {
        if (textures.get(id) == null) {
            bindTexture(buffer, id, width, height);
        }
        glBindTexture(GL_TEXTURE_2D, textures.get(id));

        glMultMatrixf(new float[]{zoom, 0, 0, 0, 0, zoom, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1});
        glColor4f(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        glPushMatrix();
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glLoadIdentity();

        if (start && !isStatic) {
            glTranslatef(-playerX * zoom + Window.width / 2f - 32, -playerY * zoom + Window.height / 2f - 200, 0);
        }

        glBegin(GL_QUADS);
        setCoords(x, y, height, width, false);
        glEnd();

        glDisable(GL_TEXTURE_2D);
        glPopMatrix();

        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private static void setCoords(float x, float y, int height, int width, boolean mirrorVertical) {
        if (mirrorVertical) {
            //top right
            glTexCoord2f(1, 1);
            glVertex2f(x, y);
            //top left
            glTexCoord2f(0, 1);
            glVertex2f(x + width, y);
            //bottom left
            glTexCoord2f(0, 0);
            glVertex2f(x + width, y + height);
            //bottom right
            glTexCoord2f(1, 0);
            glVertex2f(x, y + height);
        } else {
            //top left
            glTexCoord2f(0, 1);
            glVertex2f(x, y);
            //top right
            glTexCoord2f(1, 1);
            glVertex2f(x + width, y);
            //bottom right
            glTexCoord2f(1, 0);
            glVertex2f(x + width, y + height);
            //bottom left
            glTexCoord2f(0, 0);
            glVertex2f(x, y + height);
        }
    }

    public static void drawText(int x, int y, String text, SimpleColor color) {
        int startX = x;

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == ' ') {
                x += getCharDimension('A').width;
                continue;
            } else if (ch == '\\' && text.charAt(i + 1) == 'n') {
                y -= 30;
                i++;
                x = startX;
                continue;
            }
            TextureDrawing.drawTexture(x, y, getCharDimension(ch).width, getCharDimension(ch).height, 1, true, ch, getCharBuffer(ch), color);
            x += getCharDimension(ch).width;
        }
    }

    public static void drawText(int x, int y, String text) {
        drawText(x, y, text, new SimpleColor(210, 210, 210, 255));
    }

    public static void drawRectangleBorder(int x, int y, int width, int height, int thickness, SimpleColor color) {
        glPushMatrix();
        glLineWidth(thickness);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        drawRectangle(x, y, width, thickness, color); //Upper border
        drawRectangle(x + width - thickness, y + thickness, thickness, height - thickness * 2, color); //Right border
        drawRectangle(x, y + height - thickness, width, thickness, color); //Down border
        drawRectangle(x, y + thickness, thickness, height - thickness * 2, color); //Left border

        glEnd();
        glPopMatrix();
    }

    public static void drawRectangle(int x, int y, int width, int height, SimpleColor color) {
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

    public static void drawRoundedRectangle(int x, int y, int width, int height, SimpleColor color) {
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

    public static void drawCircle(int x, int y, float radius, SimpleColor color) {
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

    public static void drawPrompt(ButtonObject button) {
        if (getFromConfig("ShowPrompts").equals("true") && new Rectangle(button.x, button.y, button.width, button.height).contains(getMousePos()) && System.currentTimeMillis() - EventHandler.lastMouseMovedTime >= 1000 && button.prompt != null) {
            drawRectangleText(EventHandler.getMousePos().x, EventHandler.getMousePos().y, 0, button.prompt, false, new SimpleColor(40, 40, 40, 240));
        }
    }

    public static void drawPointArray(Point[] points) {
        glColor4f(0, 0, 0, 1);

        //todo вынести работу с матрицей
        glPushMatrix();
        glLoadIdentity();
        glTranslatef(-playerX + Window.width / 2f - 32, -playerY + Window.height / 2f - 200, 0);
        glMultMatrixf(new float[]{1, 0, 0, 0, 0, 3, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1});

        glLineWidth(4);
        glBegin(GL_LINES);

        for (int i = 0; i < points.length; i++) {
            glVertex2f(points[i].x * blockSize + 8, points[i].y * blockSize + 8);

            if (i + 1 < points.length) {
                glVertex2f(points[i + 1].x * blockSize + 8, points[i + 1].y * blockSize + 8);
            }
        }

        glEnd();
        glPopMatrix();
        glColor4f(1, 1, 1, 1);
    }

    public static void drawRectangleText(int x, int y, int maxWidth, String text, boolean staticTransfer, SimpleColor panColor) {
        maxWidth = (maxWidth > 0 ? maxWidth : 1920 - x);
        y = staticTransfer ? y + getTextSize(text).width / maxWidth * TextureDrawing.blockSize : y;

        StringBuilder modifiedText = new StringBuilder();
        int currentWidth = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == ' ') {
                currentWidth += getCharDimension('A').width;
            } else {
                currentWidth += getCharDimension(c).width;
            }
            if (currentWidth > maxWidth) {
                modifiedText.append("\\n");
                currentWidth = 0;
            }
            modifiedText.append(c);
        }
        text = modifiedText.toString();

        Dimension textSize = getTextSize(text);
        int width = textSize.width;
        int height = textSize.height;

        drawRectangle(x + 30, y - height / 2, width, height, panColor);
        drawText(x + 36, y + height - 32 - height / 2, text);
    }

    public static Dimension getTextSize(String text) {
        String longestLine = "";
        int width = 12;

        for (String line : text.split("\\\\n")) {
            if (line.length() >= longestLine.replaceAll("\\s+", "").length()) {
                longestLine = line;
            }
        }

        for (int i = 0; i < longestLine.length(); i++) {
            char c = longestLine.charAt(i);

            if (c == ' ') {
                width += getCharDimension('A').width;
                continue;
            }
            width += getCharDimension(c).width;
        }
        return new Dimension(width, (text.split("\\\\n").length) * 28 + 16);
    }

    public static void updateVideo() {
        if (!video.isEmpty()) {

            ByteBuffer buff = null;
            for (Map.Entry<String, Video> entry : video.entrySet()) {
                String name = entry.getKey();
                Video video = entry.getValue();

                if (video != null && video.isPlaying) {
                    if (video.frame == video.totalFrames) {
                        video.frame = 1;
                    }
                    if (byteBuffer.get(name) != null && !byteBuffer.get(name).equals(buff)) {
                        drawTexture(video.x, video.y, video.width, video.height, 1, true, name.hashCode(), byteBuffer.get(name), SimpleColor.WHITE);
                        buff = byteBuffer.get(name);
                    }
                }
            }
        }
    }

    public static void updatePlayerPos() {
        playerX = DynamicObjects.getFirst().getX();
        playerY = DynamicObjects.getFirst().getY();
    }

    public static void updateStaticObj() {
        updateSun();
        ElectricCables.drawCables();
        updatePlayerPos();

        for (int x = (int) (playerX / blockSize) - 20; x < playerX / blockSize + 21; x++) {
            for (int y = (int) (playerY / blockSize) - 8; y < playerY / blockSize + blockSize; y++) {
                if (x < 0 || y < 0 || x > SizeX || y > SizeY) {
                    continue;
                }
                short obj = getObject(x, y);

                if (obj == -1 || StaticWorldObjects.getId(obj) == 0 || getPath(obj) == null) {
                    continue;
                }
                if (getHp(obj) <= 0) {
                    destroyObject(x, y);
                    continue;
                }

                int xBlock = findX(x, y);
                int yBlock = findY(x, y);

                //todo настроить камеру
                if (isOnCamera(xBlock, yBlock)) {
                    SimpleColor color = ShadowMap.getColor(x, y);
                    int upperLimit = 100;
                    int lowestLimit = -20;
                    int maxColor = 65;
                    float temp = TemperatureMap.getTemp(x, y);

                    int a;
                    if (temp > upperLimit) {
                        a = (int) Math.min(maxColor, Math.abs((temp - upperLimit) / 3));
                        color = new SimpleColor(color.getRed(), color.getGreen() - (a / 2), color.getBlue() - a, color.getAlpha());

                    } else if (temp < lowestLimit) {
                        a = (int) Math.min(maxColor, Math.abs((temp + lowestLimit) / 3));
                        color = new SimpleColor(color.getRed() - a, color.getGreen() - (a / 2), color.getBlue(), color.getAlpha());
                    }

                    StaticWAnimations.AnimData currentFrame = StaticWAnimations.getCurrentFrame(obj, new Point(x, y));
                    if (currentFrame != null) {
                        drawTexture(xBlock, yBlock, currentFrame.width(), currentFrame.height(), 1, false, currentFrame.currentFrame() + StaticWorldObjects.getId(obj), currentFrame.currentFrameImage(), color);
                        continue;
                    }

                    if (getHp(obj) > getMaxHp(obj) / 1.5f) {
                        drawTexture(xBlock, yBlock, 1f, false, false, getPath(obj), color);

                    } else if (getHp(obj) < getMaxHp(obj) / 3) {
                        drawMultiTexture(xBlock, yBlock, 1f, false, false, getPath(obj), assetsDir(("World/Blocks/damaged" + (getHp(obj) < getMaxHp(obj) / 3 ? "1" : "0")) + ".png"), color);

                    } else {
                        drawMultiTexture(xBlock, yBlock, 1f, false, false, getPath(obj), assetsDir("World/Blocks/damaged0.png"), color);
                    }
                }
            }
        }
        updateBlocksInteraction();
        Factories.update();
    }

    private static void updateBlocksInteraction() {
        char interactionChar = 'E';
        Point mousePos = WorldUtils.getBlockUnderMousePoint();
        Point root = findRoot(mousePos.x, mousePos.y);
        boolean interactionButtonPressed = EventHandler.getKeyClick(interactionChar);

        if (root != null) {
            Runnable interaction = StaticWorldObjects.getOnInteraction(getObject(root.x, root.y));

            if (currentInteraction != null && currentInteraction.isAlive() && interactionButtonPressed) {
                currentInteraction.interrupt();
                return;
            }
            if (interaction != null) {
                TextureDrawing.drawTexture((root.x * TextureDrawing.blockSize) + TextureDrawing.blockSize, (root.y * TextureDrawing.blockSize) + TextureDrawing.blockSize, false, assetsDir("\\UI\\GUI\\interactionIcon.png"));
                TextureDrawing.drawTexture((root.x * TextureDrawing.blockSize + 16) + TextureDrawing.blockSize, (root.y * TextureDrawing.blockSize + 12) + TextureDrawing.blockSize, getCharDimension(interactionChar).width, getCharDimension(interactionChar).height, 1, false, interactionChar, getCharBuffer(interactionChar), SimpleColor.WHITE);

                if (interactionButtonPressed) {
                    currentInteraction = new Thread(interaction);
                    currentInteraction.start();
                }
            }
        }
    }

    public static boolean isOnCamera(int x, int y) {
        DynamicWorldObjects player = DynamicObjects.getFirst();

        float left = player.getX() - (1920 / 2.1f) - (32 + blockSize);
        float right = player.getX() + (1920 / 1.7f) + (32 - blockSize);
        float bottom = player.getY() - (1080 / 3.7f) - (32 + blockSize); //lower dividet number - higher drawing
        float top = player.getY() + (1080 / 1.4f) + (32 - blockSize);

        return !(x + 16 < left) && !(x > right) && !(y + 16 < bottom) && !(y > top);
    }

    public static void updateDynamicObj() {
        for (DynamicWorldObjects dynamicObject : DynamicObjects) {
            if (dynamicObject != null) {
                dynamicObject.incrementCurrentFrame();

                //todo проверить правильность настройки камеры
                if (isOnCamera((int) dynamicObject.getX(), (int) dynamicObject.getY())) {
                    if (dynamicObject.getFramesCount() == 0) {
                        drawTexture(dynamicObject.getX(), dynamicObject.getY(), 1, false, false, dynamicObject.getPath(), ShadowMap.getColorDynamic(dynamicObject));
                    } else {
                        //todo дописать
                        //drawTexture(dynamicObject.getPath() + dynamicObject.getCurrentFrame() + ".png", dynamicObject.getX(), dynamicObject.getY(), ShadowMap.getColorDynamic(), false, false);
                    }
                }
            }
        }
    }

    public static void updateGUI() {
        updatePlayerGUI();
        updatePanels();
        updateSwapButtons();
        updateButtons();
        updateSliders();
        updateTexts();
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
                        if (panelObj.options != null && panelObj.layer == layer && panelObj.visible) {
                            drawTexture(panelObj.x, panelObj.y, true, panelObj.options);
                        }
                    }
                }
                continue;
            }

            if (!panel.simple) {
                drawRectangle(panel.x, panel.y, panel.width, panel.height, panel.color);
                drawRectangleBorder(panel.x, panel.y, panel.width, panel.height, 20, panel.color);
            } else {
                drawRectangle(panel.x, panel.y, panel.width, panel.height, panel.color);
            }
        }
    }

    private static void updateSwapButtons() {
        for (Map.Entry<String, ButtonObject> entry : buttons.entrySet()) {
            ButtonObject button = entry.getValue();
            if (!button.visible || !button.swapButton) {
                continue;
            }

            if (button.simple && button.isClicked) {
                drawRectangle(button.x, button.y, button.width, button.height, button.color);
                drawTexture(button.x + button.width / 1.3f, button.y + button.height / 3f, true, assetsDir("UI/GUI/checkMarkTrue.png"));
                drawText((int) (button.x * 1.1f), button.y + button.height / 3, button.name);
            } else if (button.simple) {
                drawRectangle(button.x, button.y, button.width, button.height, button.color);
                drawText((int) (button.x * 1.1f), button.y + button.height / 3, button.name);
            } else {
                //if swap and not simple
                if (button.isClicked) {
                    drawRectangleBorder(button.x - 6, button.y - 6, button.width, button.height, 6, button.color);
                    drawTexture(button.x, button.y, true, assetsDir("UI/GUI/checkMarkTrue.png"));
                    drawText(button.width + button.x + 24, button.y, button.name);
                } else {
                    drawRectangleBorder(button.x - 6, button.y - 6, button.width, button.height, 6, button.color);
                    drawTexture(button.x, button.y, true, assetsDir("UI/GUI/checkMarkFalse.png"));
                    drawText(button.width + button.x + 24, button.y, button.name);
                }
            }
            drawPrompt(button);
        }
    }

    private static void updateButtons() {
        for (Map.Entry<String, ButtonObject> entry : buttons.entrySet()) {
            ButtonObject button = entry.getValue();
            if (!button.visible || button.swapButton) {
                continue;
            }

            if (button.path != null) {
                drawTexture(button.x, button.y, true, button.path);
                continue;
            }
            if (button.simple) {
                drawRectangle(button.x, button.y, button.width, button.height, button.color);
            } else {
                drawRectangleBorder(button.x, button.y, button.width, button.height, 6, button.color);
            }
            if (!button.isClickable) {
                drawRectangle(button.x, button.y, button.width, button.height, new SimpleColor(0, 0, 0, 123));
            }
            drawText(button.x + 20, (int) (button.y + button.height / 2.8f), button.name);
            drawPrompt(button);
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

    private static void updateTexts() {
        for (TextObject text : texts.values()) {
            if (!text.visible) {
                continue;
            }
            drawText(text.x, text.y, text.text, text.color);
        }

        if (Commandline.created) {
            drawRectangle(20, 800, 650, 260, new SimpleColor(0, 0, 0, 220));
            drawRectangleText(-10, 810, 630, EventHandler.keyLoggingText, true, SimpleColor.BLACK);
        }
    }


    public static int bindTexture(String path) {
        ByteBuffer buffer = ByteBufferEncoder(path);

        int width = TextureLoader.getSize(path).width();
        int height = TextureLoader.getSize(path).height();
        int id = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        textures.put(path.hashCode(), id);

        glBindTexture(GL_TEXTURE_2D, 0);

        return id;
    }

    public static int bindTexture(ByteBuffer buffer, int id, int width, int height) {
        int texId = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, texId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        textures.put(id, texId);

        glBindTexture(GL_TEXTURE_2D, 0);
        return texId;
    }

    public static void bindChars() {
        letterSize.forEach((character, dimension) -> {
            int width = dimension.width;
            int height = dimension.height;

            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, getCharBuffer(character));
            TextureDrawing.textures.put(character.hashCode(), id);

            glBindTexture(GL_TEXTURE_2D, 0);
        });
    }
}
