package core.content.blocks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;
import core.content.entity.BaseBlockEntity;

import java.io.IOException;

import static core.Global.player;

public class WorkbenchEntity extends BaseBlockEntity<Workbench> {

    public static final float range = 1.15f;

    public WorkbenchEntity(Workbench workbench) {
        super(workbench);
    }

    @Override
    public void update() {
        float cx = centerX() + (block.tileCountX-1)/2f;
        float cy = centerY() + (block.tileCountY-1)/2f;
        if (Math.abs(player.centerX() - cx) <= block.tileCountX / range && Math.abs(player.centerY() - cy) <= block.tileCountY / range) {
            WorkbenchLogic.nearbyWorkbench.put(block.tier, block);
        }
    }

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeEndObject();
    }

    @Override
    public void deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        assert p.currentToken() == JsonToken.START_OBJECT;
        assert p.nextToken() == JsonToken.END_OBJECT;
    }
}
