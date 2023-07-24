package core.World.Creatures.Player.Inventory;

import core.EventHandling.EventHandler;
import java.awt.*;
import static core.Window.defPath;
import static core.World.Textures.TextureDrawing.drawTexture;

public class Inventory {
    private static boolean inventoryOpen = false, create = false;
    private static final Object[][] inventoryObjects = new Object[8][6];
    public static Point currentObject;
    public static String currentObjectType = "none";
    private static long lastOpen = System.currentTimeMillis();

    public static void create() {
        create = true;
    }

    public static void update() {
        if (create) {
            if (EventHandler.getRectanglePress(1875, 1035, 1920, 1080) && System.currentTimeMillis() - lastOpen > 150) {
                inventoryOpen = !inventoryOpen;
                lastOpen = System.currentTimeMillis();
            }
            drawTexture(defPath + "\\src\\assets\\World\\inventory\\inventory" + (inventoryOpen ? "Open" : "Closed") + ".png", inventoryOpen ? 1488 : 1866, 756, 1, true);

            for (int x = inventoryOpen ? 0 : 7; x < inventoryObjects.length; x++) {
                for (int y = 0; y < inventoryObjects[x].length; y++) {
                    if (inventoryObjects[x][y] != null) {
                        float xCoord = 1498 + x * 54;
                        float yCoord = 766 + y * 54f;

                        if (inventoryObjects[x][y] instanceof Tools tool) {
                            float zoom = tool.zoom;
                            drawTexture(tool.path, (xCoord + 5) / zoom, (yCoord + 5) / zoom, zoom, true);
                        }
                        if (EventHandler.getRectanglePress((int) xCoord, (int) yCoord, (int) (xCoord + 46), (int) (yCoord + 46))) {
                            currentObjectType = getType(x, y);
                            currentObject = new Point(x, y);
                        }
                    }
                }
            }
            if (currentObject != null && (inventoryOpen || currentObject.x > 6)) {
                drawTexture(defPath + "\\src\\assets\\World\\inventory\\inventoryCurrent.png", 1488 + currentObject.x * 54, 756 + currentObject.y * 54f, 1, true);
            }
        }
    }

    public static void createElementTool(float maxHp, float damage, float secBetweenHits, float maxInteractionRange, String path) {
        Point cells = findFreeCell();

        if (cells != null) {
            inventoryObjects[cells.x][cells.y] = new Tools(maxHp, damage, secBetweenHits, maxInteractionRange, 1, path);
        }
    }

    private static Point findFreeCell() {
        for (int x = 0; x < inventoryObjects.length; x++) {
            for (int y = 0; y < inventoryObjects[x].length; y++) {
                if (x == 7 && y == 5) {
                    continue;
                }
                if (inventoryObjects[x][y] == null) {
                    return new Point(x, y);
                }
            }
        }
        return null;
    }

    public static int findCountID(int id) {
        int count = 0;

        for (int x = 0; x < inventoryObjects.length; x++) {
            for (int y = 0; y < inventoryObjects[x].length; y++) {
                if (inventoryObjects[x][y] != null) {

                    if (inventoryObjects[x][y] instanceof Tools tool && tool.id == id) {
                        count++;
                    }
//                    if (inventoryObjects[x][y] instanceof Weapons weapon && weapon.id == id) {
//                        count++;
//                    }
//                    if (inventoryObjects[x][y] instanceof PlaceableItems placeable && placeable.id == id) {
//                        count++;
//                    }
                }
            }
        }
        return count;
    }

    public static String getType(int cellX, int cellY) {
        if (inventoryObjects[cellX][cellY] instanceof Tools) {
            return "tool";

        } else if (inventoryObjects[cellX][cellY] instanceof Weapons) {
            return "weapon";

        } else if (inventoryObjects[cellX][cellY] instanceof PlaceableItems) {
            return "placeable";

        }
        return "none";
    }
}
