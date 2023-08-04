package core.World.Creatures.Player.Inventory;

import core.EventHandling.EventHandler;
import core.World.Creatures.Player.Inventory.Placeable.PlaceableItems;
import core.World.Creatures.Player.Inventory.Weapons.Weapons;
import core.World.Creatures.Player.Player;
import core.World.Textures.StaticWorldObjects;
import core.World.WorldGenerator;
import java.awt.*;
import java.util.Arrays;
import static core.Window.defPath;
import static core.World.Textures.TextureDrawing.*;

public class Inventory {
    public static boolean inventoryOpen = false, create = false;
    public static final Items[][] inventoryObjects = new Items[8][6];
    public static Point currentObject;
    public static String currentObjectType = "none";
    private static long lastOpen = System.currentTimeMillis();

    public static void create() {
        create = true;
    }

    public static void update() {
        if (create) {
            drawInventory();
            updateCurrentItem();
        }
    }

    private static void drawInventory() {
        drawTexture(defPath + "\\src\\assets\\World\\inventory\\inventory" + (inventoryOpen ? "Open" : "Closed") + ".png", inventoryOpen ? 1488 : 1866, 756, 1, true);

        for (int x = inventoryOpen ? 0 : 7; x < inventoryObjects.length; x++) {
            for (int y = 0; y < inventoryObjects[x].length; y++) {
                if (inventoryObjects[x][y] != null) {
                    float xCoord = 1498 + x * 54;
                    float yCoord = 766 + y * 54f;

                    float zoom = inventoryObjects[x][y].zoom;
                    drawTexture(inventoryObjects[x][y].path, (xCoord + 5) / zoom, (yCoord + 5) / zoom, zoom, true);
                    drawText((int) xCoord + 31, (int) yCoord - 7, inventoryObjects[x][y].countInCell > 9 ? "9+" : String.valueOf(inventoryObjects[x][y].countInCell), new Color(10, 10, 10, 255));

                    if (EventHandler.getRectanglePress((int) xCoord, (int) yCoord, (int) (xCoord + 46), (int) (yCoord + 46))) {
                        currentObjectType = inventoryObjects[x][y] == null ? "none" : String.valueOf(inventoryObjects[x][y].type).toLowerCase();
                        currentObject = new Point(x, y);
                    }
                }
            }
        }
    }

    private static void updateCurrentItem() {
        if (EventHandler.getRectanglePress(1875, 1035, 1920, 1080) && System.currentTimeMillis() - lastOpen > 150) {
            inventoryOpen = !inventoryOpen;
            lastOpen = System.currentTimeMillis();
        }
        Point current = currentObject;

        if (current != null) {
            if ((inventoryOpen || current.x > 6)) {
                drawTexture(defPath + "\\src\\assets\\World\\inventory\\inventoryCurrent.png", 1488 + current.x * 54, 756 + current.y * 54f, 1, true);
            }
            if (currentObjectType != null && currentObjectType.equals("placeable")) {
                int blockX = Player.getBlockUnderMousePoint().x;
                int blockY = Player.getBlockUnderMousePoint().y;
                boolean isDeclined = Player.getDistanceUMB() > 8 || (!StaticObjects[blockX][blockY].gas || !(WorldGenerator.StaticObjects[blockX][blockY + 1].solid || WorldGenerator.StaticObjects[blockX][blockY - 1].solid || WorldGenerator.StaticObjects[blockX + 1][blockY].solid || WorldGenerator.StaticObjects[blockX - 1][blockY].solid));
                Color color = new Color(isDeclined ? 255 : 100, 100, !isDeclined ? 255 : 100, 255);

                if (inventoryObjects[current.x][current.y] != null) {
                    drawTexture(inventoryObjects[current.x][current.y].path, blockX * 16, blockY * 16, 3, color, false, false);
                }
            }
        }
    }

    public static void createElementTool(Tools tool, int id, String path) {
        if (findCountID(id) > 1) {
            Point cell = findItemByID(id);

            inventoryObjects[cell.x][cell.y].countInCell++;
            return;
        }

        Point cell = findFreeCell();
        if (cell != null) {
            inventoryObjects[cell.x][cell.y] = new Items(tool, id, path);
        }
    }

    public static void createElementPlaceable(StaticWorldObjects object) {
        int id = object.id;

        if (findCountID(id) > 1) {
            Point cell = findItemByID(id);
            inventoryObjects[cell.x][cell.y].countInCell++;
            return;
        }

        Point cell = findFreeCell();
        if (cell != null) {
            inventoryObjects[cell.x][cell.y] = new Items(new PlaceableItems(object), id, object.path);
        }
    }

    public static void createElementWeapon(Weapons weapon, int id, String path) {
        if (findCountID(id) > 1) {
            Point cell = findItemByID(id);
            inventoryObjects[cell.x][cell.y].countInCell++;
            return;
        }

        Point cell = findFreeCell();
        if (cell != null) {
            inventoryObjects[cell.x][cell.y] = new Items(weapon, id, path);
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
        return Arrays.stream(inventoryObjects).flatMapToInt(row -> Arrays.stream(row).filter(obj -> obj != null && obj.id == id).mapToInt(obj -> 1)).sum() + 1;
    }

    public static Point findItemByID(int id) {
        for (int x = 0; x < inventoryObjects.length; x++) {
            for (int y = 0; y < inventoryObjects[x].length; y++) {

                if (inventoryObjects[x][y] != null && inventoryObjects[x][y].id == id) {
                    return new Point(x, y);
                }
            }
        }
        return findFreeCell();
    }
}
