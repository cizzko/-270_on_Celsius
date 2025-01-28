package core.Utils;

import core.EventHandling.Logging.Logger;
import core.Global;
import core.World.Textures.ShadowMap;
import core.World.StaticWorldObjects.Structures.Structures;
import core.math.Point2i;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static core.Global.world;
import static core.Window.*;
import static core.World.StaticWorldObjects.StaticWorldObjects.*;
import static core.World.WorldUtils.getBlockUnderMousePoint;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class DebugTools {
    public static final DecimalFormat FLOATS = new DecimalFormat("#.#", new DecimalFormatSymbols(Locale.ROOT));

    public static boolean selectionBlocksCopy = false, selectionBlocksDelete = false, mousePressed = false;
    private static Point2i lastMousePosBlocks = new Point2i(0, 0), lastMousePos = new Point2i(0, 0);

    public static void startUpdate() {
        Logger.log("Debug module has started");

        new Thread(() -> {
            while (!glfwWindowShouldClose(glfwWindow)) {
                if (selectionBlocksCopy || selectionBlocksDelete) {
                    if (Global.input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
                        Point2i mousePos = Global.input.mousePos();

                        if (!mousePressed) {
                            mousePressed = true;
                            lastMousePosBlocks = getBlockUnderMousePoint().copy();
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

    public static void printLastTrace() {
        Logger.printStackTrace(Thread.currentThread().getStackTrace(), "none", "none", "stack trace from debug tools", "System.err");
    }

    private static void copy() {
        int startX = lastMousePosBlocks.x;
        int startY = lastMousePosBlocks.y;
        int targetX = getBlockUnderMousePoint().x;
        int targetY = getBlockUnderMousePoint().y;

        short[][] objects = new short[targetX - startX][targetY - startY];

        for (int x = startX; x < targetX; x++) {
            for (int y = startY; y < targetY; y++) {
                if (x < core.Global.world.sizeX && y < core.Global.world.sizeY && x > 0 && y > 0 && world.get(x, y) > 0) {
                    if (getId(world.get(x, y)) != 0) {
                        ShadowMap.setShadow(x, y, Color.fromRgba8888(0, 0, 255, 255));
                        objects[x - startX][y - startY] = world.get(x, y);
                    }
                }
            }
        }
        Structures.createStructure(String.valueOf(System.currentTimeMillis()), objects);
    }

    private static void delete() {
        for (int x = lastMousePosBlocks.x; x < getBlockUnderMousePoint().x; x++) {
            for (int y = lastMousePosBlocks.y; y < getBlockUnderMousePoint().y; y++) {
                if (x < core.Global.world.sizeX && y < core.Global.world.sizeY && x > 0 && y > 0 && world.get(x, y) > 0) {
                    if (getId(world.get(x, y)) != 0) {
                        world.destroy(x, y);
                    }
                }
            }
        }
    }
}
