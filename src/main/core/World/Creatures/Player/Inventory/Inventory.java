package core.World.Creatures.Player.Inventory;

import core.EventHandling.EventHandler;
import core.World.Creatures.Player.BuildMenu.BuildMenu;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.Creatures.Player.Player;
import core.Utils.SimpleColor;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.g2d.Atlas;
import core.math.Point2i;
import core.math.Rectangle;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

import static core.Global.*;
import static core.World.Textures.TextureDrawing.*;
import static core.World.WorldUtils.getBlockUnderMousePoint;
import static core.World.WorldUtils.getDistanceToMouse;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class Inventory {
    public static boolean inventoryOpen = false, create = false;
    public static Items[][] inventoryObjects = new Items[8][6];
    public static Point2i currentObject, underMouseItem;
    public static Items.Types currentObjectType;
    private static long lastOpen = System.currentTimeMillis();
    private static final ArrayList<InventoryEvents> listeners = new ArrayList<>();

    public static void registerListener(InventoryEvents event) {
        listeners.add(event);
    }

    public static Items getCurrent() {
        Point2i current = currentObject;
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
        Atlas.Region inventory = atlas.byPath("UI/GUI/inventory/inventory" + (inventoryOpen ? "Open" : "Closed") + ".png");
        batch.draw(inventory, inventoryOpen ? 1488 : 1866, 756);

        for (int x = inventoryOpen ? 0 : 7; x < inventoryObjects.length; x++) {
            for (int y = 0; y < inventoryObjects[x].length; y++) {
                Items item = inventoryObjects[x][y];
                if (item != null) {
                    drawInventoryItem(1498 + x * 54, 766 + y * 54f, item.countInCell, item.texture);
                }
            }
        }
    }

    private static Point2i getObjectUnderMouse() {
        Point2i mousePos = input.mousePos();
        int x = mousePos.x;
        int y = mousePos.y;

        if (x > 1488 && y > 756) {
            x -= 1488;
            y -= 756;
            return new Point2i(x / 54, y / 54);
        }
        return null;
    }

    public static void drawInventoryItem(float x, float y, int countInCell, Atlas.Region region) {
        float zoom = Items.computeZoom(region);

        float oldScale = batch.scale(zoom);
        batch.draw(region, (x + 5) / zoom, (y + 5) / zoom);
        batch.scale(oldScale);

        drawText(x + 31, y - 7, countInCell > 9 ? "9+" : String.valueOf(countInCell), SimpleColor.DIRTY_BRIGHT_BLACK);
    }

    public static void drawInventoryItem(float x, float y, Atlas.Region region) {
        float scale = Items.computeZoom(region);

        float oldScale = batch.scale(scale);
        batch.draw(region, (x + 5) / scale, (y + 5) / scale);
        batch.scale(oldScale);
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

        Point2i current = currentObject;
        if (current != null) {

            Point2i mousePos = input.mousePos();
            if (underMouseItem != null) {
                Items focusedItems = inventoryObjects[underMouseItem.x][underMouseItem.y];
                float zoom = focusedItems.zoom;
                float oldScale = batch.scale(zoom);
                batch.draw(focusedItems.texture, (mousePos.x - 15) / zoom, (mousePos.y - 15) / zoom);
                batch.scale(oldScale);
            }
            if ((inventoryOpen || current.x > 6)) {
                batch.draw(atlas.byPath("UI/GUI/inventory/inventoryCurrent.png"), 1488 + current.x * 54, 756 + current.y * 54f);
            }

            //update placeables preview
            short placeable = inventoryObjects[current.x][current.y].placeable;
            int blockX = getBlockUnderMousePoint().x;
            int blockY = getBlockUnderMousePoint().y;

            if (placeable != 0 && underMouseItem == null && !Rectangle.contains(1488, 756, 500, 500, mousePos)) {
                boolean isDeclined = getDistanceToMouse() < 8 && Player.canPlace(placeable, blockX, blockY);
                Player.drawBlock(blockX, blockY, placeable, isDeclined);
            }
        }
    }

    private static void updateUnderMouse() {
        Point2i underMouse = getObjectUnderMouse();

        if (underMouse != null && EventHandler.getRectanglePress(1488, 756, 1919, 1079) && underMouseItem == null) {
            boolean hasUnderMouseItem = inventoryObjects[underMouse.x][underMouse.y] != null;

            if (currentObject != underMouse && hasUnderMouseItem) {
                currentObject = underMouse;
                currentObjectType = inventoryObjects[underMouse.x][underMouse.y].type;

                if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
                    underMouseItem = underMouse;
                }
            } else if (!hasUnderMouseItem) {
                currentObject = null;
                currentObjectType = null;
            }
        }
    }

    private static void moveItems(Point2i from, Point2i to) {
        Items buff = inventoryObjects[from.x][from.y];
        inventoryObjects[from.x][from.y] = inventoryObjects[to.x][to.y];
        inventoryObjects[to.x][to.y] = buff;
    }

    private static void updateDropItem() {
        if (!input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && underMouseItem != null) {
            //hasItemsMouse - inventory cell under the mouse when the mouse button is released, underMouseItem - item selected for movement
            Point2i hasItemsMouse = getObjectUnderMouse();

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
