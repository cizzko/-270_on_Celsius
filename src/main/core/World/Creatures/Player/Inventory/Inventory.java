package core.World.Creatures.Player.Inventory;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.World.Creatures.Player.BuildMenu.BuildMenu;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.Utils.SimpleColor;
import core.World.Item;
import core.World.ItemBlock;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator;
import core.entity.BlockEntity;
import core.g2d.Atlas;
import core.math.Point2i;
import core.math.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

import static core.Global.*;
import static core.World.Textures.TextureDrawing.*;
import static core.World.WorldUtils.getBlockUnderMousePoint;
import static core.World.WorldUtils.getDistanceToMouse;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class Inventory {
    public static boolean inventoryOpen = false, create = false;
    public static ItemStack[][] inventoryObjects = new ItemStack[8][6];
    public static Point2i currentObjectPos, underMouseItem;
    public static Item currentObject;
    private static final ArrayList<InventoryEvents> listeners = new ArrayList<>();

    public static void registerListener(InventoryEvents event) {
        listeners.add(event);
    }

    public static ItemStack getCurrent() {
        Point2i current = currentObjectPos;
        if (currentObjectPos != null) {
            return inventoryObjects[current.x][current.y];
        }

        return null;
    }

    public static void create() {
        BuildMenu.create();
    }

    public static void inputUpdate() {
        updateUnderMouse();
        updateDropItem();

        if (EventHandler.getRectangleClick(1875, 1035, 1920, 1080)) {
            inventoryOpen = !inventoryOpen;
        }
    }

    private static void drawInventory() {
        Atlas.Region inventory = atlas.byPath("UI/GUI/inventory/inventory" + (inventoryOpen ? "Open" : "Closed") + ".png");
        batch.draw(inventory, inventoryOpen ? 1488 : 1866, 756);
        Items item;

        for (int x = inventoryOpen ? 0 : 7; x < inventoryObjects.length; x++) {
            for (int y = 0; y < inventoryObjects[x].length; y++) {
                item = inventoryObjects[x][y];
                if (item != null) {
                    drawInventoryItem(1498 + x * 54, 766 + y * 54f, item);
                }
            }
        }

        Point2i current = currentObject;
        if (current != null) {

            Point2i mousePos = input.mousePos();
            if (underMouseItem != null) {
                Items focusedItems = inventoryObjects[underMouseItem.x][underMouseItem.y];
                float scale = Items.computeZoom(focusedItems.texture);

                batch.pushState(() -> {
                    batch.scale(scale);
                    batch.draw(focusedItems.texture, mousePos.x - 15, mousePos.y - 15);
                });
            }
            if ((inventoryOpen || current.x > 6)) {
                batch.draw(atlas.byPath("UI/GUI/inventory/inventoryCurrent.png"), 1488 + current.x * 54, 756 + current.y * 54f);
            }
        }
    }

    private static Point2i getObjectUnderMouse() {
        Point2i mousePos = input.mousePos();
        int x = mousePos.x;
        int y = mousePos.y;

        // 1488 и 756 - нижний левый угол инвентаря, 54 - размер ячейки
        if (x > 1488 && y > 756) {
            x -= 1488;
            y -= 756;
            return new Point2i(x / 54, y / 54);
        }
        return null;
    }

    public static void drawInventoryItem(float x, float y, ItemStack itemStack) {
        var region = itemStack.getItem().texture;
        float zoom = Items.computeZoom(region);

        batch.scale(zoom);
        batch.draw(region, (x + 5), (y + 5));
        batch.resetScale();

        drawText(x + 31, y - 7, itemStack.getCount() > 9 ? "9+" : String.valueOf(itemStack.getCount()), SimpleColor.DIRTY_BRIGHT_BLACK);
    }

    public static void decrementItem(int x, int y) {
        ItemStack itemStack = inventoryObjects[x][y];
        if (itemStack != null && itemStack.decrement()) {
            inventoryObjects[x][y] = null;
            currentObjectPos = null;
        }
    }

    private static void updateCurrentItem() {
        updateUnderMouse();
        updateDropItem();

        if (EventHandler.getRectangleClick(1875, 1035, 1920, 1080)) {
            inventoryOpen = !inventoryOpen;
        }

        Point2i current = currentObjectPos;
        if (current != null) {

            Point2i mousePos = input.mousePos();
            if (underMouseItem != null) {
                var focusedItems = inventoryObjects[underMouseItem.x][underMouseItem.y];
                float zoom = Items.computeZoom(focusedItems.getItem().texture);

                batch.scale(zoom);
                batch.draw(focusedItems.getItem().texture, (mousePos.x - 15), (mousePos.y - 15));
                batch.resetScale();
            }
            if ((inventoryOpen || current.x > 6)) {
                batch.draw(atlas.byPath("UI/GUI/inventory/inventoryCurrent.png"), 1488 + current.x * 54, 756 + current.y * 54f);
            }
        }
    }

    public static void updateStaticBlocksPreview() {
        Point2i current = currentObjectPos;

        if (current != null && inventoryObjects[current.x][current.y].getItem() instanceof ItemBlock block) {
            int blockX = getBlockUnderMousePoint().x;
            int blockY = getBlockUnderMousePoint().y;

            if (underMouseItem == null && !Rectangle.contains(1488, 756, 500, 500, input.mousePos())) {
                boolean isDeclined = getDistanceToMouse() < 8 && WorldGenerator.canPlace(blockX, blockY, block.block);
                TextureDrawing.addToBlocksQueue(blockX, blockY, block.block, isDeclined);

                if (Config.getFromConfig("BuildGrid").equalsIgnoreCase("true")) {
                    batch.color(SimpleColor.fromRGBA(230, 230, 230, 150));
                    batch.draw(atlas.byPath("World/buildGrid.png"), WorldGenerator.findX(blockX, blockY) - 243f, WorldGenerator.findY(blockX, blockY) - 244f);
                    batch.resetColor();
                }
            }
        }
    }

    private static void updateUnderMouse() {
        Point2i underMouse = getObjectUnderMouse();

        if (underMouse != null && EventHandler.getRectangleClick(1488, 756, 1919, 1079) && underMouseItem == null) {
            boolean hasUnderMouseItem = inventoryObjects[underMouse.x][underMouse.y] != null;

            if (currentObjectPos != underMouse && hasUnderMouseItem) {
                currentObjectPos = underMouse;
                currentObject = inventoryObjects[underMouse.x][underMouse.y].getItem();

                if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
                    underMouseItem = underMouse;
                }
            } else if (!hasUnderMouseItem) {
                currentObjectPos = null;
                currentObject = null;
            }
        }
    }

    private static void moveItems(Point2i from, Point2i to) {
        var buff = inventoryObjects[from.x][from.y];
        inventoryObjects[from.x][from.y] = inventoryObjects[to.x][to.y];
        inventoryObjects[to.x][to.y] = buff;
    }

    private static void updateDropItem() {
        if (!input.clicked(GLFW_MOUSE_BUTTON_LEFT) && underMouseItem != null) {
            // hasItemsMouse - inventory cell under the mouse when the mouse button is released, underMouseItem - item selected for movement or drop
            Point2i hasItemsMouse = getObjectUnderMouse();

            if (hasItemsMouse != null) {
                moveItems(hasItemsMouse, underMouseItem);
                currentObjectPos = hasItemsMouse;
            } else {
                Point2i mousePos = getBlockUnderMousePoint();

                var mouse = input.mouseWorldPos();
                int blockX = TextureDrawing.toBlock(mouse.x);
                int blockY = TextureDrawing.toBlock(mouse.y);
                var block = WorldGenerator.blockAt(blockX, blockY);
                var currentItem = inventoryObjects[underMouseItem.x][underMouseItem.y];
                if (block != null && block.itemInsertion(currentItem)) {
                    if (currentItem.isEmpty()) {
                        decrementItem(underMouseItem.x, underMouseItem.y);
                    }
                }

                for (InventoryEvents listener : listeners) {
                    listener.itemDropped(mousePos.x, mousePos.y, currentItem);
                }
            }
            underMouseItem = null;
        }
    }

    private static Point2i findFreeCell() {
        for (int x = 0; x < inventoryObjects.length; x++) {
            for (int y = 0; y < inventoryObjects[x].length; y++) {
                if (x == 7 && y == 5) {
                    continue;
                }
                if (inventoryObjects[x][y] == null) {
                    return new Point2i(x, y);
                }
            }
        }
        return null;
    }

    public static int findCountID(String id) {
        return Arrays.stream(inventoryObjects)
                .flatMapToInt(row -> Arrays.stream(row)
                        .filter(obj -> obj != null && obj.getItem().id.equals(id))
                        .mapToInt(obj -> 1))
                .sum() + 1;
    }

    public static Point2i findItemByID(String id) {
        for (int x = 0; x < inventoryObjects.length; x++) {
            for (int y = 0; y < inventoryObjects[x].length; y++) {
                if (inventoryObjects[x][y] != null && inventoryObjects[x][y].getItem().id.equals(id)) {
                    return new Point2i(x, y);
                }
            }
        }
        return findFreeCell();
    }

    public static void createElement(Item item) {
        if (findCountID(item.id) > 1) {
            Point2i cell = findItemByID(item.id);
            inventoryObjects[cell.x][cell.y].increment();
            return;
        }

        Point2i cell = findFreeCell();
        if (cell != null) {
            inventoryObjects[cell.x][cell.y] = new ItemStack(item);
        }
    }

    public static void createElementTool(String name) {
        // int id = name.hashCode();
        //
        // if (findCountID(id) > 1) {
        //     Point2i cell = findItemByID(id);
        //     inventoryObjects[cell.x][cell.y].decrement();
        //     // TODO удаление предмета со слота?
        //     return;
        // }
        //
        // Point2i cell = findFreeCell();
        // if (cell != null) {
        //     inventoryObjects[cell.x][cell.y] = new ItemStack(Items.createItem(name));
        // }
    }

    public static void createElementPlaceable(BlockEntity object) {
        // byte id = StaticWorldObjects.getId(object);
        //
        // if (findCountID(id) > 1) {
        //     Point2i cell = findItemByID(id);
        //     inventoryObjects[cell.x][cell.y].decrement();
        //     return;
        // }
        //
        // Point2i cell = findFreeCell();
        // if (cell != null) {
        //     inventoryObjects[cell.x][cell.y] = object.getItem();
        // }
    }

    public static void createElementDetail(String name) {
        // int id = name.hashCode();
        //
        // if (findCountID(id) > 1) {
        //     Point2i cell = findItemByID(id);
        //     inventoryObjects[cell.x][cell.y].decrement();
        //     return;
        // }
        //
        // Point2i cell = findFreeCell();
        // if (cell != null) {
        //     inventoryObjects[cell.x][cell.y] = new ItemStack(Items.createItem(name));
        // }
    }

    public static void createElementWeapon(String name) {
        // int id = name.hashCode();
        //
        // if (findCountID(id) > 1) {
        //     Point2i cell = findItemByID(id);
        //     inventoryObjects[cell.x][cell.y].decrement();
        //     return;
        // }
        //
        // Point2i cell = findFreeCell();
        // if (cell != null) {
        //     inventoryObjects[cell.x][cell.y] = new ItemStack(Items.createItem(name));
        // }
    }
}
