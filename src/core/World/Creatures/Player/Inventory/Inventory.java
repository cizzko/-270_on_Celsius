package core.World.Creatures.Player.Inventory;

import core.EventHandling.EventHandler;
import core.World.Creatures.Player.BuildMenu.BuildMenu;
import core.World.Creatures.Player.Inventory.Items.Details;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.Creatures.Player.Inventory.Items.Placeable.Factories;
import core.World.Creatures.Player.Inventory.Items.Tools;
import core.World.Creatures.Player.Inventory.Items.Placeable.PlaceableItems;
import core.World.Creatures.Player.Inventory.Items.Weapons.Weapons;
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
    public static Items.Types currentObjectType;
    private static long lastOpen = System.currentTimeMillis();

    public static void create() {
        create = true;
        BuildMenu.create();
    }

    public static void update() {
        if (create) {
            drawInventory();
            updateCurrentItem();
        }
    }

    private static void drawInventory() {
        drawTexture(defPath + "\\src\\assets\\UI\\GUI\\inventory\\inventory" + (inventoryOpen ? "Open" : "Closed") + ".png", inventoryOpen ? 1488 : 1866, 756, 1, true);

        for (int x = inventoryOpen ? 0 : 7; x < inventoryObjects.length; x++) {
            for (int y = 0; y < inventoryObjects[x].length; y++) {
                if (inventoryObjects[x][y] != null) {
                    float xCoord = 1498 + x * 54;
                    float yCoord = 766 + y * 54f;

                    drawInventoryItem(xCoord, yCoord, inventoryObjects[x][y].countInCell, inventoryObjects[x][y].path);

                    if (EventHandler.getRectanglePress((int) xCoord, (int) yCoord, (int) (xCoord + 46), (int) (yCoord + 46))) {
                        currentObjectType = inventoryObjects[x][y] == null ? null : inventoryObjects[x][y].type;
                        currentObject = new Point(x, y);
                    }
                }
            }
        }
    }

    public static void drawInventoryItem(float x, float y, int countInCell, String path) {
        float zoom = Items.findZoom(path);

        drawTexture(path, (x + 5) / zoom, (y + 5) / zoom, zoom, true);
        drawText((int) x + 31, (int) y - 7, countInCell > 9 ? "9+" : String.valueOf(countInCell), new Color(10, 10, 10, 255));
    }

    public static void drawInventoryItem(float x, float y, String path) {
        float zoom = Items.findZoom(path);

        drawTexture(path, (x + 5) / zoom, (y + 5) / zoom, zoom, true);
    }

    public static void decrementItem(int x, int y) {
        if (inventoryObjects[x][y] != null) {
            inventoryObjects[x][y].countInCell--;

            if (inventoryObjects[x][y].countInCell <= 0) {
                inventoryObjects[x][y] = null;
                currentObject = null;
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
                drawTexture(defPath + "\\src\\assets\\UI\\GUI\\inventory\\inventoryCurrent.png", 1488 + current.x * 54, 756 + current.y * 54f, 1, true);
            }
            if (currentObjectType == Items.Types.PLACEABLE_BLOCK || currentObjectType == Items.Types.PLACEABLE_FACTORY) {
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

    public static void createElementTool(Tools tool, String path, String description) {
        int id = path.hashCode();

        if (findCountID(id) > 1) {
            Point cell = findItemByID(id);

            inventoryObjects[cell.x][cell.y].countInCell++;
            return;
        }

        Point cell = findFreeCell();
        if (cell != null) {
            inventoryObjects[cell.x][cell.y] = new Items(tool, path, description);
        }
    }

    public static void createElementPlaceable(StaticWorldObjects object, String description) {
        int id = object.id;

        if (findCountID(id) > 1) {
            Point cell = findItemByID(id);
            inventoryObjects[cell.x][cell.y].countInCell++;
            return;
        }

        Point cell = findFreeCell();
        if (cell != null) {
            inventoryObjects[cell.x][cell.y] = new Items(new PlaceableItems(object), Items.Types.PLACEABLE_BLOCK, description);
        }
    }

    public static void createElementDetail(Details object, String description) {
        int id = object.path.hashCode();

        if (findCountID(id) > 1) {
            Point cell = findItemByID(id);
            inventoryObjects[cell.x][cell.y].countInCell++;
            return;
        }

        Point cell = findFreeCell();
        if (cell != null) {
            inventoryObjects[cell.x][cell.y] = new Items(new Details(object.name, object.path), description);
        }
    }

    public static void createElementFactory(Factories factory, String description) {
        int id = factory.id;

        if (findCountID(id) > 1) {
            Point cell = findItemByID(id);
            inventoryObjects[cell.x][cell.y].countInCell++;
            return;
        }

        Point cell = findFreeCell();
        if (cell != null) {
            inventoryObjects[cell.x][cell.y] = new Items(new PlaceableItems(factory), Items.Types.PLACEABLE_FACTORY, description);
        }
    }

    public static void createElementWeapon(Weapons weapon, String path, String description) {
        int id = path.hashCode();

        if (findCountID(id) > 1) {
            Point cell = findItemByID(id);
            inventoryObjects[cell.x][cell.y].countInCell++;
            return;
        }

        Point cell = findFreeCell();
        if (cell != null) {
            inventoryObjects[cell.x][cell.y] = new Items(weapon, path, description);
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
