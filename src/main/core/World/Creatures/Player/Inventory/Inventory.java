package core.World.Creatures.Player.Inventory;

import core.Hotkeys;
import core.World.WorldUtils;
import core.content.ItemStack;
import core.content.entity.comp.InventoryComponent;
import core.content.items.Item;
import core.content.items.ItemBlock;
import core.g2d.Atlas;
import core.g2d.StackfulRender;
import core.graphic.WorldDrawing;
import core.math.Point2i;
import core.math.Rectangle;
import core.util.Config;
import org.jetbrains.annotations.Nullable;

import static core.Global.*;
import static core.World.WorldUtils.getDistanceToMouse;
import static core.WorldCoordinates.toBlock;
import static core.WorldCoordinates.toWorld;
import static core.content.creatures.ItemEntity.ITEM_DROPPED_SIZE;
import static core.graphic.Color.rgba8888;
import static core.graphic.GuiDrawing.drawItemStack;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class Inventory {
    public static final int ITEM_DROP_DISTANCE = 10;
    public static final int CELL_SIZE = 54;
    public static boolean inventoryOpen = false;
    private static final boolean buildGrid = Config.getBoolean("BuildGrid");

    public static void inputUpdate() {
        updateUnderMouse();
        updateDropItem();

        if (Hotkeys.isMouseClickedIn(input.viewportWidth() - 45, input.viewportHeight() - 45, input.viewportWidth(), input.viewportHeight())) {
            inventoryOpen = !inventoryOpen;
        }
    }

    public static void draw() {
        String gridTex = "UI/GUI/inventory/inventory" + (inventoryOpen ? "Open" : "Closed");
        int viewportWidth = input.viewportWidth();
        int viewportHeight = input.viewportHeight();
        StackfulRender.draw(atlas.get(gridTex), viewportWidth - (inventoryOpen ? 432 : CELL_SIZE), viewportHeight - 324);

        var items = player.items();
        for (int x = inventoryOpen ? 0 : 7; x < items.size(); x++) {
            var line = items.get(x);
            for (int y = 0; y < line.size(); y++) {
                ItemStack item = line.get(y);
                if (item != null) {
                    drawItemStack((viewportWidth - 422) + x * CELL_SIZE, (viewportHeight - 314) + y * 54f, item);
                }
            }
        }

        if (player.hasItemInHand()) {
            ItemStack focusedItem = player.getDraggingItem();
            if (focusedItem != null) {
                var mousePos = input.mousePos();
                var tex = focusedItem.item().texture;
                float uiScale = focusedItem.item().uiScale();
                StackfulRender.draw(tex, mousePos.x - 15, mousePos.y - 15,
                        tex.width() * uiScale, tex.height() * uiScale);
            }
            var current = player.itemInHandIdx;
            if ((inventoryOpen || current.x > 6)) {
                StackfulRender.draw(atlas.get("UI/GUI/inventory/inventoryCurrent"),
                        (viewportWidth - 432) + current.x * CELL_SIZE, (viewportHeight - 324) + current.y * 54f);
            }
        }
    }

    public static @Nullable Point2i getFocusedItemIdx() {
        var mousePos = input.mousePos();
        float x = mousePos.x;
        float y = mousePos.y;

        int viewportWidth = input.viewportWidth();
        int viewportHeight = input.viewportHeight();
        int tl = viewportWidth - 432;
        int bl = viewportHeight - 324;
        if (x > tl && y > bl) {
            x -= tl;
            y -= bl;
            return new Point2i((int) Math.floor(x / CELL_SIZE), (int) Math.floor(y / CELL_SIZE));
        }
        return null;
    }

    public static void updateStaticBlocksPreview() {
        var itemInHand = player.getItemInHand();
        if (itemInHand != null && itemInHand.item() instanceof ItemBlock b) {
            Point2i blockPos = input.mouseBlockPos();

            int viewportWidth = input.viewportWidth();
            int viewportHeight = input.viewportHeight();
            int tl = viewportWidth - 432;
            int bl = viewportHeight - 324;
            if (!player.hasDraggingItem() &&
                        !Rectangle.contains(tl, bl, 500, 500, input.mousePos())) {
                boolean canBuild = getDistanceToMouse() < 8 && world.checkPlaceRules(blockPos.x, blockPos.y, b.block);
                WorldDrawing.addBlockPreview(blockPos.x, blockPos.y, b.block.id, (byte) b.block.maxHp, canBuild);
            }
        }
    }

    public static void drawBuildGrid(int blockX, int blockY) {
        if (!buildGrid || player.hasDraggingItem()) {
            return;
        }
        var itemInHand = player.getItemInHand();
        if (itemInHand != null && itemInHand.item() instanceof ItemBlock) {
            Atlas.Region tex = atlas.get("World/buildGrid");
            float w = toWorld(tex.width());
            float h = toWorld(tex.height());
            StackfulRender.draw(tex,
                    rgba8888(230, 230, 230, 150),
                    blockX - toWorld(243f), blockY - toWorld(244f), w, h);
        }
    }

    private static void updateUnderMouse() {
        Point2i underMouse = getFocusedItemIdx();
        int viewportWidth = input.viewportWidth();
        int viewportHeight = input.viewportHeight();
        int tl = viewportWidth - 432;
        int bl = viewportHeight - 324;
        if (underMouse != null && Hotkeys.isMouseClickedIn(tl, bl, tl + 431, bl + 323) && !player.hasDraggingItem()) {
            boolean hasUnderMouseItem = player.getItem(underMouse.x, underMouse.y) != null;
            if (hasUnderMouseItem) {
                if (player.itemInHandIdx.equals(underMouse)) {
                    player.resetItemInHand();
                } else {
                    player.itemInHandIdx.set(underMouse);
                    if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
                        player.draggingItemIdx.set(underMouse);
                    }
                }
            }
        }
    }

    private static void swapItems(Point2i from, Point2i to) {
        var tmp = player.getItem(from);
        player.setItem(from, player.getItem(to));
        player.setItem(to, tmp);
    }

    private static void updateDropItem() {
        var me = player;
        if (!input.clicked(GLFW_MOUSE_BUTTON_LEFT) && me.hasDraggingItem()) {
            Point2i focusedItemIdx = getFocusedItemIdx();

            if (focusedItemIdx != null) {
                if (!focusedItemIdx.equals(me.draggingItemIdx)) {
                    swapItems(focusedItemIdx, me.draggingItemIdx);
                    me.resetItemInHand();
                }
            } else {
                var currentInMouse = me.getDraggingItem();
                if (currentInMouse == null) {
                    return;
                }

                Point2i mousePos = input.mouseBlockPos();
                var blockEntity = world.getEntity(mousePos);


                if (blockEntity != null) {
                    switch (blockEntity.insertItem(currentInMouse)) {
                        case MOVE -> me.setItem(me.draggingItemIdx, null);
                        case PARTIAL_MOVE -> {
                            if (currentInMouse.isEmpty()) {
                                me.setItem(me.draggingItemIdx, null);
                            }
                        }
                        case FAILED -> {}
                    }
                } else {
                    var worldMousePos = input.mouseWorldPos();

                    float dst = (float) Math.sqrt(me.dstSq(worldMousePos.x, worldMousePos.y));
                    if (dst > ITEM_DROP_DISTANCE) dst = ITEM_DROP_DISTANCE;
                    worldMousePos.sub(me.x(), me.y()).nor().scale(dst);
                    worldMousePos.add(me.x(), me.y());
                    worldMousePos.sub(toWorld(ITEM_DROPPED_SIZE/2f), toWorld(ITEM_DROPPED_SIZE/2f));

                    int blockId = world.getBlockId(toBlock(worldMousePos.x), toBlock(worldMousePos.y));
                    if (blockId == 0) {
                        WorldUtils.spawnItemEntity(currentInMouse, worldMousePos.x, worldMousePos.y);
                        me.setItem(me.draggingItemIdx, null);
                    }
                }
                me.resetItemInHand();
            }
            me.resetDraggingItem();
        }
    }

    public static boolean addItem(Item item) {
        return player.addItem(new ItemStack(item)) != InventoryComponent.TransitionResult.FAILED;
    }

    public static boolean addItemStack(ItemStack item) {
        return player.addItem(item) != InventoryComponent.TransitionResult.FAILED;
    }
}
