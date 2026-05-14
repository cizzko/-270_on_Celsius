package core.World.Creatures.Player.Inventory;

import core.EventHandling.EventHandler;
import core.EventHandling.Config;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.Item;
import core.World.ItemBlock;
import core.World.WorldGenerator.WorldGenerator;
import core.content.entity.InventoryComponent;
import core.math.Point2i;
import core.math.Rectangle;
import org.jetbrains.annotations.Nullable;

import static core.Global.*;
import static core.Global.player;
import static core.World.Creatures.Player.Inventory.Items.ItemGrid.findItemOrFree;
import static core.World.Textures.TextureDrawing.*;
import static core.World.Textures.TextureDrawing.drawText;
import static core.World.WorldUtils.getDistanceToMouse;
import static core.util.Color.*;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class Inventory {
    public static boolean inventoryOpen = false;
    private static final boolean buildGrid = Config.getBoolean("BuildGrid");

    public static void inputUpdate() {
        updateUnderMouse();
        updateDropItem();

        if (EventHandler.isMouseClickedIn(1875, 1035, 1920, 1080)) {
            inventoryOpen = !inventoryOpen;
        }
    }

    public static void draw() {
        String gridTex = "UI/GUI/inventory/inventory" + (inventoryOpen ? "Open" : "Closed");
        batch.draw(atlas.get(gridTex), inventoryOpen ? 1488 : 1866, 756);

        var items = player.items();
        for (int x = inventoryOpen ? 0 : 7; x < items.size(); x++) {
            var line = items.get(x);
            for (int y = 0; y < line.size(); y++) {
                ItemStack item = line.get(y);
                if (item != null) {
                    drawItemStack(1498 + x * 54, 766 + y * 54f, item);
                }
            }
        }

        if (player.hasItemInHand()) {

            ItemStack focusedItem = player.getDraggingItem();
            if (focusedItem != null) {
                var mousePos = input.mousePos();
                var tex = focusedItem.item().texture;
                float uiScale = focusedItem.item().getUiScale();
                batch.draw(tex, mousePos.x - 15, mousePos.y - 15,
                        tex.width() * uiScale, tex.height() * uiScale);
            }
            var current = player.itemInHandIdx;
            if ((inventoryOpen || current.x > 6)) {
                batch.draw(atlas.get("UI/GUI/inventory/inventoryCurrent"), 1488 + current.x * 54, 756 + current.y * 54f);
            }
        }
    }

    public static @Nullable Point2i getFocusedItemIdx() {
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

    public static void updateStaticBlocksPreview() {
        var itemInHand = player.getItemInHand();
        if (itemInHand != null && itemInHand.item() instanceof ItemBlock b) {
            Point2i blockPos = input.mouseBlockPos();

            if (!player.hasDraggingItem() &&
                        !Rectangle.contains(1488, 756, 500, 500, input.mousePos())) {
                boolean canBuild = getDistanceToMouse() < 8 && world.checkPlaceRules(blockPos.x, blockPos.y, b.block);
                addBlockPreview(blockPos.x, blockPos.y, (short) content.blocksRegistry.idByType(b.block), (byte) b.block.maxHp, canBuild);
                drawBuildGrid(blockPos.x, blockPos.y);
            }
        }
    }

    public static void drawBuildGrid(int blockX, int blockY) {
        if (!buildGrid) {
            return;
        }

        var itemInHand = player.getItemInHand();
        if (itemInHand != null && itemInHand.item() instanceof ItemBlock b) {

            //todo ?????
            batch.matrix(camera.projection);
            if (!player.hasDraggingItem()) {
                batch.draw(atlas.get("World/buildGrid"),
                        rgba8888(230, 230, 230, 150),
                        WorldGenerator.findX(blockX, blockY) - 243f, WorldGenerator.findY(blockX, blockY) - 244f);
            }
        }
    }

    private static void updateUnderMouse() {
        Point2i underMouse = getFocusedItemIdx();

        if (underMouse != null && EventHandler.isMouseClickedIn(1488, 756, 1919, 1079) && !player.hasDraggingItem()) {
            boolean hasUnderMouseItem = player.getItem(underMouse.x, underMouse.y) != null;

            if (!player.itemInHandIdx.equals(underMouse) && hasUnderMouseItem) {
                player.itemInHandIdx.set(underMouse);

                if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
                    player.draggingItemIdx.set(underMouse);
                }
            } else if (!hasUnderMouseItem) {
                player.resetItemInHand();
            }
        }
    }

    private static void swapItems(Point2i from, Point2i to) {
        var tmp = player.getItem(from);
        player.setItem(from, player.getItem(to));
        player.setItem(to, tmp);
    }

    private static void updateDropItem() {
        if (!input.clicked(GLFW_MOUSE_BUTTON_LEFT) && player.hasDraggingItem()) {
            Point2i focusedItemIdx = getFocusedItemIdx();
            if (focusedItemIdx != null) {
                swapItems(focusedItemIdx, player.draggingItemIdx);
                player.itemInHandIdx.set(focusedItemIdx);
            } else {
                Point2i mousePos = input.mouseBlockPos();
                var blockEntity = world.getEntity(mousePos);

                var currentInMouse = player.getDraggingItem();
                if (blockEntity != null && currentInMouse != null) {
                    switch (blockEntity.insertItem(currentInMouse)) {
                        case MOVE -> player.setItem(player.draggingItemIdx, null);
                        case PARTIAL_MOVE -> {
                            if (currentInMouse.isEmpty()) {
                                player.setItem(player.draggingItemIdx, null);
                            }
                        }
                        case FAILED -> {}
                    }
                    player.resetItemInHand();
                }
            }
            player.resetDraggingItem();
        }
    }

    public static boolean addItem(Item item) {
        return player.addItem(new ItemStack(item)) != InventoryComponent.TransitionResult.FAILED;
    }

    public static boolean addItemStack(ItemStack item) {
        return player.addItem(item) != InventoryComponent.TransitionResult.FAILED;
    }
}
