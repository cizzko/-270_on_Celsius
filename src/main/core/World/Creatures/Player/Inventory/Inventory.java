package core.World.Creatures.Player.Inventory;

import core.EventHandling.EventHandler;
import core.Global;
import core.World.Creatures.Player.BuildMenu.BuildMenu;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.Creatures.Player.Player;
import core.Utils.SimpleColor;
import core.World.StaticWorldObjects.StaticWorldObjects;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import static core.Window.assetsDir;
import static core.World.Textures.TextureDrawing.*;
import static core.World.WorldUtils.getBlockUnderMousePoint;
import static core.World.WorldUtils.getDistanceToMouse;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class Inventory {
    public static boolean inventoryOpen = false, create = false;
    public static Items[][] inventoryObjects = new Items[8][6];
    public static Point currentObject, underMouseItem;
    public static Items.Types currentObjectType;
    private static long lastOpen = System.currentTimeMillis();
    private static final ArrayList<InventoryEvents> listeners = new ArrayList<>();

    public static void registerListener(InventoryEvents event) {
        listeners.add(event);
    }

    public static Items getCurrent() {
        Point current = currentObject;
        if (currentObject != null) {
            return inventoryObjects[current.x][current.y];
        }

        return null;
    }

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
        drawTexture(inventoryOpen ? 1488 : 1866, 756, true, assetsDir("UI/GUI/inventory/inventory" + (inventoryOpen ? "Open" : "Closed") + ".png"));

        for (int x = inventoryOpen ? 0 : 7; x < inventoryObjects.length; x++) {
            for (int y = 0; y < inventoryObjects[x].length; y++) {
                if (inventoryObjects[x][y] != null) {
                    drawInventoryItem(1498 + x * 54, 766 + y * 54f, inventoryObjects[x][y].countInCell, inventoryObjects[x][y].path);
                }
            }
        }
    }

    private static Point getObjectUnderMouse() {
        Point mousePos = Global.input.mousePos();
        int x = mousePos.x;
        int y = mousePos.y;

        if (x > 1488 && y > 756) {
            x -= 1488;
            y -= 756;
            return new Point(x / 54, y / 54);
        }
        return null;
    }

    public static void drawInventoryItem(float x, float y, int countInCell, String path) {
        float zoom = Items.findZoom(path);

        drawTexture((x + 5) / zoom, (y + 5) / zoom, zoom, true, false, path, null);
        drawText((int) x + 31, (int) y - 7, countInCell > 9 ? "9+" : String.valueOf(countInCell), SimpleColor.DIRTY_BRIGHT_BLACK);
    }

    public static void drawInventoryItem(float x, float y, String path) {
        float zoom = Items.findZoom(path);

        drawTexture((x + 5) / zoom, (y + 5) / zoom, zoom, true, false, path, null);
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
        updateUnderMouse();
        updateDropItem();

        if (EventHandler.getRectanglePress(1875, 1035, 1920, 1080) && System.currentTimeMillis() - lastOpen > 150) {
            inventoryOpen = !inventoryOpen;
            lastOpen = System.currentTimeMillis();
        }

        Point current = currentObject;
        if (current != null) {

            Point mousePos = Global.input.mousePos();
            if (underMouseItem != null) {
                float zoom = inventoryObjects[underMouseItem.x][underMouseItem.y].zoom;
                drawTexture((mousePos.x - 15) / zoom, (mousePos.y - 15) / zoom, zoom, true, false, inventoryObjects[underMouseItem.x][underMouseItem.y].path, SimpleColor.WHITE);
            }
            if ((inventoryOpen || current.x > 6)) {
                drawTexture(1488 + current.x * 54, 756 + current.y * 54f, true, assetsDir("UI/GUI/inventory/inventoryCurrent.png"));
            }

            //update placeables preview
            short placeable = inventoryObjects[current.x][current.y].placeable;
            int blockX = getBlockUnderMousePoint().x;
            int blockY = getBlockUnderMousePoint().y;

            if (placeable != 0 && underMouseItem == null && !new Rectangle(1488, 756, 500, 500).contains(mousePos)) {
                boolean isDeclined = getDistanceToMouse() < 8 && Player.canPlace(placeable, blockX, blockY);
                Player.drawBlock(blockX, blockY, placeable, isDeclined);
            }
        }
    }

    private static void updateUnderMouse() {
        Point underMouse = getObjectUnderMouse();

        if (underMouse != null && EventHandler.getRectanglePress(1488, 756, 1919, 1079) && underMouseItem == null) {
            boolean hasUnderMouseItem = inventoryObjects[underMouse.x][underMouse.y] != null;

            if (currentObject != underMouse && hasUnderMouseItem) {
                currentObject = underMouse;
                currentObjectType = inventoryObjects[underMouse.x][underMouse.y].type;

                if (Global.input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
                    underMouseItem = underMouse;
                }
            } else if (!hasUnderMouseItem) {
                currentObject = null;
                currentObjectType = null;
            }
        }
    }

    private static void moveItems(Point from, Point to) {
        Items buff = inventoryObjects[from.x][from.y];
        inventoryObjects[from.x][from.y] = inventoryObjects[to.x][to.y];
        inventoryObjects[to.x][to.y] = buff;
    }

    private static void updateDropItem() {
        if (!Global.input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && underMouseItem != null) {
            //hasItemsMouse - inventory cell under the mouse when the mouse button is released, underMouseItem - item selected for movement
            Point hasItemsMouse = getObjectUnderMouse();

            if (hasItemsMouse != null) {
                moveItems(hasItemsMouse, underMouseItem);
                currentObject = hasItemsMouse;
            } else {
                for (InventoryEvents listener : listeners) {
                    Point mousePos = getBlockUnderMousePoint();
                    listener.itemDropped(mousePos.x, mousePos.y, inventoryObjects[underMouseItem.x][underMouseItem.y]);
                }
            }
            underMouseItem = null;
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

    public static void createElementTool(String name) {
        int id = name.hashCode();

        if (findCountID(id) > 1) {
            Point cell = findItemByID(id);
            inventoryObjects[cell.x][cell.y].countInCell++;
            return;
        }

        Point cell = findFreeCell();
        if (cell != null) {
            inventoryObjects[cell.x][cell.y] = Items.createTool(name);
        }
    }

    public static void createElementPlaceable(short object) {
        byte id = StaticWorldObjects.getId(object);

        if (findCountID(id) > 1) {
            Point cell = findItemByID(id);
            inventoryObjects[cell.x][cell.y].countInCell++;
            return;
        }

        Point cell = findFreeCell();
        if (cell != null) {
            inventoryObjects[cell.x][cell.y] = Items.createPlaceable(object);
        }
    }

    public static void createElementDetail(String name) {
        int id = name.hashCode();

        if (findCountID(id) > 1) {
            Point cell = findItemByID(id);
            inventoryObjects[cell.x][cell.y].countInCell++;
            return;
        }

        Point cell = findFreeCell();
        if (cell != null) {
            inventoryObjects[cell.x][cell.y] = Items.createDetail(name);
        }
    }

    public static void createElementWeapon(String name) {
        int id = name.hashCode();

        if (findCountID(id) > 1) {
            Point cell = findItemByID(id);
            inventoryObjects[cell.x][cell.y].countInCell++;
            return;
        }

        Point cell = findFreeCell();
        if (cell != null) {
            inventoryObjects[cell.x][cell.y] = Items.createWeapon(name);
        }
    }
}
