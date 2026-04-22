package core.World.StaticWorldObjects.Structures;

import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.InventoryEvents;
import core.World.Creatures.Player.Inventory.Items.ItemGrid;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.StaticWorldObjects.StaticBlocksEvents;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.math.Point2i;
import core.math.Vector2f;

import java.util.HashMap;

import static core.Global.*;
import static core.World.Creatures.Player.Inventory.Inventory.drawInventoryItem;
import static core.World.StaticWorldObjects.StaticWorldObjects.getFileName;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.World.WorldUtils.getBlockUnderMousePoint;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class Chests implements StaticBlocksEvents, InventoryEvents {
    private static HashMap<Point2i, Items[][]> chests = new HashMap<>();
    private static Point2i current, draggedItem;

    @Override
    public void onBlockChanged(int cellX, int cellY, short oldB, short newB) {
        if (newB != 0) {
            if (newB != -1 && StaticWorldObjects.getId(oldB) != StaticWorldObjects.getId(newB) && StaticWorldObjects.getTexture(newB) != null && getFileName(newB).toLowerCase().contains("chest")) {
                chests.put(new Point2i(cellX, cellY), new Items[3][3]);
            }
        } else {
            chests.remove(new Point2i(cellX, cellY));
        }
    }

    @Override
    public void itemDropped(int blockX, int blockY, Items item) {
        Items[][] grid = chests.get(new Point2i(blockX, blockY));

        if (grid != null) {
            Point2i cell = ItemGrid.findItemOrFree(grid, item.id);

            if (grid[cell.x][cell.y] != null) {
                grid[cell.x][cell.y].countInCell++;
            } else {
                grid[cell.x][cell.y] = Inventory.getCurrent();
            }
            Inventory.deleteCurrentItem();
        }
    }

    public static void updateChests() {
        Point2i underMouse = getBlockUnderMousePoint();

        if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && chests.containsKey(underMouse)) {
            current = (current == null) ? underMouse.copy() : null;
        }
        updateGetItem();
    }

    private static void updateGetItem() {
        Items[][] chest = (current != null) ? chests.get(current) : null;

        if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && chest != null) {
            Vector2f worldPos = input.mouseWorldPos();
            int bx = current.x * blockSize;
            int by = current.y * blockSize;

            if (worldPos.x > bx - 61 && worldPos.x < bx + 109 && worldPos.y > by + 56 && worldPos.y < by + 218) {
                Point2i underMouse = getItemUnderMouse();

                if (chest[underMouse.x][underMouse.y] != null) {
                    draggedItem = underMouse;
                }
            }
        }

        if (draggedItem != null) {
            if (!input.clicked(GLFW_MOUSE_BUTTON_LEFT)) {
                Point2i inventoryUMB = Inventory.getObjectUnderMouse();

                if (inventoryUMB != null && Inventory.inventoryObjects[inventoryUMB.x][inventoryUMB.y] == null) {
                    Inventory.inventoryObjects[inventoryUMB.x][inventoryUMB.y] = chest[draggedItem.x][draggedItem.y];
                    chest[draggedItem.x][draggedItem.y] = null;
                }
                draggedItem = null;
            } else {
                Items item = chest[draggedItem.x][draggedItem.y];

                if (item != null) {
                    Point2i mousePos = input.mousePos();
                    float scale = Items.computeZoom(item.texture);

                    batch.pushState(() -> {
                        batch.scale(scale);
                        batch.draw(item.texture, mousePos.x - 15, mousePos.y - 15);
                    });
                }
            }
        }
    }

    private static Point2i getItemUnderMouse() {
        Vector2f worldPos = input.mouseWorldPos();
        return new Point2i((int) ((worldPos.x - (current.x * blockSize - 61)) / 54), (int) ((worldPos.y - (current.y * blockSize + 56)) / 54));
    }

    public static void draw() {
        if (current != null && chests.containsKey(current)) {
            Items[][] currentChest = chests.get(current);
            float xPos = current.x * blockSize - 61;
            float yPos = current.y * blockSize + 56;

            batch.draw(atlas.byPath("UI/GUI/inventory/chestInventory.png"), xPos, yPos);

            Items item;

            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    item = currentChest[x][y];
                    if (item != null) {
                        drawInventoryItem(10 + xPos + x * 54, 10 + yPos + y * 54f, item.countInCell + 1, item.texture);
                    }
                }
            }
        }
    }
}
