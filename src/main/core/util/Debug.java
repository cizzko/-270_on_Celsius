package core.util;

import core.*;
import core.EventHandling.Config;
import core.UI.Dialog;
import core.UI.Styles;
import core.UI.TextArea;
import core.World.Creatures.Player.Inventory.Inventory;
import core.content.blocks.Block;
import core.World.Weather.Sun;
import core.World.World;
import core.World.WorldUtils;
import core.content.blocks.data.TileData;
import core.content.entity.Entity;
import core.content.entity.Hitbox;
import core.content.items.Item;
import core.g2d.Fill;
import core.g2d.Render;
import core.g2d.StackfulRender;
import core.graphic.GuiDrawing;
import core.math.Point2i;
import core.math.Rectangle;
import core.math.TmpShapes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.function.Supplier;

import static core.Application.log;
import static core.EventHandling.Config.json;
import static core.Global.*;
import static core.WorldCoordinates.*;
import static core.content.ItemStack.itemStack;
import static core.graphic.Color.*;
import static org.lwjgl.glfw.GLFW.*;

public class Debug {
    public static final DecimalFormat FLOATS = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.ROOT));

    public static final int debugLevel = Config.getInt("Debug");

    static final Rectangle rect = new Rectangle();
    static final Hitbox hitbox = new Hitbox();
    static final int acid = 0x8ffe09ff;

    // Включается по нажатию M английской
    public static boolean debugMesh = false;

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> void rethrow(Throwable t) throws T {
        throw (T) t;
    }

    public static void deserializeWorld() {
        long t = System.currentTimeMillis();
        try {
            var reader = Files.readString(assets.workingDir().resolve("open worl.json"));
            World w = json.readValue(reader, World.class);
            var tree = json.valueToTree(w);
            Files.writeString(assets.workingDir().resolve("open worl (2).json"), tree.toString());

            var refTree = json.readTree(reader);
            if (!tree.equals(refTree)) {
                log.info("РАЗНЫЕ !!!");
            }

        } catch (Exception e) {
            log.error("", e);
        }
        log.info("Time took: {}ms", (System.currentTimeMillis() - t));
    }

    public static void serializeTargetBlock() {
        Point2i blockUnderMouse = Global.input.mouseBlockPos();

        if (world.getBlockId(blockUnderMouse.x, blockUnderMouse.y) > 0) {
            var blockEntity = world.getEntity(blockUnderMouse.x, blockUnderMouse.y);
            if (blockEntity != null) {
                long t = System.currentTimeMillis();

                var str = new StringWriter();
                try (var out = json.createGenerator(str)) {
                    blockEntity.serialize(out, json.getSerializerProvider());
                } catch (Exception e) {
                    log.error("", e);
                }

                log.info("Time took: {}ms", (System.currentTimeMillis() - t));
            }
        }
    }

    public static void serializeWorld() {
        long t = System.currentTimeMillis();
        try {
            Files.writeString(assets.workingDir().resolve("open worl.json"), Config.json.writeValueAsString(world));
        } catch (Exception e) {
            log.error(e);
        }
        log.info("Time took: {}ms", (System.currentTimeMillis() - t));
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
        setDebugValue(() -> "RenderFPS: " + Global.app.getFps());

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


        setDebugValue(() -> {
            if (gameState != GameState.PLAYING) return null;
            if (gameScene instanceof PlayGameScene) {
                Sun sun = ((PlayGameScene) gameScene).sun;
                return "Sun y: " + (int) (sun.y * 100) / 100f;
            }
            return null;
        });
        setDebugValue(() -> {
            if (gameState != GameState.PLAYING) return null;
            return "[Player] x: " + player.x() + ", y: " + player.y();
        });
        setDebugValue(() -> "Camera Pos: " + camera.position);
        setDebugValue(() ->{
            if (gameState != GameState.PLAYING) return null;
            return "Velocity: " + player.velocity();
        });
        setDebugValue(() -> {
            if (gameState != GameState.PLAYING) return null;
            return "PlayerHp: " + player.getHp();
        });

        setDebugValue(() -> {
            if (gameState != GameState.PLAYING) return null;
            var mouseBlockPos = (input.mouseBlockPos());
            var mouseBlock = world.getBlock(mouseBlockPos.x, mouseBlockPos.y);
            return "MouseBlock: " + mouseBlockPos + " " + (mouseBlock != null ? mouseBlock.id + " (BID: " + Global.content.blocksRegistry.idByType(mouseBlock) + ")" : "<void>");
        });
        setDebugValue(() -> {
            if (gameState != GameState.PLAYING) return null;
            var mouseBlockPos = (input.mouseBlockPos());
            return "BlockHp: " + world.getHp(mouseBlockPos.x, mouseBlockPos.y);
        });
    }


    public static void drawDebugBorders() {
        if (debugLevel < 2) {
            return;
        }

        StackfulRender.z(Render.LAYER_DEBUG);

        for (Entity ent : entityPool.entities().values()) {
            ent.getHitboxTo(rect);
            var pos = camera.project(TmpShapes.v1.set(rect.x, rect.y));
            GuiDrawing.drawText(pos.x, pos.y,
                    "HasFloor: " + ent.hasFloor(), black);
        }
    }

    public static void giveItems() {
        final int n = 10;

        Inventory.addItemStack(itemStack(content.itemById("blockDeleter"), Item.DEFAULT_MAX_STACK_SIZE));
        Inventory.addItemStack(itemStack(content.itemById("aluminum"), n));
        Inventory.addItemStack(itemStack(content.itemById("chest"), n));
        Inventory.addItemStack(itemStack(content.itemById("stick"), n));
        Inventory.addItemStack(itemStack(content.itemById("redHammer"), n));
        Inventory.addItemStack(itemStack(content.itemById("grass"), n));
        Inventory.addItemStack(itemStack(content.itemById("workbenchSmall"), n));
        Inventory.addItemStack(itemStack(content.itemById("workbenchMedium"), n));
        Inventory.addItemStack(itemStack(content.itemById("smallStone"), n));
        Inventory.addItemStack(itemStack(content.itemById("stoneOven"), n));
    }

    public static void saveWorldImage() {
        if (debugLevel < 2) {
            return;
        }
        Thread.startVirtualThread(() -> {
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
        //if (input.justClicked(GLFW_MOUSE_BUTTON_RIGHT)) setStructureUnderMouse();
        // if (input.justPressed(GLFW_KEY_F3)) serializeWorld();
        //if (input.justPressed(GLFW_KEY_F4)) deserializeWorld();
        // if (input.justClicked(GLFW_MOUSE_BUTTON_RIGHT)) serializeTargetBlock();
        debugUIHotkeys();
    }

    static void setStructureUnderMouse() {
        Point2i pointedBlock = input.mouseBlockPos();
        if (Global.world.getBlockId(pointedBlock) == 0) {
            var tree = content.structuresRegistry.typeByName("tree");
            WorldUtils.setStructure(pointedBlock.x, pointedBlock.y, tree);
        }
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
        final Supplier<String> format;

        DebugBox(Supplier<String> format) {
            super(debugDialog, Styles.DEBUG_TEXT);
            this.format = format;
        }

        @Override
        public void updateThis(float dt) { setText(format.get()); }
    }
    static final Dialog debugDialog = new Dialog();

    ///фактически можно вызывать откуда угодно, но рекомендуется ставить в DebugTools.initDebugValuesGame() или DebugTools.initDebugValuesMenu()
    public static void setDebugValue(Supplier<String> format) {
        if (Debug.debugLevel <= 0) {
            return;
        }

        var elem = new DebugBox(format);
        debugDialog.add(elem);
        int i = debugDialog.children().size();
        elem.setPosition(5, 1080 - (25 * i));
    }

    public static void drawTextValues() {
        if (Debug.debugLevel <= 0) {
            return;
        }
        StackfulRender.z(Render.LAYER_DEBUG);
        debugDialog.update(42);
        debugDialog.draw();
    }
}
