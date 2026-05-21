package core.util;

import core.EventHandling.Config;
import core.Global;
import core.Time;
import core.UI.Dialog;
import core.UI.Styles;
import core.UI.TextArea;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.WorldUtils;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.content.blocks.data.TileData;
import core.World.World;
import core.content.items.Item;
import core.g2d.Fill;
import core.g2d.Render;
import core.g2d.StackfulRender;
import core.math.Point2i;
import core.math.Rectangle;

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

import static core.Application.*;
import static core.EventHandling.Config.json;
import static core.Global.*;
import static core.World.Creatures.Physics.swap;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.content.ItemStack.itemStack;
import static core.content.entity.DrawComponent.GAP;
import static core.util.Color.*;
import static org.lwjgl.glfw.GLFW.*;

public class Debug {
    public static final DecimalFormat FLOATS = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.ROOT));

    public static final int debugLevel = Config.getInt("Debug");

    static final Rectangle rect = new Rectangle();
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
            if (world == null) return null;
            return "[Player] x: " + player.x() + ", y: " + player.y();
        });
        setDebugValue(() -> "Camera Pos: " + camera.position);
        setDebugValue(() ->{
            if (world == null) return null;
            return "Velocity: " + player.getVelocity();
        });
        setDebugValue(() -> {
            if (world == null) return null;
            return "PlayerHp: " + player.getHp();
        });

        setDebugValue(() -> {
            if (world == null) return null;
            var mouseBlockPos = (input.mouseBlockPos());
            var mouseBlock = world.getBlock(mouseBlockPos.x, mouseBlockPos.y);
            return "MouseBlock: " + mouseBlockPos + " " + (mouseBlock != null ? mouseBlock.id + " (NID: " + Global.content.blocksRegistry.idByType(mouseBlock) + ")" : "<void>");
        });
        setDebugValue(() -> {
            if (world == null) return null;
            var mouseBlockPos = (input.mouseBlockPos());
            return "BlockHp: " + world.getHp(mouseBlockPos.x, mouseBlockPos.y);
        });
        //setDebugValue(() -> "Current time: " + sun.currentTime);
    }

    // public static int leftInt(long field) { return (int)(field >> 32); }
    // public static int rightInt(long field) { return (int)(field); }

    public static void drawDebugBorders() {
        if (debugLevel < 2) {
            return;
        }

        StackfulRender.z(Render.LAYER_DEBUG);

        entityPool.entities().values().forEach(e -> {
            e.getHitboxTo(rect);
            Fill.rectangleBorder(rect.x, rect.y, rect.width, rect.height, red);
            // TextureDrawing.drawText(rect.x, rect.y,
            //         "HasFloor: " + e.hasFloor(), black);
        });

        if (debugLevel >= 3) {
            // entityPool.worldIndex().eachNode(0, 0, camera.width(), camera.height(), e -> {
            //     Fill.rectangleBorder(e.bounds.x, e.bounds.y, e.bounds.width, e.bounds.height, acid);
            //     TextureDrawing.drawText(e.bounds.x, e.bounds.y, "GroupSize: " + e.objects.size());
            // });


            // var r = entityPool.worldIndex().resolution;
            // var hashIndex = entityPool.worldIndex().hash;
            // hashIndex.keySet().forEach(hash -> {
            //     long key = HashCommon.invMix(hash);
            //     float x = leftInt(key) * r;
            //     float y = rightInt(key) * r;
            //
            //     Fill.rectangleBorder(x, y, r, r, acid);
            //     var group = hashIndex.get(hash);
            //     TextureDrawing.drawText(x, y, "GroupSize: " + group.size());
            // });
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

            // {
            //     camera.getBoundsTo(viewport);
            //     int minX = (int) Math.floor(viewport.x / blockSize);
            //     int minY = (int) Math.floor(viewport.y / blockSize);
            //     int maxX = (int) Math.floor((viewport.x + viewport.width) / blockSize);
            //     int maxY = (int) Math.floor((viewport.y + viewport.height) / blockSize);
            //     Fill.rectangleBorder(minX * blockSize + 5, minY * blockSize + 5, maxX * blockSize - 5, maxY * blockSize - 5, acid);
            // }

            { // Блоки которые считаются за пол. Черная обводка
                int minX = (int) Math.floor(player.x() / blockSize);
                int maxX = (int) Math.floor((player.x() + player.creature.texture.width() - GAP) / blockSize);
                int minY = (int) Math.floor((player.y() - GAP) / blockSize);

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

        Inventory.addItemStack(itemStack(content.itemById("blockDeleter"), Item.DEFAULT_MAX_STACK_SIZE));
        Inventory.addItemStack(itemStack(content.itemById("aluminum"), n));
        Inventory.addItemStack(itemStack(content.itemById("chest"), n));
        Inventory.addItemStack(itemStack(content.itemById("stick"), n));
        Inventory.addItemStack(itemStack(content.itemById("redHammer"), n));
        Inventory.addItemStack(itemStack(content.itemById("grass"), n));
        Inventory.addItemStack(itemStack(content.itemById("workbenchSmall"), n));
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

        if (input.justPressed(GLFW_KEY_F1)) app.setFramerate(60);
        if (input.justPressed(GLFW_KEY_F2)) app.setFramerate(1000);
        if (input.justClicked(GLFW_MOUSE_BUTTON_RIGHT)) setStructureUnderMouse();
        // if (input.justPressed(GLFW_KEY_F3)) serializeWorld();
        if (input.justPressed(GLFW_KEY_F4)) deserializeWorld();
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
        if (input.justPressed(GLFW_KEY_F9)) uiScene.toggleDebug();
        if (input.justPressed(GLFW_KEY_F10)) uiScene.debug();
        if (input.justPressed(GLFW_KEY_M)) debugMesh = !debugMesh;
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
