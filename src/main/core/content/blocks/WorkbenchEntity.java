package core.content.blocks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;
import core.entity.BaseBlockEntity;

import java.io.IOException;

import static core.Global.player;

public class WorkbenchEntity extends BaseBlockEntity<Workbench> {

    public WorkbenchEntity(Workbench workbench) {
        super(workbench);
    }

    @Override
    public void update() {
        if (Math.abs(player.getBlockX() - getBlockX()) < 16) {
            WorkbenchLogic.nearbyWorkbench.put(block.tier, block);
        }
    }

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider provider) throws IOException {

    }
}
