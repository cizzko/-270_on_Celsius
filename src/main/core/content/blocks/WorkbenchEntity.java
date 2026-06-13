package core.content.blocks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.Global;
import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;
import core.content.entity.BaseBlockEntity;

import java.io.IOException;

public class WorkbenchEntity extends BaseBlockEntity<Workbench> {

    public static final float range = 1.25f;

    public WorkbenchEntity(Workbench workbench) {
        super(workbench);
    }

    @Override
    public void update() {
        double cx = centerX() + block.tileCountX/2d;
        double cy = centerY() + block.tileCountY/2d;
        float effectiveRange = Math.max(block.tileCountX, block.tileCountY) * range;
        if (Global.player.dstSq(cx, cy) <= effectiveRange*effectiveRange) {
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
