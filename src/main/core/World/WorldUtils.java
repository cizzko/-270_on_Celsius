package core.World;

import core.Application;
import core.Global;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator.WorldGenerator;
import core.content.creatures.CreatureType;
import core.entity.CreatureEntity;
import core.math.Point2i;

import java.util.concurrent.ThreadLocalRandom;

import static core.Global.player;
import static core.Global.world;
import static core.World.Textures.TextureDrawing.blockSize;

public class WorldUtils {

    public static int getDistanceToMouse() {
        return (int) Math.abs(
                (player.getX() / TextureDrawing.blockSize - Global.input.mouseBlockPos().x) +
                (player.getY() / TextureDrawing.blockSize - Global.input.mouseBlockPos().y));
    }

    public static int getDistanceBetweenBlocks(Point2i mainPoint, Point2i secondPoint) {
        return Math.abs(mainPoint.x - secondPoint.x) + Math.abs(mainPoint.y - secondPoint.y);
    }

    public static <E extends CreatureEntity> E spawn(CreatureType entity) {
        int bx = ThreadLocalRandom.current().nextInt(0, world.sizeX);
        return spawn0(entity, bx);
    }

    private static <E extends CreatureEntity> E spawn0(CreatureType entity, int bx) {
        float wx = bx * TextureDrawing.blockSize;
        float wy = blockSize * (WorldGenerator.findTopmostSolidBlock(bx, 5) + 1);

        if (HitboxMap.checkIntersInside(wx, wy * blockSize, entity.texture.width(), entity.texture.height()) != null) {
            Application.log.warn("Unable spawning at: ({}, {})", wx, wy * blockSize);
            return spawn0(entity, bx + 1);
        }

        @SuppressWarnings("unchecked")
        var ent = (E) entity.create(wx, wy);
        return ent;
    }
}
