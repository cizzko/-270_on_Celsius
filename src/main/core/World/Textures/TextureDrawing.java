package core.World.Textures;

import core.EventHandling.EventHandler;
import core.UI.GUI.Objects.ButtonObject;
import core.UI.GUI.Objects.PanelObject;
import core.UI.GUI.Objects.SliderObject;
import core.UI.GUI.Objects.TextObject;
import core.UI.GUI.Video;
import core.Utils.Commandline;
import core.Utils.SimpleColor;
import core.Window;
import core.World.Creatures.DynamicWorldObjects;
import core.World.StaticWorldObjects.StaticWAnimations;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.StaticWorldObjects.Structures.ElectricCables;
import core.World.StaticWorldObjects.Structures.Factories;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.WorldUtils;
import core.g2d.Fill;
import core.g2d.Font;
import core.graphic.Layer;
import core.math.Rectangle;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static core.EventHandling.Logging.Config.getFromConfig;
import static core.Global.*;
import static core.UI.GUI.CreateElement.*;
import static core.UI.GUI.Video.byteBuffer;
import static core.UI.GUI.Video.video;
import static core.Window.start;
import static core.World.Creatures.Player.Player.*;
import static core.World.StaticWorldObjects.StaticWorldObjects.*;
import static core.World.Weather.Sun.updateSun;
import static core.World.WorldGenerator.*;
import static org.lwjgl.opengl.GL13.*;

public class TextureDrawing {
    public static final int blockSize = 48;
    public static float playerX = 0, playerY = 0;

    // TODO Сломано сознательно, чуть позже доделаю
    @Deprecated(forRemoval = true)
    public static void drawTexture(float x, float y, int w, int h, float zoom, boolean isStatic, int id, ByteBuffer buffer, SimpleColor color) {
    }

