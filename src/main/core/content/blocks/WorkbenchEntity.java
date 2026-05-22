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

    public WorkbenchEntity(Workbench workbench) {
        super(workbench);
    }

    @Override
    public void update() {
        if (Math.abs(player.blockX() - blockX()) < 16) {
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

    @Override
    public final boolean drawStateChanged() { return false; }
}
