package core.content.blocks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.Time;
import core.World.Creatures.Player.Inventory.Inventory;
import core.content.ItemGrid;
import core.content.ItemStack;
import core.World.Textures.TextureDrawing;
import core.content.entity.BaseBlockEntity;
import core.content.entity.BlockItemStorage;
import core.g2d.Fill;
import core.g2d.RenderList;
import core.util.ArrayUtils;

import java.io.IOException;

import static core.Global.atlas;
import static core.Global.player;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.util.Color.*;

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
    public void deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        assert p.currentToken() == JsonToken.START_OBJECT;

        JsonToken t;
        while ((t = p.nextToken()) != null) {
            if (t == JsonToken.END_OBJECT)
                break;
            if (t == JsonToken.FIELD_NAME) {
                switch (p.currentName()) {
                    case "progress" -> {
                        p.nextToken();
                        progress = p.getFloatValue();
                    }
                    case "input" -> {
                        p.nextToken();
                        input = ctxt.readValue(p, BlockItemStorage.class);
                    }
                    case "fuel" -> {
                        p.nextToken();
                        fuel = ctxt.readValue(p, BlockItemStorage.class);
                    }
                    case "outputStored" -> {
                        p.nextToken();
                        outputStored = ctxt.readValue(p, ItemStack[].class);
                    }
                }
            }
        }

        assert p.currentToken() == JsonToken.END_OBJECT;
    }

    @Override
    public final boolean drawStateChanged() { return false; }

    @Override
    public void draw(RenderList rlist) {
        if (isSelected) {
            float addedX = block.texture.width();
            float addedY = block.texture.height();
            float x = this.x + addedX - (blockSize / 2f);
            float y = this.y + addedY - (blockSize / 2f);

            int playerSize = Math.max(player.creature.texture.width(), player.creature.texture.height());
            if (!input.isEmpty()) {
                int width1 = input.items.length * 54 + playerSize;

                Fill.rect(x, y, width1, 64, rgba8888(0, 0, 0, 170));
                TextureDrawing.drawObjects(x, y, input.items, atlas.get("UI/GUI/buildMenu/factoryIn"));
            }
            if (ArrayUtils.findFreeCell(outputStored) != 0) {
                x += (ArrayUtils.findFreeCell(outputStored) != 0 ? 78 : 0);
                int width = ArrayUtils.findDistinctObjects(outputStored) * 54 + playerSize;

                Fill.rect(x, y, width, 64, rgba8888(0, 0, 0, 170));
                TextureDrawing.drawObjects(x, y, outputStored, atlas.get("UI/GUI/buildMenu/factoryOut"));
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
