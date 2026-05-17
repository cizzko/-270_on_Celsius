package core.content.blocks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.World.Creatures.Player.Inventory.Inventory;
import core.content.ItemGrid;
import core.content.ItemStack;
import core.World.Textures.TextureDrawing;
import core.content.entity.BaseBlockEntity;
import core.g2d.Atlas;
import core.g2d.Render;
import core.g2d.RenderList;
import core.g2d.StackfulRender;
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

            if (worldPos.x > x - 61 && worldPos.x < x + 109 && worldPos.y > y + 56 && worldPos.y < y + 218) {
                Point2i underMouse = getItemUnderMouse();
                if (inBounds(underMouse.x, underMouse.y) && getStorage()[underMouse.x][underMouse.y] != null) {
                    draggedCell = underMouse;
                }
            }

            if (!player.within(x, y, 3*blockSize)) {
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
                Point2i mousePos = input.mousePos();
                float uiScale = item.item().uiScale();
                Atlas.Region tex = item.item().texture;
                StackfulRender.draw(tex, mousePos.x - 15, mousePos.y - 15, tex.width() * uiScale, tex.height() * uiScale);
            }
        } else {
            Point2i inventoryUMB = Inventory.getFocusedItemIdx();
            if (inventoryUMB != null && player.getItem(inventoryUMB) == null) {
                ItemGrid.moveTo(getStorage(), player.items(), draggedCell, inventoryUMB);
            }
            draggedCell = null;
        }

        if (!isClicked) {
            return;
        }


        StackfulRender.pushState(() -> {
            float xPos = x - 61;
            float yPos = y + 56;

            StackfulRender.z(Render.LAYER_GUI);
            StackfulRender.draw(atlas.get("UI/GUI/inventory/chestInventory"), xPos, yPos);
            var storage = getStorage();
            for (int x = 0; x < storage.length; x++) {
                var line = storage[x];
                for (int y = 0; y < line.length; y++) {
                    var itemStack = line[y];
                    if (itemStack != null) {
                        TextureDrawing.drawItemStack(10 + xPos + x * 54, 10 + yPos + y * 54f, itemStack);
                    }
                }
            }
        });
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
    public void deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        assert p.currentToken() == JsonToken.START_OBJECT;

        JsonToken t;
        while ((t = p.nextToken()) != null) {
            if (t == JsonToken.END_OBJECT)
                break;
            if (t == JsonToken.FIELD_NAME && "storage".equals(p.currentName())) {
                p.nextToken();
                storage = ctxt.readValue(p, ItemStack[][].class);
            }
        }

        assert p.currentToken() == JsonToken.END_OBJECT;
    }

    private Point2i getItemUnderMouse() {
        Vector2f worldPos = input.mouseWorldPos();
        return new Point2i(
                (int) ((worldPos.x - (x - 61)) / 54),
                (int) ((worldPos.y - (y + 56)) / 54));
    }

    @Override
    public final boolean drawStateChanged() { return false; }
}
