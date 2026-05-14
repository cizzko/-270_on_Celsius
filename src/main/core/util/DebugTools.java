package core.util;

import core.Application;
import core.EventHandling.Config;
import core.Global;
import core.World.StaticWorldObjects.Structures.Structures;
import core.World.Textures.ShadowMap;
import core.World.World;
import core.math.Point2i;

import java.io.StringWriter;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static core.EventHandling.Config.json;
import static core.Global.assets;
import static core.Global.world;
import static core.Window.glfwWindow;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class DebugTools {
    public static final DecimalFormat FLOATS = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.ROOT));

    public static boolean selectionBlocksCopy = false, selectionBlocksDelete = false, mousePressed = false;
    private static Point2i lastMousePosBlocks = new Point2i(0, 0), lastMousePos = new Point2i(0, 0);

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
                Application.log.info("РАЗНЫЕ !!!");
            }

        } catch (Exception e) {
            Application.log.error(e);
            e.printStackTrace();
        }
        Application.log.info("Time took: {}ms", (System.currentTimeMillis() - t));
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
                    Application.log.error(e);
                }
                System.out.println(str);

                Application.log.info("Time took: {}ms", (System.currentTimeMillis() - t));
            }
        }
    }

    public static void serializeWorld() {
        long t = System.currentTimeMillis();
        try {
            Files.writeString(assets.workingDir().resolve("open worl.json"), Config.json.writeValueAsString(world));
        } catch (Exception e) {
            Application.log.error(e);
        }
        Application.log.info("Time took: {}ms", (System.currentTimeMillis() - t));
    }

    public static void startUpdate() {

        new Thread(() -> {
            while (!glfwWindowShouldClose(glfwWindow)) {
                if (selectionBlocksCopy || selectionBlocksDelete) {
                    if (Global.input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
                        Point2i mousePos = Global.input.mousePos();

                        if (!mousePressed) {
                            mousePressed = true;
                            lastMousePosBlocks = Global.input.mouseBlockPos().copy();
                            lastMousePos.set(mousePos.x, mousePos.y);
                        }
                    }
                    if (mousePressed && !Global.input.clicked(GLFW_MOUSE_BUTTON_LEFT)) {
                        mousePressed = false;

                        if (selectionBlocksCopy) {
                            copy();
                        } else if (selectionBlocksDelete) {
                            delete();
                        }
                    }
                }
            }
        }).start();
    }

    private static void copy() {
        int startX = lastMousePosBlocks.x;
        int startY = lastMousePosBlocks.y;
        int targetX = Global.input.mouseBlockPos().x;
        int targetY = Global.input.mouseBlockPos().y;

        short[][] objects = new short[targetX - startX][targetY - startY];

        for (int x = startX; x < targetX; x++) {
            for (int y = startY; y < targetY; y++) {
                if (x < Global.world.sizeX && y < Global.world.sizeY && x > 0 && y > 0 && world.getBlockId(x, y) > 0) {
                    ShadowMap.setShadow(x, y, Color.fromRgba8888(0, 0, 255, 255));
                    objects[x - startX][y - startY] = (short) world.getBlockId(x, y);
                }
            }
        }
        Structures.createStructure(String.valueOf(System.currentTimeMillis()), objects);
    }

    private static void delete() {
        Point2i block = Global.input.mouseBlockPos();

        for (int x = lastMousePosBlocks.x; x < block.x; x++) {
            for (int y = lastMousePosBlocks.y; y < block.y; y++) {
                world.set(x, y, null, false);
            }
        }
    }
}
