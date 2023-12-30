package core.Utils;

//start core.Utils.DebugTools.startUpdate
//modify core.Utils.DebugTools.selectionBlocksCopy true
//modify core.Utils.DebugTools.selectionBlocksDelete true

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Logger;
import core.UI.GUI.CreateElement;
import core.World.Textures.ShadowMap;
import core.World.StaticWorldObjects.Structures.Structures;
import core.World.WorldGenerator;
import java.awt.Point;
import static core.Window.*;
import static core.World.StaticWorldObjects.StaticWorldObjects.*;
import static core.World.WorldGenerator.destroyObject;
import static core.World.WorldGenerator.getObject;
import static core.World.WorldUtils.getBlockUnderMousePoint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class DebugTools {
    public static boolean selectionBlocksCopy = false, selectionBlocksDelete = false, mousePressed = false;
    private static Point lastMousePosBlocks = new Point(0, 0), lastMousePos = new Point(0, 0);

    public static void startUpdate() {
        Logger.log("Debug module has started");

        new Thread(() -> {
            while (!glfwWindowShouldClose(glfwWindow)) {
                if (selectionBlocksCopy || selectionBlocksDelete) {
                    if (EventHandler.getMousePress()) {
                        if (!mousePressed) {
                            mousePressed = true;
                            lastMousePosBlocks = getBlockUnderMousePoint();
                            lastMousePos = EventHandler.getMousePos();
                        }
                        CreateElement.createPanel(lastMousePos.x, lastMousePos.y, EventHandler.getMousePos().x - lastMousePos.x, EventHandler.getMousePos().y - lastMousePos.y, "debugPanel", true, "debugModule");
                    }
                    if (mousePressed && !EventHandler.getMousePress()) {
                        mousePressed = false;
                        CreateElement.panels.get("debugPanel").visible = false;

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
                if (x < WorldGenerator.SizeX && y < WorldGenerator.SizeY && x > 0 && y > 0 && getObject(x, y) > 0 && getId(getObject(x, y)) != 0) {
                    ShadowMap.setShadow(x, y, new SimpleColor(0, 0, 255, 255));
                    objects[x - startX][y - startY] = getObject(x, y);
                }
            }
        }
        Structures.createStructure(String.valueOf(System.currentTimeMillis()), objects);
    }

    private static void delete() {
        for (int x = lastMousePosBlocks.x; x < getBlockUnderMousePoint().x; x++) {
            for (int y = lastMousePosBlocks.y; y < getBlockUnderMousePoint().y; y++) {
                if (x < WorldGenerator.SizeX && y < WorldGenerator.SizeY && x > 0 && y > 0 && getObject(x, y) > 0 && getId(getObject(x, y)) != 0) {
                   destroyObject(x, y);
                }
            }
        }
    }
}
