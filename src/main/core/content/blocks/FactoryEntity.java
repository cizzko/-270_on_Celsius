package core.content.blocks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.Time;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.ItemGrid;
import core.World.Creatures.Player.Inventory.Items.ItemStack;
import core.World.Textures.TextureDrawing;
import core.entity.BaseBlockEntity;
import core.entity.BlockItemStorage;
import core.g2d.Fill;
import core.util.ArrayUtils;
import core.util.Color;

import java.io.IOException;

import static core.Global.atlas;
import static core.World.Creatures.Player.Player.playerSize;
import static core.World.Textures.TextureDrawing.blockSize;

public class FactoryEntity extends BaseBlockEntity<Factory> {
    public boolean isSelected;

    public float progress;
    public long lastMouseClickTime;
    public ItemStack[] outputStored;
    public BlockItemStorage input, fuel;

    protected FactoryEntity(Factory block) {
        super(block);
    }

    @Override
    public void init() {
        this.outputStored = ItemStack.createArray(block.output);
        this.input = new BlockItemStorage(block.input, block.maxItemCapacity);
        this.fuel = new BlockItemStorage(block.fuel, block.maxItemCapacity);
    }

    @Override
    public void update() {
        if (isDoubleClick()) {
            for (int i = 0; i < outputStored.length; i++) {
                if (outputStored[i] == null) {
                    continue;
                }
                if (Inventory.addItemStack(outputStored[i])) {
                    outputStored[i] = null;
                    break;
                }
            }
        }

        if (input.hasRequired() && fuel.hasRequired() && block.malfunction != Factory.Malfunction.CRITICAL) {
            progress += Time.MS_PER_TICK * Time.delta;

            while (progress >= block.productionSpeed) {
                produceItem();
                progress -= block.productionSpeed;
            }
        }
    }

    private void produceItem() {
        for (int i = 0; i < block.output.length; i++) {
            for (int j = 0; j < outputStored.length; j++) {
                if (outputStored[j] == null || outputStored[j].isSame(block.output[i])) {
                    fuel.removeFirst();
                    input.removeFirst();
                    ItemGrid.insertCopy(outputStored, j, block.output, i);
                    // Sound.playSound(block.sound, Sound.types.EFFECT, false);
                }
            }
        }
    }

    private boolean isDoubleClick() {
        return System.currentTimeMillis() - lastMouseClickTime <= 300;
    }

    @Override
    public void onMouseClick() {
        isSelected = !isSelected;
        lastMouseClickTime = System.currentTimeMillis();
    }

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();

        gen.writeFieldName("input");
        input.serialize(gen, provider);
        gen.writeFieldName("fuel");
        fuel.serialize(gen, provider);
        gen.writeFieldName("outputStored");
        gen.writeObject(outputStored);

        gen.writeNumberField("progress", progress);
        gen.writeEndObject();
    }

    @Override
    public void draw() {
        if (isSelected) {
            float addedX = block.texture.width();
            float addedY = block.texture.height();
            float x = this.x + addedX - (blockSize / 2f);
            float y = this.y + addedY - (blockSize / 2f);

            Color color = Color.fromRgba8888(0, 0, 0, 170);
            if (!input.isEmpty()) {
                int width1 = input.items.length * 54 + playerSize;

                Fill.rect(x, y, width1, 64, color);
                TextureDrawing.drawObjects(x, y, input.items, atlas.byPath("UI/GUI/buildMenu/factoryIn.png"));
            }
            if (ArrayUtils.findFreeCell(outputStored) != 0) {
                x += (ArrayUtils.findFreeCell(outputStored) != 0 ? 78 : 0);
                int width = ArrayUtils.findDistinctObjects(outputStored) * 54 + playerSize;

                Fill.rect(x, y, width, 64, color);
                TextureDrawing.drawObjects(x, y, outputStored, atlas.byPath("UI/GUI/buildMenu/factoryOut.png"));
            }
        }
    }

    @Override
    public TransitionResult insertItem(ItemStack item) {
        int added = input.add(item);
        if (added <= 0) {
            added = fuel.add(item);
        }

        if (added > 0) {
            item.decrement(added);
            return item.isEmpty() ? TransitionResult.MOVE : TransitionResult.PARTIAL_MOVE;
        }
        return TransitionResult.FAILED;
    }
}
