package core.World;

import core.Global;
import core.World.Textures.TextureDrawing;
import core.math.Point2i;

import java.awt.geom.Point2D;

import static core.World.WorldGenerator.*;
import static core.World.WorldGenerator.DynamicObjects;

public class WorldUtils {
    public static Point2i getBlockUnderMousePoint() {
        int blockX = (int) Math.max(0, Math.min(getWorldMousePoint().x / TextureDrawing.blockSize, SizeX));
        int blockY = (int) Math.max(0, Math.min(getWorldMousePoint().y / TextureDrawing.blockSize, SizeY));

        return new Point2i(blockX, blockY);
    }

    public static Point2D.Float getWorldMousePoint() {
        float blockX = ((Global.input.mousePos().x - 960) + 32) + DynamicObjects.getFirst().getX();
        float blockY = ((Global.input.mousePos().y - 540) + 200) + DynamicObjects.getFirst().getY();

        return new Point2D.Float(blockX, blockY);
    }

    public static int getDistanceToMouse() {
        return (int) Math.abs((DynamicObjects.getFirst().getX() / TextureDrawing.blockSize - getBlockUnderMousePoint().x) + (DynamicObjects.getFirst().getY() / TextureDrawing.blockSize - getBlockUnderMousePoint().y));
    }

    public static int getDistanceBetweenBlocks(Point2i mainPoint, Point2i secondPoint) {
        return Math.abs(mainPoint.x - secondPoint.x) + Math.abs(mainPoint.y - secondPoint.y);
    }
}
