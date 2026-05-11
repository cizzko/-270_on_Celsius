package core.World;

import core.World.Creatures.DynamicWorldObjects;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.Textures.TextureDrawing;
import core.math.Point2i;

import java.util.ArrayList;
import java.util.Optional;

import static core.Global.world;
import static core.World.WorldGenerator.WorldGenerator.*;

public class HitboxMap {

    public static boolean checkIntersStaticR(float x, float y, float sizeX, float sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = y % TextureDrawing.blockSize > TextureDrawing.blockSize - 0.6 ? (int) (y / TextureDrawing.blockSize) : (int) (y / TextureDrawing.blockSize) - 1;
        int tarXSize = (int) Math.floor(sizeX / TextureDrawing.blockSize);
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);

        for (int i = 0; i < tarYSize; i++) {
            var block = world.getBlock(tarX + tarXSize, tarY + i + 1);
            if (block == null ||
                    (block.resistance >= 100 && x + sizeX >= (block.type == StaticObjectsConst.Type.SOLID
                            ? findX(tarX + tarXSize, tarY + i + 1) : world.sizeX * TextureDrawing.blockSize))) {
                return true;
            }
        }
        return false;
    }

    private static Point2i[] checkIntersStaticRP(float x, float y, float sizeX, float sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = y % TextureDrawing.blockSize > TextureDrawing.blockSize - 0.6 ? (int) (y / TextureDrawing.blockSize) : (int) (y / TextureDrawing.blockSize) - 1;
        int tarXSize = (int) Math.ceil(sizeX / TextureDrawing.blockSize);
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);
        ArrayList<Point2i> inters = new ArrayList<>(tarYSize);

        for (int i = 0; i < tarYSize; i++) {
            var block = world.getBlock(tarX + tarXSize, tarY + i + 1);
            if (block == null ||
                    (block.resistance >= 100 && x + sizeX >= (block.type == StaticObjectsConst.Type.SOLID
                            ? findX(tarX + tarXSize, tarY + i + 1) : world.sizeX * TextureDrawing.blockSize))) {
                inters.add(new Point2i(tarX + tarXSize, tarY + i + 1));
            }
        }
        return inters.toArray(new Point2i[0]);
    }

    public static boolean checkIntersStaticL(float x, float y, float sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = y % TextureDrawing.blockSize > TextureDrawing.blockSize - 0.6 ? (int) (y / TextureDrawing.blockSize) : (int) (y / TextureDrawing.blockSize) - 1;
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);

        for (int i = 0; i < tarYSize; i++) {
            var block = world.getBlock(tarX, tarY + i + 1);

            if (tarX < 0 || tarY < 0 || block == null || (block.resistance >= 100 && block.type == StaticObjectsConst.Type.SOLID)) {
                return true;
            }
        }
        return false;
    }

    private static Point2i[] checkIntersStaticLP(float x, float y, float sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = y % TextureDrawing.blockSize > TextureDrawing.blockSize - 0.6 ? (int) (y / TextureDrawing.blockSize) : (int) (y / TextureDrawing.blockSize) - 1;
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);
        ArrayList<Point2i> inters = new ArrayList<>(tarYSize);

        for (int i = 0; i < tarYSize; i++) {
            var block = world.getBlock(tarX, tarY + i + 1);

            if (tarX < 0 || tarY < 0 || block == null || (block.resistance >= 100 && block.type == StaticObjectsConst.Type.SOLID)) {
                inters.add(new Point2i(tarX, tarY + i + 1));
            }
        }
        return inters.toArray(new Point2i[0]);
    }

    public static boolean checkIntersStaticD(float x, float y, float sizeX, float sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = (int) Math.floor(y / TextureDrawing.blockSize);
        int tarXSize = (int) Math.ceil(sizeX / TextureDrawing.blockSize);

        for (int dx = 0; dx <= tarXSize; dx++) {
            var block = world.getBlock(tarX + dx, tarY);

            if (block == null || block.type == StaticObjectsConst.Type.SOLID) {
                return true;
            }
        }
        return false;
    }

    private static Point2i[] checkIntersStaticDP(float x, float y, float sizeX, float sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = (int) Math.floor(y / TextureDrawing.blockSize);
        int tarXSize = (int) Math.ceil(sizeX / TextureDrawing.blockSize);
        ArrayList<Point2i> inters = new ArrayList<>(tarXSize);

        for (int dx = 0; dx <= tarXSize; dx++) {
            var block = world.getBlock(tarX + dx, tarY);

            if (block == null || block.type == StaticObjectsConst.Type.SOLID) {
                inters.add(new Point2i(tarX + dx, tarY));
            }
        }
        return inters.toArray(new Point2i[0]);
    }

    public static boolean checkIntersStaticU(float x, float y, float sizeX, float sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = (int) (y / TextureDrawing.blockSize);
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);
        int tarXSize = (int) Math.ceil(sizeX / TextureDrawing.blockSize);

        for (int dx = 0; dx <= tarXSize; dx++) {
            var block = world.getBlock(tarX + dx, tarY + tarYSize);
            if (block == null ||
                    (block.resistance == 100 && (y + sizeY >= (block.type == StaticObjectsConst.Type.SOLID
                            ? findY(tarX + dx, tarY + tarYSize) : world.sizeY * TextureDrawing.blockSize)))) {
                return true;
            }
        }
        return false;
    }

    private static Point2i[] checkIntersStaticUP(float x, float y, float sizeX, float sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = (int) (y / TextureDrawing.blockSize);
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);
        int tarXSize = (int) Math.ceil(sizeX / TextureDrawing.blockSize);
        ArrayList<Point2i> inters = new ArrayList<>(tarXSize);

        for (int i = 0; i < tarXSize; i++) {
            var block = world.getBlock(tarX + i, tarY + tarYSize);
            if (block == null ||
                    (block.resistance == 100 &&
                            (y + sizeY >= (block.type == StaticObjectsConst.Type.SOLID
                                    ? findY(tarX + i, tarY + tarYSize) : world.sizeY * TextureDrawing.blockSize)))) {
                inters.add(new Point2i(tarX + i, tarY + tarYSize));
            }
        }
        return inters.toArray(new Point2i[0]);
    }

    public static Point2i checkIntersInside(float x, float y, float sizeX, float sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = (int) (y / TextureDrawing.blockSize);
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);
        int tarXSize = (int) Math.ceil(sizeX / TextureDrawing.blockSize);

        for (int xPos = 0; xPos <= tarXSize; xPos++) {
            for (int yPos = 0; yPos <= tarYSize; yPos++) {
                var block = world.getBlock(tarX + xPos, tarY + yPos);
                if (block == null) {
                    continue;
                }
                var sizeBlock = world.getBlock(tarX + tarXSize, tarY + tarYSize);
                if (sizeBlock == null) {
                    continue;
                }
                if (block.type == StaticObjectsConst.Type.SOLID) {
                    return new Point2i(tarX + xPos, tarY + yPos);
                }
            }
        }
        return null;
    }

    public static Point2i[] checkIntersOutside(float x, float y, int sizeX, int sizeY) {
        //не в стайл, но мне пофик, зато красиво
        return Optional.of(checkIntersStaticDP(x, y, sizeX, sizeY))
                .filter(d -> d.length > 0)
                .or(() -> Optional.of(checkIntersStaticUP(x, y, sizeX, sizeY)).filter(u -> u.length > 0))
                .or(() -> Optional.of(checkIntersStaticRP(x, y, sizeX, sizeY)).filter(r -> r.length > 0))
                .or(() -> Optional.of(checkIntersStaticLP(x, y, sizeY)).filter(l -> l.length > 0))
                .orElse(null);
    }

    public static DynamicWorldObjects checkIntersectionsDynamic(float x, float y, int sizeX, int sizeY) {
        for (DynamicWorldObjects dynamicObject : DynamicObjects) {
            if (dynamicObject != null) {
                if ((x + sizeX > dynamicObject.getX() && x < dynamicObject.getX() + dynamicObject.getTexture().width()) ||
                        (y + sizeY > dynamicObject.getY() && y < dynamicObject.getY() + dynamicObject.getTexture().height())) {
                    return dynamicObject;
                }
            }
        }
        return null;
    }
}
