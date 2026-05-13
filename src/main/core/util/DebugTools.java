package core.util;

import core.Global;
import core.World.StaticWorldObjects.Structures.Structures;
import core.World.Textures.ShadowMap;
import core.math.Point2i;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

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
