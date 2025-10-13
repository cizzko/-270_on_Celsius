package core.World.Creatures;

import core.EventHandling.Logging.Logger;
import core.Global;
import core.World.HitboxMap;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator;
import core.entity.CreatureEntity;

import java.io.Serializable;

import static core.World.Textures.TextureDrawing.blockSize;

// dynamic objects, can have any coordinates within the world and be moved at any time
public class DynamicWorldObjects implements Serializable {

    public static CreatureEntity createDynamic(String creatureId, float x) {
        var type = Global.content.creatureById(creatureId);
        int topmostBlock = WorldGenerator.findTopmostSolidBlock(TextureDrawing.toBlock(x), 5) + 1;

        if (HitboxMap.checkIntersInside(x, topmostBlock * blockSize, type.texture.width(), type.texture.height()) != null) {
            Logger.log("Unable spawning player at: x - " + x + ", y - " + topmostBlock * blockSize);
            return createDynamic(creatureId, x + blockSize);
        }
        return type.create(x, TextureDrawing.toWorld(topmostBlock));
    }
}
