package core.util;

import core.Application;
import core.EventHandling.Config;
import core.EventHandling.EventHandler;
import core.GameState;
import core.Global;
import core.Time;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.TileData;
import core.World.Textures.TextureDrawing;
import core.World.World;
import core.g2d.Fill;
import core.math.Point2i;
import core.math.Rectangle;
import it.unimi.dsi.fastutil.HashCommon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static core.Application.*;
import static core.EventHandling.Config.json;
import static core.EventHandling.EventHandler.debugLevel;
import static core.EventHandling.EventHandler.setDebugValue;
import static core.Global.*;
import static core.World.Creatures.Physics.swap;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.content.entity.DrawComponent.GAP;
import static core.util.Color.BLACK;
import static core.util.Color.rgba8888;
import static org.lwjgl.glfw.GLFW.*;

public class DebugTools {
    public static final DecimalFormat FLOATS = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.ROOT));

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
            log.error(e);
            e.printStackTrace();
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
                    log.error(e);
                }
                System.out.println(str);

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

    public static void initDebugValuesGame() {
        if (debugLevel < 2) {
            return;
        }
        // TODO: Дефолтные предметы в отдельном json
        DebugTools.giveItems();

        setDebugValue(() -> "[Player] x: " + player.getX() + ", y: " + player.getY());
        setDebugValue(() -> "Camera Pos: " + camera.position);
        setDebugValue(() -> "Velocity: " + player.getVelocity());
        setDebugValue(() -> "PlayerHp: " + player.getHp());

        setDebugValue(() -> {
            var mouseBlockPos = (input.mouseBlockPos());
            var mouseBlock = world.getBlock(mouseBlockPos.x, mouseBlockPos.y);
            return "MouseBlock: " + mouseBlockPos + " " + (mouseBlock != null ? mouseBlock.id + " (NID: " + Global.content.blocksRegistry.idByType(mouseBlock) + ")" : "<void>");
        });
        setDebugValue(() -> {
            var mouseBlockPos = (input.mouseBlockPos());
            return "BlockHp: " + world.getHp(mouseBlockPos.x, mouseBlockPos.y);
        });
        //setDebugValue(() -> "Current time: " + sun.currentTime);
    }

    static final Rectangle rect = new Rectangle();
    static final int red = rgba8888(255, 0, 0, 255);
    static final int blue = rgba8888(0, 0, 255, 255);
    static final int white = rgba8888(255, 255, 255, 255);
    static final int acid = 0x8ffe09ff;
    static final int black = rgba8888(0, 0, 0, 255);

    public static int leftInt(long field) { return (int)(field >> 32); }
    public static int rightInt(long field) { return (int)(field); }

    public static void drawDebugBorders() {
        if (debugLevel < 2) {
            return;
        }

        entityPool.entities().values().forEach(e -> {
            e.getHitboxTo(rect);
            Fill.rectangleBorder(rect.x, rect.y, rect.width, rect.height, red);
            // TextureDrawing.drawText(rect.x, rect.y,
            //         "HasFloor: " + e.hasFloor(), black);
        });

        if (debugLevel >= 3) {
            var r = entityPool.worldIndex().resolution;
            var hashIndex = entityPool.worldIndex().hash;
            hashIndex.keySet().forEach(hash -> {
                long key = HashCommon.invMix(hash);
                float x = leftInt(key) * r;
                float y = rightInt(key) * r;

                Fill.rectangleBorder(x, y, r, r, acid);
                var group = hashIndex.get(hash);
                TextureDrawing.drawText(x, y, "GroupSize: " + group.size());
            });
        }

        camera.getBoundsTo(rect);
        // правая граница
        Fill.line(
                (world.sizeX) * blockSize, rect.y,
                (world.sizeX) * blockSize, rect.y + rect.height,
                4,
                red);
        Fill.line(
                (world.sizeX - swap) * blockSize, rect.y,
                (world.sizeX - swap) * blockSize, rect.y + rect.height,
                4,
                black);
        // левая граница
        Fill.line(
                0, rect.y,
                0, rect.y + rect.height,
                4,
                blue);
        Fill.line(
                swap * blockSize, rect.y,
                swap * blockSize, rect.y + rect.height,
                4,
                black);

        if (!player.isDead()) {
            player.getHitboxTo(rect);
            { // Блоки интегрированной модели
                int minX = (int) Math.floor(rect.x / blockSize);
                int minY = (int) Math.floor(rect.y / blockSize);
                int maxX = (int) Math.floor((rect.x + rect.width) / blockSize);
                int maxY = (int) Math.floor((rect.y + rect.height) / blockSize);
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        Fill.rectangleBorder(x * blockSize, y * blockSize, blockSize, blockSize, white);
                    }
                }
            }

            { // Блоки которые считаются за пол. Черная обводка
                int minX = (int) Math.floor(player.getX() / blockSize);
                int maxX = (int) Math.floor((player.getX() + player.creature.texture.width() - GAP) / blockSize);
                int minY = (int) Math.floor((player.getY() - GAP) / blockSize);

                for (int x = minX; x <= maxX; x++) {
                    var block = world.getBlock(x, minY);
                    if (block == null || block.type == StaticObjectsConst.Type.SOLID) {
                        Fill.rectangleBorder(x*blockSize,minY*blockSize, blockSize, blockSize, BLACK);
                    }
                }
            }
        }

        { // Корень красный, дочерние синие
            camera.getBoundsTo(rect);
            int minX = (int) Math.floor(rect.x / blockSize);
            int maxX = (int) Math.floor((rect.x + rect.width) / blockSize);
            int minY = (int) Math.floor(rect.y / blockSize);
            int maxY = (int) Math.floor((rect.y + rect.height) / blockSize);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    if (!world.inBounds(x, y)) {
                        continue;
                    }

                    var obj = world.getBlock(x, y);
                    if (obj == null || obj == StaticObjectsConst.AIR) {
                        continue;
                    }

                    var data = world.getData(x, y);
                    if (data instanceof TileData.MultiblockPart) {
                        Fill.rectangleBorder(x * blockSize, y * blockSize, blockSize, blockSize, blue);
                    } else {
                        var rootPos = world.getRootBlockPos(x, y);
                        if (rootPos != null && rootPos.x == x && rootPos.y == y) {
                            Fill.rectangleBorder(x * blockSize, y * blockSize, blockSize, blockSize, red);
                        }
                    }
                }
            }
        }
    }

    public static void giveItems() {
        final int n = 10;

        Inventory.addItem(content.itemById("blockDeleter"));
        Inventory.addItemStack(new ItemStack(content.itemById("aluminum"), n));
        Inventory.addItemStack(new ItemStack(content.itemById("chest"), n));
        Inventory.addItemStack(new ItemStack(content.itemById("stick"), n));
        Inventory.addItemStack(new ItemStack(content.itemById("redHammer"), n));
        Inventory.addItemStack(new ItemStack(content.itemById("grass"), n));
        Inventory.addItemStack(new ItemStack(content.itemById("workbenchSmall"), n));
        Inventory.addItemStack(new ItemStack(content.itemById("smallStone"), n));
        Inventory.addItemStack(new ItemStack(content.itemById("stoneOven"), n));
    }

    public static void saveWorldImage() {
        if (EventHandler.debugLevel < 2) {
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
                log.error(e.getMessage());
            }
        });
    }

    public static void debugHotKeys() {

        if (EventHandler.debugLevel >= 2) {
            if (input.justPressed(GLFW_KEY_F1)) app.setFramerate(60);
            if (input.justPressed(GLFW_KEY_F2)) app.setFramerate(1000);
            if (input.justPressed(GLFW_KEY_F3)) serializeWorld();
            if (input.justPressed(GLFW_KEY_F4)) deserializeWorld();
            if (input.justClicked(GLFW_MOUSE_BUTTON_RIGHT)) serializeTargetBlock();
        }
    }
}
