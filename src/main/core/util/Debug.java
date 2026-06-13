package core.util;

import core.GameState;
import core.PlayGameScene;
import core.Time;
import core.World.TemperatureMap;
import core.World.Weather.Sun;
import core.content.blocks.Block;
import core.content.blocks.data.TileData;
import core.content.items.Item;
import core.g2d.Fill;
import core.g2d.Font;
import core.g2d.Render;
import core.g2d.StackfulRender;
import core.graphic.Color;
import core.graphic.GuiDrawing;
import core.graphic.ShadowMap;
import core.math.TmpShapes;
import core.math.Vector2d;
import core.ui.Styles;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import static core.Application.log;
import static core.Global.*;
import static core.WorldCoordinates.toBlock;
import static core.WorldCoordinates.toWorld;
import static core.content.ItemStack.itemStack;
import static core.content.entity.DrawComponent.GAP;
import static core.graphic.Color.*;
import static core.graphic.GuiDrawing.drawTextUncached;
import static org.lwjgl.glfw.GLFW.*;

public class Debug {
    public static final DecimalFormat FLOATS = new DecimalFormat("#.####", new DecimalFormatSymbols(Locale.ROOT));

    public static final int debugLevel = Config.getInt("Debug");

    // Директория ./tmp для созданных картинок и прочего
    public static final String TEMP_DIR = "tmp";

    // Включается по нажатию F3+M английской
    public static boolean debugMesh = false;

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void rethrow(Throwable t) throws T {
        throw (T) t;
    }

