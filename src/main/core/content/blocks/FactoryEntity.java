package core.content.blocks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.Time;
import core.World.Creatures.Player.Inventory.Inventory;
import core.graphic.GuiDrawing;
import core.content.ItemGrid;
import core.content.ItemStack;
import core.content.entity.BaseBlockEntity;
import core.content.entity.BlockItemStorage;
import core.g2d.Fill;
import core.math.TmpShapes;
import core.math.Vector2f;
import core.util.ArrayUtils;
import core.graphic.Color;

import java.io.IOException;

import static core.Global.*;
import static core.WorldCoordinates.toWorld;

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
            progress += Time.ONE_MILLISECOND * Time.delta;

            while (progress >= block.productionSpeed) {
                produceItem();
                progress -= block.productionSpeed;
            }
        }
    }

    private void produceItem() {
        for (int i = 0; i < block.output.length; i++) {
            for (int j = 0; j < outputStored.length; j++) {
                if (outputStored[j] != null && !outputStored[j].isSame(block.output[i])) {
                    continue;
                }
                if (ItemGrid.insertCopy(outputStored, j, block.output, i) >= 0) {
                    fuel.removeFirst();
                    input.removeFirst();
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
            if (t == JsonToken.END_OBJECT) {
                break;
            }
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
    public void drawGui() {
        if (!isSelected) {
            return;
        }
        Vector2f pos = TmpShapes.v1
                .set(x + toWorld(block.texture.width()) - .5f, y + toWorld(block.texture.height()) - .5f);
        camera.project(pos);

        int playerSize = Math.max(player.creature.texture.width(), player.creature.texture.height());
        var backpanelColor = Color.rgba8888(40, 40, 40, 170);
        int freeCell = ArrayUtils.findFreeCell(outputStored);
        if (!input.isEmpty() && freeCell != -1) {
            int w = input.items.length * 54 + playerSize;
            Fill.rect(pos.x, pos.y, w, 64, backpanelColor);
        }

        if (!input.isEmpty())
            GuiDrawing.drawObjects(pos.x, pos.y, input.items, atlas.get("UI/GUI/buildMenu/factoryIn"));

        if (outputStored != null && freeCell != 0) {
            pos.x += 78;
            GuiDrawing.drawObjects(pos.x, pos.y, outputStored, atlas.get("UI/GUI/buildMenu/factoryOut"));
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
