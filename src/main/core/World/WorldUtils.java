package core.World;

import core.Global;
import core.World.Textures.TextureDrawing;
import core.math.Point2i;

import static core.World.WorldGenerator.WorldGenerator.DynamicObjects;

public class WorldUtils {

    public static int getDistanceToMouse() {
        return (int) Math.abs(
                (DynamicObjects.getFirst().getX() / TextureDrawing.blockSize - Global.input.mouseBlockPos().x) +
                (DynamicObjects.getFirst().getY() / TextureDrawing.blockSize - Global.input.mouseBlockPos().y));
    }

    public static int getDistanceBetweenBlocks(Point2i mainPoint, Point2i secondPoint) {
        return Math.abs(mainPoint.x - secondPoint.x) + Math.abs(mainPoint.y - secondPoint.y);
    }
}