    // region .MENU
    public static void initMenu() {
        if (debugLevel < 2 || once2) {
            return;
        }
        once2 = true;
        Debug.initDebugValuesMenu();
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

    static String formatWorldPos(Vector2d pos) {
        int x = toBlock(pos.x);
        int y = toBlock(pos.y);
        float offsetX = (float) (pos.x - x);
        float offsetY = (float) (pos.y - y);
        return FLOATS.format((double)x + offsetX) + " / " + FLOATS.format((double)y + offsetY);
    }

    static boolean once, once2;

    public static void initDebugValuesGame() {
        if (debugLevel < 2 || once) {
            return;
        }
        once = true;

        setDebugValue(GameState.PLAYING, () -> {
            var worldPos = TmpShapes.v1d.set(player.lastX(), player.lastY());
            return "Last XY: " + formatWorldPos(worldPos);
        });
        setDebugValue(GameState.PLAYING, () -> {
            var worldPos = player.posTo(TmpShapes.v1d);
            return "XY: " + formatWorldPos(worldPos);
        });
        setDebugValue(GameState.PLAYING, () -> "Camera: " + formatWorldPos(camera.position));
        setDebugValue(GameState.PLAYING, () -> "Velocity: " + player.velocity());
        setDebugValue(GameState.PLAYING, () ->
                "HP: " + FLOATS.format(player.hp()) +
                "/" + FLOATS.format(player.maxHp()) + " (" + FLOATS.format(player.hpFract()) + ")");
        setDebugValue(GameState.PLAYING, () -> {
            var mouseBlockPos = input.mouseBlockPos();
            var mouseBlock = world.getBlock(mouseBlockPos);
            String blockId = mouseBlock != null ? mouseBlock.key + " (BID: " + mouseBlock.id + ")" : "<void>";
            return "Mouse: " + mouseBlockPos + " ID: " + blockId + " HP: " + world.getHp(mouseBlockPos) +
                   " Shadow: " + ShadowMap.getColorTo(mouseBlockPos.x, mouseBlockPos.y, TmpShapes.c1);
        });
        setDebugValue(GameState.PLAYING, () -> {
            var gs = (PlayGameScene) gameScene;
            Sun sun = gs.sun;
            return "Sun y: " + FLOATS.format((int)(sun.y * 100f) / 100f);
        });
    }


    public static void drawDebugBorders() {
        if (debugLevel < 2) {
            return;
        }

        StackfulRender.z(Render.LAYER_DEBUG);

        entityPool.forEach(ent -> {
            var pos = camera.projectTo(ent.posTo(TmpShapes.v1d), TmpShapes.v1f);
            Fill.rectangleBorder(pos.x, pos.y, ent.width(), ent.height(), white);
            GuiDrawing.drawText(pos.x, pos.y, "HasFloor: " + ent.hasFloor(), black);
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

    interface PixelOperator {
        int apply(int x, int y);
    }

    private static void saveTempData(String name, @Nullable BatchScope scope, PixelOperator pixelAct) {
        Executor ex = scope;
        if (ex == null)
            ex = Thread.ofPlatform()::start;

        log.debug("Saving {}.png", name);
        ex.execute(() -> {
            BufferedImage image = new BufferedImage(world.sizeX, world.sizeY, BufferedImage.TYPE_INT_RGB);
            Path path = assets.workingDir().resolve(TEMP_DIR, name + ".png");
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                log.error("Failed to create dir: {}", path, e);
                return;
            }
            for (int y = 0; y < world.sizeY; y++) {
                for (int x = 0; x < world.sizeX; x++) {
                    int ry = (world.sizeY - 1) - y;
                    image.setRGB(x, ry, pixelAct.apply(x, y));
                }
            }
            try {
                ImageIO.write(image, "png", path.toFile());
                log.debug("Saving done {}.png", name);
            } catch (IOException e) {
                log.error("", e);
            }
        });
    }

    public static void saveWorldImage() {
        if (debugLevel < 2) {
            return;
        }
        log.debug("Saving world image..");
        Thread.ofPlatform().start(() -> {
            // у виртуальных нет имени)
            // FIXME(Skat): забавный факт. на большом мире сохранение изображение настолько долгое, что если выйти из мира, то он упадёт)
            Thread.currentThread().setName("WorldImageSaver");
            BufferedImage image = new BufferedImage(world.sizeX, world.sizeY, BufferedImage.TYPE_INT_RGB);
            Path path = assets.workingDir().resolve(TEMP_DIR, "worldImage.png");
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                log.error("Failed to create dir: {}", path, e);
                return;
            }

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

    public static void saveTemp(String name, BatchScope scope) {
        saveTempData(name, scope, (x, y) -> {
            return (255 << 24) | ((int)Math.clamp((TemperatureMap.getTempCell(x, y) - 20f) / 980f * 255f, 0, 255) << 16);
        });
    }

    public static void savePressures(String name, BatchScope scope) {
        saveTempData(name, scope, (x, y) -> {
            return (255 << 24) | ((int)Math.clamp((TemperatureMap.getPressure(x, y) / 1000 - 20f) / 980f * 255f, 0, 255) << 16);
        });
    }

    public static void saveDens(String name, BatchScope scope) {
        saveTempData(name, scope, (x, y) -> {
            return (255 << 24) | ((int)Math.clamp((TemperatureMap.getDensity(x, y) * 300 - 20f) / 980f * 255f, 0, 255) << 16);
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
        // TODO нужны комбинации клавиш на уровне InputHandler
        if (input.justPressed(GLFW_KEY_F3) && input.justPressed(GLFW_KEY_M)) {
            debugMesh = !debugMesh;
        }
    }

    public static void drawPlayerBorders() {
        if (debugLevel < 2) {
            return;
        }

        StackfulRender.z(Render.LAYER_DEBUG);
        Fill.lineWidth(toWorld(1));

        if (!player.isDead()) {
            var hitbox = TmpShapes.aabb1;
            player.hitboxTo(hitbox);

            if (false) { // Блоки интегрированной модели
                short minX = hitbox.blockMinX();
                short minY = hitbox.blockMinY();
                short maxX = hitbox.blockMaxX();
                short maxY = hitbox.blockMaxY();

                for (short y = minY; y <= maxY; y++) {
                    for (short x = minX; x <= maxX; x++) {
                        Fill.rectangleBorder(x, y, 1, 1, white);
                    }
                }
            }
            { // Блоки которые считаются за пол. Черная обводка

                hitbox.maxY = hitbox.minY;
                hitbox.minY -= GAP;
                hitbox.maxX -= GAP;
                hitbox.minX += GAP;

                short minX = hitbox.blockMinX();
                short maxX = hitbox.blockMaxX();
                short minY = hitbox.blockMinY();
                short maxY = hitbox.blockMaxY();

                for (short y = minY; y <= maxY; y++) {
                    for (short x = minX; x <= maxX; x++) {
                        var block = world.getBlock(x, y);
                        if (block == null) continue;
                        if (block.type == Block.Type.SOLID) {
                            Fill.rectangleBorder(x, y, block.tileCountX, block.tileCountY, black);
                        }
                    }
                }
            }
        }

        { // Корень красный, дочерние синие
            var hitbox = TmpShapes.aabb1;
            camera.boundsTo(hitbox);
            hitbox.clampToWorld();

            short minX = hitbox.blockMinX();
            short minY = hitbox.blockMinY();
            short maxX = hitbox.blockMaxX();
            short maxY = hitbox.blockMaxY();

            for (short y = minY; y <= maxY; y++) {
                for (short x = minX; x <= maxX; x++) {
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

    static final ObjectArrayList<Supplier<String>> debugValues = new ObjectArrayList<>();

    ///фактически можно вызывать откуда угодно, но рекомендуется ставить в DebugTools.initDebugValuesGame() или DebugTools.initDebugValuesMenu()
    public static void setDebugValue(Supplier<String> format) {
        if (Debug.debugLevel <= 0) {
            return;
        }
        debugValues.add(format);
    }

    public static void setDebugValue(GameState state, Supplier<String> format) {
        if (Debug.debugLevel <= 0) {
            return;
        }

        debugValues.add(() -> {
            if (state == gameState)
                return format.get();
            return null;
        });
    }

    static final int debugTextBackColor = Color.rgba8888(0, 0, 0, 255 / 2);

    public static void drawTextValues() {
        if (Debug.debugLevel <= 0) {
            return;
        }
        StackfulRender.z(Render.LAYER_DEBUG);
        StackfulRender.blending(Render.BLENDING_PREMUL);
        var font = Styles.DEBUG_TEXT.font;
        int color = Styles.DEBUG_TEXT.color.rgba8888();

        float bx = 0;
        float by = input.viewportHeight() - font.lineHeight();
        var list = debugValues;
        {
            float y = by;
            for (var debugValue : list) {
                String value = debugValue.get();
                if (value == null) continue;
                float textWidth = textWidth(font, value);
                Fill.rect(bx, y, textWidth, font.lineHeight(), debugTextBackColor);
                y -= font.lineHeight();
            }
        }
        for (var debugValue : list) {
            String value = debugValue.get();
            if (value == null) continue;
            drawTextUncached(font, bx, by, value, color);
            by -= font.lineHeight();
        }
        // float width = 200;
        // float height = 100;
        // renderFrameTimeGraph(input.width() - width, input.height() - height, width, height);
        StackfulRender.blending(Render.BLENDING_NORMAL);
    }

    static float textWidth(Font font, String text) {
        float w = 0;
        for (int i = 0, n = text.length(); i < n; i++) {
            char c = text.charAt(i);
            w += font.getGlyph(c).width();
        }
        return w;
    }

    /*
    static final int background2 = Color.rgba8888(0, 0, 0, 255 / 3);

    public static void renderFrameTimeGraph(float x, float y, float width, float height) {
        Fill.rect(x, y, width, height, background2);
        Fill.lineWidth(1);

        var profiler = app.profiler;
        int samples = profiler.maxSamples();
        float xStep = width / (samples - 1);

        int targetFrametime = app.framerate() > 0 ? app.framerate() : 60;
        float targetMs = 1000f / targetFrametime;
        float maxVisibleMs = targetMs * 2f;

        float targetY = y + targetMs / maxVisibleMs * height;
        Fill.line(x, targetY, x + width, targetY, green);
        Fill.lineWidth(1.85f);

        for (int i = 0; i < samples - 1; i++) {
            float currentMs = profiler.getSample(i);
            float nextMs = profiler.getSample(i + 1);

            float x1 = x + i * xStep;
            float x2 = x + (i + 1) * xStep;

            float y1 = y + Math.min(currentMs / maxVisibleMs, 1f) * height;
            float y2 = y + Math.min(nextMs / maxVisibleMs, 1f) * height;

            int color = currentMs > targetMs + 1.0f ? red : white;

            Fill.line(x1, y1, x2, y2, color);
        }

        Fill.resetLineWidth();
    }*/
}
