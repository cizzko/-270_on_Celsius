package core.content.block;

import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.ItemGrid;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.entity.BaseBlockEntity;
import core.math.Point2i;
import core.math.Vector2f;

import static core.Global.*;
import static core.Global.input;
import static core.World.Textures.TextureDrawing.blockSize;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class ChestEntity extends BaseBlockEntity<Chest> {

    private ItemStack[][] storage;
    private ItemStack[][] getStorage() {
        if (storage == null) {
            storage = new ItemStack[block.width][block.height];
        }
        return storage;
    }

    private boolean isClicked;
    private Point2i draggedCell;

    public ChestEntity(Chest block) {
        super(block);
    }

    @Override
    public boolean insertItem(ItemStack item) {
        return ItemGrid.tryAddTo(getStorage(), item);
    }

    @Override
    public void update() {
        if (isClicked) {
            Vector2f worldPos = input.mouseWorldPos();
            int bx = (int)x * blockSize;
            int by = (int)y * blockSize;

            if (worldPos.x > bx - 61 && worldPos.x < bx + 109 && worldPos.y > by + 56 && worldPos.y < by + 218) {
                Point2i underMouse = getItemUnderMouse();
                if (getStorage()[underMouse.x][underMouse.y] != null) {
                    draggedCell = underMouse;
                }
            }
        }

        if (draggedCell == null) {
            return;
        }
        if (input.clicked(GLFW_MOUSE_BUTTON_LEFT)) {
            ItemStack item = getStorage()[draggedCell.x][draggedCell.y];
            if (item != null) {
                // TODO: ЗДЕСЬ НИКАКОГО РЕНДЕРА !!!! ДОЛЖЕН БЫТЬ СЛОТ "РУК" У ИГРОКА
                batch.pushState(() -> {
                    Point2i mousePos = input.mousePos();
                    batch.scale(item.getItem().getUiScale());
                    batch.draw(item.getItem().texture, mousePos.x - 15, mousePos.y - 15);
                });
            }
        } else {
            Point2i inventoryUMB = Inventory.getObjectUnderMouse();
            if (inventoryUMB != null && Inventory.inventoryObjects[inventoryUMB.x][inventoryUMB.y] == null) {
                ItemGrid.moveTo(getStorage(), Inventory.inventoryObjects, draggedCell, inventoryUMB);
            }
            draggedCell = null;
        }
    }

    @Override
    public void onMouseClick() {
        isClicked = !isClicked;
    }

    @Override
    public void draw() {

        if (!isClicked) {
            return;
        }

        float xPos = x * blockSize - 61;
        float yPos = y * blockSize + 56;

        batch.draw(atlas.byPath("UI/GUI/inventory/chestInventory.png"), xPos, yPos);

        for (int x = 0; x < storage.length; x++) {
            var line = storage[x];
            for (int y = 0; y < line.length; y++) {
                var itemStack = line[y];
                if (itemStack != null) {
                    Inventory.drawItemStack(10 + xPos + x * 54, 10 + yPos + y * 54f, itemStack);
                }
            }
        }
    }

    private Point2i getItemUnderMouse() {
        Vector2f worldPos = input.mouseWorldPos();
        return new Point2i(
                (int) ((worldPos.x - (x * blockSize - 61)) / 54),
                (int) ((worldPos.y - (y * blockSize + 56)) / 54));
    }
}
