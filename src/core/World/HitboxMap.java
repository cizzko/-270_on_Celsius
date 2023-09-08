package core.World;

import core.World.Textures.DynamicWorldObjects;
import core.World.Textures.StaticWorldObjects.StaticObjectsConst;
import core.World.Textures.StaticWorldObjects.StaticWorldObjects;
import core.World.Textures.TextureLoader;
import static core.World.WorldGenerator.*;

public class HitboxMap {

    public static boolean checkIntersStaticR(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / 16);
        int tarY = y % 16 > 15.4 ? (int) (y / 16) : (int) (y / 16 - 1);
        int tarXSize = (int) Math.ceil(sizeX / 16f);
        int tarYSize = (int) Math.ceil(sizeY / 16f);

        for (int i = 0; i < tarYSize; i++) {
            if (getObject(tarX + tarXSize, tarY + i + 1) == null) {
                return true;
            }
            if (getObject(tarX + tarXSize, tarY + i + 1).getResistance() == 100 && x + sizeX >= (getObject(tarX + tarXSize, tarY + i + 1).getType() == StaticObjectsConst.Types.SOLID ? getObject(tarX + tarXSize, tarY + i + 1).x : SizeX * 16)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkIntersStaticL(float x, float y, int sizeY) {
        int tarX = (int) (x / 16);
        int tarY = y % 16 > 15.4 ? (int) (y / 16) : (int) (y / 16 - 1);
        int tarYSize = (int) Math.ceil(sizeY / 16f);

        for (int i = 0; i < tarYSize; i++) {
            if (tarX < 0 || tarY < 0 || getObject(tarX, tarY + i + 1) == null) {
                return true;
            }
            if (getObject(tarX, tarY + i + 1).getResistance() == 100 && getObject(tarX, tarY + i + 1).getType() == StaticObjectsConst.Types.SOLID) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkIntersStaticD(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / 16);
        int tarY = (int) Math.floor(y / 16);
        int tarXSize = (int) Math.ceil(sizeX / 16f);

        for (int i = 0; i < tarXSize; i++) {
            if (getObject(tarX + i, tarY) == null || getObject(tarX + tarXSize, tarY) == null) {
                return true;
            }
            if (getObject(tarX + i, tarY).getResistance() == 100 && (y - sizeY <= (getObject(tarX + i, tarY).getType() == StaticObjectsConst.Types.SOLID ? getObject(tarX + i, tarY).y : 0)) || x + sizeX >= (getObject(tarX + tarXSize, tarY).getType() == StaticObjectsConst.Types.SOLID ? getObject(tarX + tarXSize, tarY).x : SizeX * 16)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkIntersStaticU(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / 16);
        int tarY = (int) (y / 16);
        int tarYSize = (int) Math.ceil(sizeY / 16f);
        int tarXSize = (int) Math.ceil(sizeX / 16f);

        for (int i = 0; i < tarXSize; i++) {
            if (getObject(tarX + i, tarY + tarYSize) == null || getObject(tarX + tarXSize, tarY + tarYSize) == null) {
                return true;
            }
            if (getObject(tarX + i, tarY + tarYSize).getResistance() == 100 && (y + sizeY >= (getObject(tarX + i, tarY + tarYSize).getType() == StaticObjectsConst.Types.SOLID ?getObject(tarX + i, tarY + tarYSize).y : SizeY * 16)) || x + sizeX >= (getObject(tarX + tarXSize, tarY + tarYSize).getType() == StaticObjectsConst.Types.SOLID ? getObject(tarX + tarXSize, tarY + tarYSize).x : SizeX * 16)) {
                return true;
            }
        }
        return false;
    }

    public static StaticWorldObjects checkIntersInside(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / 16);
        int tarY = (int) (y / 16);
        int tarYSize = (int) Math.ceil(sizeY / 16f);
        int tarXSize = (int) Math.ceil(sizeX / 16f);

        for (int xPos = 0; xPos < tarXSize; xPos++) {
            for (int yPos = 0; yPos < tarYSize; yPos++) {
                if (tarX + tarXSize > SizeX || tarY + tarYSize > SizeY || getObject(tarX + xPos, tarY + yPos) == null || getObject(tarX + tarXSize, tarY + tarYSize) == null) {
                    continue;
                }
                if (getObject(tarX + xPos, tarY + yPos).getType() == StaticObjectsConst.Types.SOLID) {
                    return getObject(tarX + xPos, tarY + yPos);
                }
                if (getObject(tarX + tarXSize, tarY + tarYSize).getType() == StaticObjectsConst.Types.SOLID) {
                    return getObject(tarX +tarXSize, tarY + tarYSize);
                }
            }
        }
        return null;
    }

    public static DynamicWorldObjects checkIntersectionsDynamic(float x, float y, int sizeX, int sizeY) {
        for (DynamicWorldObjects dynamicObject : DynamicObjects) {
            if (dynamicObject != null) {
                if ((x + sizeX > dynamicObject.x && x < dynamicObject.x + TextureLoader.getSize(dynamicObject.path).width) || (y + sizeY > dynamicObject.y && y < dynamicObject.y + TextureLoader.getSize(dynamicObject.path).height)) {
                    return dynamicObject;
                }
            }
        }
        return null;
    }
}
