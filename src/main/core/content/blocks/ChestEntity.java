package core.content.blocks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.ItemGrid;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.entity.BaseBlockEntity;
import core.math.Point2i;
import core.math.Vector2f;

import java.io.IOException;

import static core.Global.*;
import static core.World.Textures.TextureDrawing.blockSize;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class ChestEntity extends BaseBlockEntity<Chest> {
    private ItemStack[][] storage;
    private boolean isClicked;
    private Point2i draggedCell;

    private ItemStack[][] getStorage() {
        if (storage == null) {
            storage = new ItemStack[block.width][block.height];
        }
        return storage;
    }

    public ChestEntity(Chest block) {
        super(block);
    }

    private boolean inBounds(int i, int j) {
        return i >= 0 && i < block.width &&
               j >= 0 && j < block.height;
    }

    @Override
    public TransitionResult insertItem(ItemStack item) {
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
                if (inBounds(underMouse.x, underMouse.y) && getStorage()[underMouse.x][underMouse.y] != null) {
                    draggedCell = underMouse;
                }
            }

            if (!player.within(bx, by, 3)) {
                isClicked = false;
            }
        }

        if (draggedCell == null) {
            return;
        }
        if (input.clicked(GLFW_MOUSE_BUTTON_LEFT)) {
            ItemStack item = getStorage()[draggedCell.x][draggedCell.y];
            if (item != null) {
                // TODO: ЗДЕСЬ НИКАКОГО РЕНДЕРА !!!! ДОЛЖЕН БЫТЬ СЛОТ "РУК" У ИГРОКА
                //поч
                // Потому что порядок обновления блоков в мире не определен и кто знает что ещё будет рендерить в этом апдейте
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
    public void serialize(JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        if (storage != null) {
            gen.writeFieldName("storage");
            gen.writeObject(storage);
        }
        gen.writeEndObject();
    }

    @Override
    public void draw() {
        if (!isClicked) {
            return;
        }

        float xPos = x - 61;
        float yPos = y + 56;

        batch.draw(atlas.byPath("UI/GUI/inventory/chestInventory.png"), xPos, yPos);

        var storage = getStorage();
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
                (int) ((worldPos.x - (x - 61)) / 54),
                (int) ((worldPos.y - (y + 56)) / 54));
    }
}
