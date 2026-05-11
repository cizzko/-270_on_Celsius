package core.World.Creatures.Player.Inventory;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.UI.Styles;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.Item;
import core.World.ItemBlock;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator.WorldGenerator;
import core.g2d.Atlas;
import core.math.Point2i;
import core.math.Rectangle;
import core.util.Color;
import core.util.Sized;

import static core.Global.*;
import static core.World.Creatures.Player.Inventory.Items.ItemGrid.findItemOrFree;
import static core.World.Textures.TextureDrawing.drawText;
import static core.World.WorldUtils.getBlockUnderMousePoint;
import static core.World.WorldUtils.getDistanceToMouse;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class Inventory {
    public static boolean inventoryOpen = false;
    public static final ItemStack[][] inventoryObjects = new ItemStack[8][6];
    public static Point2i currentObject, underMouseItem;
    private static final boolean buildGrid = Config.getFromConfigBool("BuildGrid");

    public static ItemStack getCurrent() {
        Point2i current = currentObject;

        if (currentObject != null) {
            return inventoryObjects[current.x][current.y];
        }
        return null;
    }

    public static void inputUpdate() {
        updateUnderMouse();
        updateDropItem();

        if (EventHandler.getRectangleClick(1875, 1035, 1920, 1080)) {
            inventoryOpen = !inventoryOpen;
        }
    }

    public static void draw() {
        String gridTex = "UI/GUI/inventory/inventory" + (inventoryOpen ? "Open" : "Closed");
        batch.draw(atlas.byPath(gridTex), inventoryOpen ? 1488 : 1866, 756);

        for (int x = inventoryOpen ? 0 : 7; x < inventoryObjects.length; x++) {
            for (int y = 0; y < inventoryObjects[x].length; y++) {
                ItemStack item = inventoryObjects[x][y];
                if (item != null) {
                    drawInventoryItem(1498 + x * 54, 766 + y * 54f, item.getCount(), item.getItem().texture);
                }
            }
        }

        Point2i current = currentObject;
        if (current != null) {

            Point2i mousePos = input.mousePos();
            if (underMouseItem != null) {
                ItemStack focusedItems = inventoryObjects[underMouseItem.x][underMouseItem.y];
                float scale = computeZoom(focusedItems.getItem().texture);

                batch.pushState(() -> {
                    batch.scale(scale);
                    batch.draw(focusedItems.getItem().texture, mousePos.x - 15, mousePos.y - 15);
                });
            }
            if ((inventoryOpen || current.x > 6)) {
                batch.draw(atlas.byPath("UI/GUI/inventory/inventoryCurrent.png"), 1488 + current.x * 54, 756 + current.y * 54f);
            }
        }
    }

    public static Point2i getObjectUnderMouse() {
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

    public static void drawInventoryItem(float x, float y, int countInCell, Atlas.Region region) {
        drawInventoryItem(x, y, region);
        drawText(x + 28, y - 7, countInCell > 9 ? "9+" : String.valueOf(countInCell), Styles.DIRTY_BRIGHT_BLACK);
    }

    public static float computeZoom(Sized size) {
        // 32 - target structure size
        return 32f / Math.max(size.width(), size.height());
    }

    public static void drawInventoryItem(float x, float y, Atlas.Region region) {
        float scale = computeZoom(region);

        batch.pushState(() -> {
            batch.scale(scale);
            batch.draw(region, x + 5, y + 5);
        });
    }

    public static void deleteCurrentItem() {
        Point2i current = currentObject;
        inventoryObjects[current.x][current.y] = null;
        currentObject = null;
    }

    public static void decrementCurrentItem() {
        Point2i current = currentObject;
        decrementItem(current.x, current.y);
    }

    public static void decrementItem(int x, int y) { decrementItem(x, y, 1); }

    public static void decrementItem(int x, int y, int count) {
        if (inventoryObjects[x][y].decrement(count)) {
            inventoryObjects[x][y] = null;
            currentObject = null;
        }
    }

    public static void updateStaticBlocksPreview() {
        Point2i current = currentObject;
        if (current != null && inventoryObjects[current.x][current.y].getItem() instanceof ItemBlock b) {
            Point2i mouseBlockPos = input.mouseBlockPos();
            int blockX = mouseBlockPos.x;
            int blockY = mouseBlockPos.y;

            if (underMouseItem == null && !Rectangle.contains(1488, 756, 500, 500, input.mousePos())) {
                boolean isDeclined = getDistanceToMouse() < 8 && world.checkPlaceRules(blockX, blockY, b.block);
                TextureDrawing.addBlockPreview(blockX, blockY, (short) content.getBlockIdByType(b.block), (byte) b.block.maxHp, isDeclined);
                drawBuildGrid(blockX, blockY);
            }
        }
    }

    public static void drawBuildGrid(int blockX, int blockY) {
        if (!buildGrid) {
            return;
        }

        Point2i current = currentObject;
        if (current != null && inventoryObjects[current.x][current.y].getItem() instanceof ItemBlock) {

            //todo
            batch.matrix(camera.projection);
            if (underMouseItem == null) {
                batch.draw(atlas.byPath("World/buildGrid.png"), Color.rgba8888(230, 230, 230, 150),
                        WorldGenerator.findX(blockX, blockY) - 243f, WorldGenerator.findY(blockX, blockY) - 244f);
            }
        }
    }

    private static void updateUnderMouse() {
        Point2i underMouse = getObjectUnderMouse();

        if (underMouse != null && EventHandler.getRectangleClick(1488, 756, 1919, 1079) && underMouseItem == null) {
            boolean hasUnderMouseItem = inventoryObjects[underMouse.x][underMouse.y] != null;

            if (currentObject != underMouse && hasUnderMouseItem) {
                currentObject = underMouse;

                if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
                    underMouseItem = underMouse;
                }
            } else if (!hasUnderMouseItem) {
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
                currentObject = hasItemsMouse;
            } else {
                Point2i mousePos = getBlockUnderMousePoint();

                var blockEntity = world.getEntity(mousePos.x, mousePos.y);
                if (blockEntity != null) {
                    blockEntity.insertItem(inventoryObjects[underMouseItem.x][underMouseItem.y]);
                }
            }
            underMouseItem = null;
        }
    }

    public static boolean addItem(Item item) {
        Point2i cell = findItemOrFree(inventoryObjects, new Point2i(7, 5), item);

        //нету места -> добавление неудачно
        if (cell == null) {
            return false;
        }

        ItemStack stack;
        if (inventoryObjects[cell.x][cell.y] != null) {
            stack = inventoryObjects[cell.x][cell.y];
            stack.increment();
        } else {
            stack = new ItemStack(item);
            inventoryObjects[cell.x][cell.y] = stack;
        }
        return true;
    }
}
