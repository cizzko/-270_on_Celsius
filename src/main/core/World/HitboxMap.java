package core.World;

import core.World.Creatures.DynamicWorldObjects;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.Textures.TextureDrawing;
import core.World.Textures.TextureLoader;
import java.awt.Point;
import java.util.ArrayList;
import static core.World.StaticWorldObjects.StaticWorldObjects.getResistance;
import static core.World.StaticWorldObjects.StaticWorldObjects.getType;
import static core.World.WorldGenerator.*;

public class HitboxMap {

    public static boolean checkIntersStaticR(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = y % 48 > 47.4 ? (int) (y / TextureDrawing.blockSize) : (int) (y / TextureDrawing.blockSize - 1);
        int tarXSize = (int) Math.ceil(sizeX / TextureDrawing.blockSize);
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);

        for (int i = 0; i < tarYSize; i++) {
            if (getObject(tarX + tarXSize, tarY + i + 1) == -1 || (getResistance(getObject(tarX + tarXSize, tarY + i + 1)) == 100 && x + sizeX >= (getType(getObject(tarX + tarXSize, tarY + i + 1)) == StaticObjectsConst.Types.SOLID ? findX(tarX + tarXSize, tarY + i + 1) : SizeX * TextureDrawing.blockSize))) {
                return true;
            }
        }
        return false;
    }

    private static Point[] checkIntersStaticRP(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = y % 48 > 47.4 ? (int) (y / TextureDrawing.blockSize) : (int) (y / TextureDrawing.blockSize - 1);
        int tarXSize = (int) Math.ceil(sizeX / TextureDrawing.blockSize);
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);
        ArrayList<Point> inters = new ArrayList<>(tarYSize);

        for (int i = 0; i < tarYSize; i++) {
            if (getObject(tarX + tarXSize, tarY + i + 1) == -1 || (getResistance(getObject(tarX + tarXSize, tarY + i + 1)) == 100 && x + sizeX >= (getType(getObject(tarX + tarXSize, tarY + i + 1)) == StaticObjectsConst.Types.SOLID ? findX(tarX + tarXSize, tarY + i + 1) : SizeX * TextureDrawing.blockSize))) {
                inters.add(new Point(tarX + tarXSize, tarY + i + 1));
            }
        }
        return inters.toArray(new Point[0]);
    }

    public static boolean checkIntersStaticL(float x, float y, int sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = y % 48 > 47.4 ? (int) (y / TextureDrawing.blockSize) : (int) (y / TextureDrawing.blockSize - 1);
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);

        for (int i = 0; i < tarYSize; i++) {
            if (tarX < 0 || tarY < 0 || getObject(tarX, tarY + i + 1) == -1 || (getResistance(getObject(tarX, tarY + i + 1)) == 100 && getType(getObject(tarX, tarY + i + 1)) == StaticObjectsConst.Types.SOLID)) {
                return true;
            }
        }
        return false;
    }

    private static Point[] checkIntersStaticLP(float x, float y, int sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = y % 48 > 47.4 ? (int) (y / TextureDrawing.blockSize) : (int) (y / TextureDrawing.blockSize - 1);
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);
        ArrayList<Point> inters = new ArrayList<>(tarYSize);

        for (int i = 0; i < tarYSize; i++) {
            if (tarX < 0 || tarY < 0 || getObject(tarX, tarY + i + 1) == -1 || getResistance(getObject(tarX, tarY + i + 1)) == 100 && getType(getObject(tarX, tarY + i + 1)) == StaticObjectsConst.Types.SOLID) {
                inters.add(new Point(tarX, tarY + i + 1));
            }
        }
        return inters.toArray(new Point[0]);
    }

    public static boolean checkIntersStaticD(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = (int) Math.floor(y / TextureDrawing.blockSize);
        int tarXSize = (int) Math.ceil(sizeX / TextureDrawing.blockSize);

        if (getObject(tarX + tarXSize, tarY) == -1 || (getResistance(getObject(tarX + tarXSize, tarY)) == 100 && x + sizeX >= (getType(getObject(tarX + tarXSize, tarY)) == StaticObjectsConst.Types.SOLID ? findX(tarX + tarXSize, tarY) : SizeX * TextureDrawing.blockSize))) {
            return true;
        }

        for (int i = 0; i < tarXSize; i++) {
            if (getObject(tarX + i, tarY) == -1 || (getResistance(getObject(tarX + i, tarY)) == 100 && (y - sizeY <= (getType(getObject(tarX + i, tarY)) == StaticObjectsConst.Types.SOLID ? findY(tarX + i, tarY) : 0)))) {
                return true;
            }
        }
        return false;
    }

    private static Point[] checkIntersStaticDP(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = (int) Math.floor(y / TextureDrawing.blockSize);
        int tarXSize = (int) Math.ceil(sizeX / TextureDrawing.blockSize);
        ArrayList<Point> inters = new ArrayList<>(tarXSize);

        for (int i = 0; i < tarXSize; i++) {
            if (getObject(tarX + i, tarY) == -1 || (getResistance(getObject(tarX + i, tarY)) == 100 && (y - sizeY <= (getType(getObject(tarX + i, tarY)) == StaticObjectsConst.Types.SOLID ? findY(tarX + i, tarY) : 0)))) {
                inters.add(new Point(tarX + i, tarY));
            }
        }
        return inters.toArray(new Point[0]);
    }

    public static boolean checkIntersStaticU(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = (int) (y / TextureDrawing.blockSize);
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);
        int tarXSize = (int) Math.ceil(sizeX / TextureDrawing.blockSize);

        if (getObject(tarX + tarXSize, tarY + tarYSize) == -1 || (getResistance(getObject(tarX + tarXSize, tarY + tarYSize)) == 100 && x + sizeX >= (getType(getObject(tarX + tarXSize, tarY + tarYSize)) == StaticObjectsConst.Types.SOLID ? findX(tarX + tarXSize, tarY + tarYSize) : SizeX * TextureDrawing.blockSize))) {
            return true;
        }

        for (int i = 0; i < tarXSize; i++) {
            if (getObject(tarX + i, tarY + tarYSize) == -1 || (getResistance(getObject(tarX + i, tarY + tarYSize)) == 100 && (y + sizeY >= (getType(getObject(tarX + i, tarY + tarYSize)) == StaticObjectsConst.Types.SOLID ? findY(tarX + i, tarY + tarYSize) : SizeY * TextureDrawing.blockSize)))) {
                return true;
            }
        }
        return false;
    }

    private static Point[] checkIntersStaticUP(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = (int) (y / TextureDrawing.blockSize);
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);
        int tarXSize = (int) Math.ceil(sizeX / TextureDrawing.blockSize);
        ArrayList<Point> inters = new ArrayList<>(tarXSize);

        for (int i = 0; i < tarXSize; i++) {
            if (getObject(tarX + i, tarY + tarYSize) == -1 || (getResistance(getObject(tarX + i, tarY + tarYSize)) == 100 && (y + sizeY >= (getType(getObject(tarX + i, tarY + tarYSize)) == StaticObjectsConst.Types.SOLID ? findY(tarX + i, tarY + tarYSize) : SizeY * TextureDrawing.blockSize)))) {
                inters.add(new Point(tarX + i, tarY + tarYSize));
            }
        }
        return inters.toArray(new Point[0]);
    }

    public static Point checkIntersInside(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / TextureDrawing.blockSize);
        int tarY = (int) (y / TextureDrawing.blockSize);
        int tarYSize = (int) Math.ceil(sizeY / TextureDrawing.blockSize);
        int tarXSize = (int) Math.ceil(sizeX / TextureDrawing.blockSize);

        for (int xPos = 0; xPos < tarXSize; xPos++) {
            for (int yPos = 0; yPos < tarYSize; yPos++) {
                if (tarX + tarXSize > SizeX || tarY + tarYSize > SizeY || getObject(tarX + xPos, tarY + yPos) == -1 || getObject(tarX + tarXSize, tarY + tarYSize) == -1) {
                    continue;
                }
                if (getType(getObject(tarX + xPos, tarY + yPos)) == StaticObjectsConst.Types.SOLID) {
                    return new Point(tarX + xPos, tarY + yPos);
                }
                if (getType(getObject(tarX + tarXSize, tarY + tarYSize)) == StaticObjectsConst.Types.SOLID) {
                    return new Point(tarX +tarXSize, tarY + tarYSize);
                }
            }
        }
        return null;
    }

    public static Point[] checkIntersOutside(float x, float y, int sizeX, int sizeY) {
        //TODO: ужс, переписать

        Point[] d = checkIntersStaticDP(x, y, sizeX, sizeY);
        if (d.length > 0) {
            return d;
        }
        Point[] u = checkIntersStaticUP(x, y, sizeX, sizeY);
        if (u.length > 0) {
            return u;
        }
        Point[] r = checkIntersStaticRP(x, y, sizeX, sizeY);
        if (r.length > 0) {
            return r;
        }
        Point[] l = checkIntersStaticLP(x, y, sizeY);
        if (l.length > 0) {
            return l;
        }
        return null;
    }

    public static DynamicWorldObjects checkIntersectionsDynamic(float x, float y, int sizeX, int sizeY) {
        for (DynamicWorldObjects dynamicObject : DynamicObjects) {
            if (dynamicObject != null) {
                if ((x + sizeX > dynamicObject.getX() && x < dynamicObject.getX() + TextureLoader.getSize(dynamicObject.getPath()).width()) || (y + sizeY > dynamicObject.getY() && y < dynamicObject.getY() + TextureLoader.getSize(dynamicObject.getPath()).height())) {
                    return dynamicObject;
                }
            }
        }
        return null;
    }
}
