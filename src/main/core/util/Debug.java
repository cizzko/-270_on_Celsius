package core.util;

import core.EventHandling.Config;
import core.GameState;
import core.PlayGameScene;
import core.Time;
import core.UI.Dialog;
import core.UI.Styles;
import core.UI.TextArea;
import core.World.Weather.Sun;
import core.content.blocks.Block;
import core.content.blocks.data.TileData;
import core.content.entity.Hitbox;
import core.content.items.Item;
import core.g2d.Fill;
import core.g2d.Render;
import core.g2d.StackfulRender;
import core.graphic.Color;
import core.graphic.GuiDrawing;
import core.graphic.ShadowMap;
import core.math.Rectangle;
import core.math.TmpShapes;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.Supplier;

import static core.Application.log;
import static core.Global.*;
import static core.WorldCoordinates.toBlock;
import static core.WorldCoordinates.toWorld;
import static core.content.ItemStack.itemStack;
import static core.graphic.Color.*;
import static org.lwjgl.glfw.GLFW.*;

public class Debug {
    public static final DecimalFormat FLOATS = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.ROOT));

    public static final int debugLevel = Config.getInt("Debug");

    static final Rectangle rect = new Rectangle();
    static final Hitbox hitbox = new Hitbox();

    // Включается по нажатию M английской
    public static boolean debugMesh = false;

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void rethrow(Throwable t) throws T {
        throw (T) t;
    }

    // region GameState.MENU
    public static void initMenu() {
        if (debugLevel < 2) {
            return;
        }

    }

    // endregion
    // region GameState.PLAYING
    public static void initPlaying() {
        // TODO: Дефолтные предметы в отдельном json
        giveItems();
        initDebugValuesGame();
    }

    // endregion


    public static void initDebugValuesMenu() {
        if (debugLevel < 1) {
            return;
        }
        setDebugValue(() -> "RenderFPS: " + app.fps());

        if (debugLevel < 2) {
            return;
        }

        setDebugValue(() -> "DeltaTime: " + Time.delta);
    }

    static boolean once;

    public static void initDebugValuesGame() {
        if (debugLevel < 2 || once) {
            return;
        }
        once = true;

        setDebugValue(GameState.PLAYING, () -> {
            var gs = (PlayGameScene) gameScene;
            Sun sun = gs.sun;
            return "Sun y: " + (int) (sun.y * 100) / 100f;
        });
        setDebugValue(GameState.PLAYING, () -> "XY: " + player.x() + " / " + player.y());
        setDebugValue(GameState.PLAYING, () -> "Camera: " + camera.position.x + " / " + camera.position.y);
        setDebugValue(GameState.PLAYING, () -> "Velocity: " + player.velocity());
        setDebugValue(GameState.PLAYING, () -> "HP: " + player.getHp() + "/" + player.getMaxHp() + " (" + player.getHpFraction() + ")");
        setDebugValue(GameState.PLAYING, () -> {
            var mouseBlockPos = input.mouseBlockPos();
            var mouseBlock = world.getBlock(mouseBlockPos);
            String blockId = mouseBlock != null ? mouseBlock.key + " (BID: " + mouseBlock.id + ")" : "<void>";
            return "Mouse: " + mouseBlockPos + " ID: " + blockId + " HP: " + world.getHp(mouseBlockPos) +
                   " Shadow: " + ShadowMap.getColorTo(mouseBlockPos.x, mouseBlockPos.y, TmpShapes.c2);
        });
    }


    public static void drawDebugBorders() {
        if (debugLevel < 2) {
            return;
        }

        StackfulRender.z(Render.LAYER_DEBUG);

        entityPool.forEach(ent -> {
            ent.getHitboxTo(rect);
            var pos = camera.project(TmpShapes.v1.set(rect.x, rect.y));
            GuiDrawing.drawText(pos.x, pos.y,
                    "HasFloor: " + ent.hasFloor(), black);
        });
    }

    public static void giveItems() {
        final int n = 10;

        player.addItem(itemStack(content.itemById("blockDeleter"), Item.DEFAULT_MAX_STACK_SIZE));
        player.addItem(itemStack(content.itemById("aluminum"), n));
        player.addItem(itemStack(content.itemById("chest"), n));
        player.addItem(itemStack(content.itemById("stick"), n));
        player.addItem(itemStack(content.itemById("redHammer"), n));
        player.addItem(itemStack(content.itemById("grass"), n));
        player.addItem(itemStack(content.itemById("workbenchSmall"), n));
        player.addItem(itemStack(content.itemById("workbenchMedium"), n));
        player.addItem(itemStack(content.itemById("smallStone"), n));
        player.addItem(itemStack(content.itemById("stoneOven"), n));
    }

    public static void saveWorldImage() {
        if (debugLevel < 2) {
            return;
        }
        log.debug("Saving world image..");
        Thread.startVirtualThread(() -> {
            // у виртуальных нет имени)
            // FIXME(Skat): забавный факт. на большом мире сохранение изображение настолько долгое, что если выйти из мира, то он упадёт)
            Thread.currentThread().setName("WorldImageSaver");
            BufferedImage image = new BufferedImage(world.sizeX, world.sizeY, BufferedImage.TYPE_INT_RGB);
            Path path = assets.workingDir().resolve("worldImage.png");
            for (int y = 0; y < world.sizeY; y++) {
                for (int x = 0; x < world.sizeX; x++) {
                    int block = world.getBlockId(x, y);
                    if (block != 0) {
                        image.setRGB(x, (world.sizeY - 1) - y, 0xFFFFFF);
                    } else {
                        image.setRGB(x, (world.sizeY - 1) - y, 0x000000);
                    }
                }
            }
            try {
                ImageIO.write(image, "png", path.toFile());
                log.debug("Saving world image done");
            } catch (IOException e) {
                log.error("", e);
            }
        });
    }

    public static void debugHotKeys() {
        if (debugLevel < 2) {
            return;
        }

        if (input.justPressed(GLFW_KEY_F1)) {
            app.setFramerate(60);
        }
        if (input.justPressed(GLFW_KEY_F2)) {
            app.setFramerate(1000);
        }
        debugUIHotkeys();
    }

    public static void menuHotKeys() {
        if (debugLevel < 2) {
            return;
        }
        debugUIHotkeys();
    }

    static void debugUIHotkeys() {
        if (input.justPressed(GLFW_KEY_F9)) {
            uiScene.toggleDebug();
        }
        if (input.justPressed(GLFW_KEY_F10)) {
            uiScene.debug();
        }
        if (input.justPressed(GLFW_KEY_M)) {
            debugMesh = !debugMesh;
        }
    }

    public static void drawPlayerBorders() {
        if (debugLevel < 2) {
            return;
        }

        StackfulRender.z(Render.LAYER_DEBUG);
        Fill.lineWidth(toWorld(1));

        camera.getBoundsTo(rect);

        if (!player.isDead()) {
            player.getHitboxTo(rect);
            { // Блоки интегрированной модели
                int minX = toBlock(rect.x);
                int minY = toBlock(rect.y);
                int maxX = minX + toBlock(rect.width);
                int maxY = minY + toBlock(rect.height);

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        Fill.rectangleBorder(x, y, 1, 1, white);
                    }
                }
            }

            { // Блоки которые считаются за пол. Черная обводка
                int minX = toBlock(player.x());
                int maxX = toBlock(player.x() + toWorld(player.creature.texture.width()));
                int minY = toBlock(player.y());

                for (int x = minX; x <= maxX; x++) {
                    var block = world.getBlock(x, minY);
                    if (block == null) continue;
                    if (block.type == Block.Type.SOLID) {
                        Fill.rectangleBorder(x, minY, block.tileCountX, block.tileCountY, black);
                    }
                }
            }
        }

        { // Корень красный, дочерние синие
            camera.getBoundsTo(rect);
            hitbox.set(rect);
            hitbox.clamp(world.sizeX, world.sizeY);

            int minX = hitbox.minX;
            int maxX = hitbox.maxX;
            int minY = hitbox.minY;
            int maxY = hitbox.maxY;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    var obj = world.getBlock(x, y);
                    if (obj == null || obj == Block.AIR) {
                        continue;
                    }

                    var data = world.getData(x, y);
                    if (data instanceof TileData.MultiblockPart) {
                        Fill.rectangleBorder(x, y, 1, 1, blue);
                    } else if (world.getRootBlockPosTo(x, y, TmpShapes.p1) && TmpShapes.p1.equals(x, y)) {
                        Fill.rectangleBorder(x, y, 1, 1, red);
                    }
                }
            }
        }

        Fill.resetLineWidth();
    }

    static final class DebugBox extends TextArea {
        static final int background = Color.rgba8888(0, 0, 0, 255 / 2);

        final GameState state;
        final Supplier<String> format;

        DebugBox(GameState state, Supplier<String> format) {
            super(debugValues, Styles.DEBUG_TEXT);
            this.state = state;
            this.format = format;
        }

        @Override
        public void updateThis(float dt) { if (state == null || gameState == state) setText(format.get()); }

        @Override
        public void draw() {
            Fill.rect(x, y, cache.rect.width, cache.rect.height, background);
            super.draw();
        }
    }
    static final Dialog debugValues = new Dialog();

    ///фактически можно вызывать откуда угодно, но рекомендуется ставить в DebugTools.initDebugValuesGame() или DebugTools.initDebugValuesMenu()
    public static void setDebugValue(Supplier<String> format) {
        setDebugValue(null, format);
    }

    public static void setDebugValue(@Nullable GameState state, Supplier<String> format) {
        if (Debug.debugLevel <= 0) {
            return;
        }

        var elem = new DebugBox(state, format);
        debugValues.add(elem);
        int i = debugValues.children().size();
        elem.setPosition(5, 1080 - (25 * i));
    }

    public static void drawTextValues() {
        if (Debug.debugLevel <= 0) {
            return;
        }
        StackfulRender.z(Render.LAYER_DEBUG);
        StackfulRender.blending(Render.BLENDING_PREMUL);
        debugValues.update(42);
        debugValues.draw();
        StackfulRender.blending(Render.BLENDING_NORMAL);
    }
}