    public static void drawText(float x, float y, String text, SimpleColor color) {
        float startX = x;

        batch.color(color);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == ' ') {
                x += Window.defaultFont.getGlyph('A').width();
                continue;
            } else if (ch == '\\' && text.charAt(i + 1) == 'n') {
                y -= 30;
                i++;
                x = startX;
                continue;
            }
            Font.Glyph glyph = Window.defaultFont.getGlyph(ch);
            batch.draw(glyph, x, y);
            x += glyph.width();
        }
        batch.resetColor();
    }

    public static void drawText(float x, float y, String text) {
        drawText(x, y, text, SimpleColor.fromRGBA(210, 210, 210, 255));
    }

    public static void drawPrompt(ButtonObject button) {
        if (getFromConfig("ShowPrompts").equals("true")) {
            if (Rectangle.contains(button.x, button.y, button.width, button.height, input.mousePos()) && System.currentTimeMillis() - input.getLastMouseMoveTimestamp() >= 1000 && button.prompt != null) {
                drawRectangleText(input.mousePos().x, input.mousePos().y, 0, button.prompt, false, SimpleColor.fromRGBA(40, 40, 40, 240));
            }
        }
    }

    public static void drawPointArray(Point[] points) {
        Fill.lineWidth(4f);

        batch.color(SimpleColor.fromRGBA(0, 0, 0, 1));
        float d = blockSize + 8;
        for (int i = 0; i < points.length; i++) {
            if (i + 1 < points.length) {
                Point point = points[i];
                Point next = points[i + 1];
                Fill.line(point.x * d, point.y * d, next.x * d, next.y * d);
            }
        }
        Fill.resetLineWidth();
    }

    public static void drawRectangleText(int x, int y, int maxWidth, String text, boolean staticTransfer, SimpleColor panColor) {
        maxWidth = (maxWidth > 0 ? maxWidth : 1920 - x);
        y = staticTransfer ? y + getTextSize(text).width / maxWidth * blockSize : y;

        StringBuilder modifiedText = new StringBuilder();
        int currentWidth = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == ' ') {
                currentWidth += Window.defaultFont.getGlyph('A').width();
            } else {
                currentWidth += Window.defaultFont.getGlyph(c).width();
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

        Fill.rect(x + 30, y - height / 2f, width, height, panColor);
        drawText(x + 36, y + height - 32 - height / 2f, text);
    }

    public static Dimension getTextSize(String text) {
        String longestLine = "";
        int width = 12;

        int linesCount = 0;
        for (String line : text.split("\\\\n")) {
            linesCount++;
            if (line.length() >= longestLine.replaceAll("\\s+", "").length()) {
                longestLine = line;
            }
        }

        for (int i = 0; i < longestLine.length(); i++) {
            char c = longestLine.charAt(i);

            if (c == ' ') {
                width += Window.defaultFont.getGlyph('A').width();
                continue;
            }
            width += Window.defaultFont.getGlyph(c).width();
        }
        return new Dimension(width, linesCount * 28 + 16);
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
                        // TODO сломано специально
                        // drawTexture(video.x, video.y, video.width, video.height, 1, true, name.hashCode(), byteBuffer.get(name), SimpleColor.WHITE);
                        buff = byteBuffer.get(name);
                    }
                }
            }
        }
    }

    public static void updatePlayerPos() {
        playerX = DynamicObjects.getFirst().getX();
        playerY = DynamicObjects.getFirst().getY();

        camera.position.set(playerX + 32, playerY + 200);
        camera.update();

        batch.matrix(camera.projection);
    }

    public static void updateStaticObj() {
        batch.z(Layer.BACKGROUND);
        updateSun();
        ElectricCables.drawCables();
        batch.resetZ();

        updatePlayerPos();

        for (int x = (int) (playerX / blockSize) - 20; x < playerX / blockSize + 21; x++) {
            for (int y = (int) (playerY / blockSize) - 8; y < playerY / blockSize + blockSize; y++) {
                if (x < 0 || y < 0 || x > SizeX || y > SizeY) {
                    continue;
                }
                short obj = getObject(x, y);

                if (obj == -1 || StaticWorldObjects.getId(obj) == 0 || getTexture(obj) == null) {
                    continue;
                }
                byte hp = getHp(obj);
                if (hp <= 0) {
                    destroyObject(x, y);
                    continue;
                }

                int xBlock = findX(x, y);
                int yBlock = findY(x, y);

                if (isOnCamera(xBlock, yBlock)) {
                    SimpleColor color = ShadowMap.getColor(x, y);
                    int upperLimit = 100;
                    int lowestLimit = -20;
                    int maxColor = 65;
                    float temp = TemperatureMap.getTemp(x, y);

                    int a;
                    if (temp > upperLimit) {
                        a = (int) Math.min(maxColor, Math.abs((temp - upperLimit) / 3));
                        color = SimpleColor.fromRGBA(color.getRed(), color.getGreen() - (a / 2), color.getBlue() - a, color.getAlpha());

                    } else if (temp < lowestLimit) {
                        a = (int) Math.min(maxColor, Math.abs((temp + lowestLimit) / 3));
                        color = SimpleColor.fromRGBA(color.getRed() - a, color.getGreen() - (a / 2), color.getBlue(), color.getAlpha());
                    }

                    StaticWAnimations.AnimData currentFrame = StaticWAnimations.getCurrentFrame(obj, new Point(x, y));
                    if (currentFrame != null) {
                        drawTexture(xBlock, yBlock, currentFrame.width(), currentFrame.height(), 1, false, currentFrame.currentFrame() + StaticWorldObjects.getId(obj), currentFrame.currentFrameImage(), color);
                        continue;
                    }

                    batch.color(color);
                    batch.draw(getTexture(obj), xBlock, yBlock);

                    float maxHp = getMaxHp(obj);
                    if (hp > maxHp / 1.5f) {
                        // ???
                    } else if (hp < maxHp / 3) {
                        batch.draw(atlas.byPath("World/Blocks/damaged1.png"), xBlock, yBlock);
                    } else {
                        batch.draw(atlas.byPath("World/Blocks/damaged0.png"), xBlock, yBlock);
                    }
                    batch.resetColor();
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
        boolean interactionButtonPressed = input.pressed(interactionChar);

        if (root != null) {
            Runnable interaction = StaticWorldObjects.getOnInteraction(getObject(root.x, root.y));

            if (currentInteraction != null && currentInteraction.isAlive() && interactionButtonPressed) {
                currentInteraction.interrupt();
                return;
            }
            if (interaction != null) {
                int iconY = (root.y * blockSize) + blockSize;
                int iconX = (root.x * blockSize) + blockSize;
                batch.draw(atlas.byPath("UI/GUI/interactionIcon.png"), iconX, iconY);
                batch.draw(Window.defaultFont.getGlyph(interactionChar),
                        (root.x * blockSize + 16) + blockSize,
                        (root.y * blockSize + 12) + blockSize);

                if (interactionButtonPressed) {
                    currentInteraction = new Thread(interaction);
                    currentInteraction.start();
                }
            }
        }
    }

    public static boolean isOnCamera(int x, int y) {
        //todo очень редко проскакивает белая полоска снизу, возможно есть смысл немного повысить дальность отрисовки снизу
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

                if (isOnCamera((int) dynamicObject.getX(), (int) dynamicObject.getY())) {
                    if (dynamicObject.getFramesCount() == 0) {
                        batch.color(ShadowMap.getColorDynamic(dynamicObject));
                        batch.draw(dynamicObject.getTexture(), dynamicObject.getX(), dynamicObject.getY());
                        batch.resetColor();
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
        List<PanelObject> sortedPanels = panels.values().stream().sorted(Comparator.comparingInt(p -> p.layer)).toList();

        for (PanelObject panel : sortedPanels) {
            if (!panel.visible) {
                continue;
            }

            if (panel.texture != null) {
                batch.draw(panel.texture, panel.x, panel.y);
            } else {
                if (!panel.simple) {
                    Fill.rect(panel.x, panel.y, panel.width, panel.height, panel.color);
                    Fill.rectangleBorder(panel.x, panel.y, panel.width, panel.height, 20, panel.color);
                } else {
                    Fill.rect(panel.x, panel.y, panel.width, panel.height, panel.color);
                }
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
                Fill.rect(button.x, button.y, button.width, button.height, button.color);
                batch.draw(atlas.byPath("UI/GUI/checkMarkTrue.png"), button.x + button.width / 1.3f, button.y + button.height / 3f);
                drawText(button.x * 1.1f, button.y + button.height / 3f, button.name);
            } else if (button.simple) {
                Fill.rect(button.x, button.y, button.width, button.height, button.color);
                drawText(button.x * 1.1f, button.y + button.height / 3f, button.name);
            } else {
                //if swap and not simple
                if (button.isClicked) {
                    Fill.rectangleBorder(button.x - 6, button.y - 6, button.width, button.height, 6, button.color);
                    batch.draw(atlas.byPath("UI/GUI/checkMarkTrue.png"), button.x, button.y);
                    drawText(button.width + button.x + 24, button.y, button.name);
                } else {
                    Fill.rectangleBorder(button.x - 6, button.y - 6, button.width, button.height, 6, button.color);
                    batch.draw(atlas.byPath("UI/GUI/checkMarkFalse.png"), button.x, button.y);
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

            if (button.texture != null) {
                batch.draw(button.texture, button.x, button.y);
            } else {
                if (button.simple) {
                    Fill.rect(button.x, button.y, button.width, button.height, button.color);
                } else {
                    Fill.rectangleBorder(button.x, button.y, button.width, button.height, 6, button.color);
                }

                if (!button.isClickable) {
                    Fill.rect(button.x, button.y, button.width, button.height, SimpleColor.fromRGBA(0, 0, 0, 123));
                }
                drawText(button.x + 20, button.y + button.height / 2.8f, button.name);
                drawPrompt(button);
            }
        }
    }

    private static void updateSliders() {
        for (SliderObject slider : sliders.values()) {
            if (!slider.visible) {
                continue;
            }
            Fill.rect(slider.x, slider.y, slider.width, slider.height, slider.sliderColor);
            batch.color(slider.dotColor);
            Fill.circle(slider.sliderPos, slider.y - 5, slider.height * 1.5f);
            batch.resetColor();
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
            Fill.rect(20, 800, 650, 260, SimpleColor.fromRGBA(0, 0, 0, 220));
            drawRectangleText(-10, 810, 630, EventHandler.keyLoggingText.toString(), true, SimpleColor.CLEAR);
        }
    }
}
