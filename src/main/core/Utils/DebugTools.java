package core.Utils;

//start core.Utils.DebugTools.startUpdate
//modify core.Utils.DebugTools.selectionBlocksCopy true
//modify core.Utils.DebugTools.selectionBlocksDelete true

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Logger;
import core.UI.GUI.CreateElement;
import core.World.Creatures.Player.Player;
import core.World.Textures.ShadowMap;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.Structures.Structures;
import core.World.WorldGenerator;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.zip.DeflaterOutputStream;
import static core.EventHandling.Logging.Logger.printException;
import static core.Window.defPath;
import static core.Window.glfwWindow;
import static core.World.StaticWorldObjects.StaticWorldObjects.*;
import static core.World.WorldGenerator.destroyObject;
import static core.World.WorldGenerator.getObject;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

//сделан по принципу автономного модуля, который можно удалить без последствий
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
                            lastMousePosBlocks = Player.getBlockUnderMousePoint();
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
        int lowestSolidBlock = -1;
        int startX = lastMousePosBlocks.x;
        int startY = lastMousePosBlocks.y;
        int targetX = Player.getBlockUnderMousePoint().x;
        int targetY = Player.getBlockUnderMousePoint().y;

        short[][] objects = new short[targetX - startX][targetY - startY];

        for (int x = startX; x < targetX; x++) {
            for (int y = startY; y < targetY; y++) {
                if (x < WorldGenerator.SizeX && y < WorldGenerator.SizeY && x > 0 && y > 0 && getObject(x, y) > 0 && getId(getObject(x, y)) != 0) {
                    ShadowMap.setShadow(x, y, new SimpleColor(0, 0, 255, 255));
                    objects[x - startX][y - startY] = getObject(x, y);

                    if (lowestSolidBlock == -1 && y == startY && getType(objects[x - startX][y - startY]) == StaticObjectsConst.Types.SOLID) {
                        lowestSolidBlock = x - startX;
                    }
                }
            }
        }
        saveStructure(new Structures(lowestSolidBlock, objects));
    }

    private static void delete() {
        for (int x = lastMousePosBlocks.x; x < Player.getBlockUnderMousePoint().x; x++) {
            for (int y = lastMousePosBlocks.y; y < Player.getBlockUnderMousePoint().y; y++) {
                if (x < WorldGenerator.SizeX && y < WorldGenerator.SizeY && x > 0 && y > 0 && getObject(x, y) > 0 && getId(getObject(x, y)) != 0) {
                   destroyObject(x, y);
                }
            }
        }
    }

    private static void saveStructure(Structures data) {
        long time = System.currentTimeMillis();

        Logger.log("Start saving structure: " + time);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(data);
            oos.close();
            byte[] bytes = baos.toByteArray();

            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            DeflaterOutputStream dos = new DeflaterOutputStream(compressed);
            dos.write(bytes);
            dos.close();
            byte[] compressedBytes = compressed.toByteArray();

            FileOutputStream fos = new FileOutputStream(defPath + "\\src\\assets\\World\\Saves\\Structures\\structure" + time + ".ser");
            fos.write(compressedBytes);
            fos.close();
        } catch (Exception e) {
            printException("Error when serialization (saving) structure: " + time, e);
        }
        Logger.log("End saving structure: " + time);
    }
}
