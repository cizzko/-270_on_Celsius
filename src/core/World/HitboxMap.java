package core.World;

import core.World.Textures.DynamicWorldObjects;
import core.World.Textures.StaticWorldObjects;
import core.World.Textures.TextureLoader;

import static core.World.WorldGenerator.*;

public class HitboxMap {

    public static boolean checkIntersStaticR(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / 16);
        int tarY = y % 16 > 15.4 ? (int) (y / 16) : (int) (y / 16 - 1);
        int tarXSize = (int) Math.ceil(sizeX / 16f);
        int tarYSize = (int) Math.ceil(sizeY / 16f);

        for (int i = 0; i < tarYSize; i++) {
            if (StaticObjects[tarX + tarXSize][tarY + i + 1] == null) {
                return true;
            }
            if (x + sizeX >= (StaticObjects[tarX + tarXSize][tarY + i + 1].solid ? StaticObjects[tarX + tarXSize][tarY + i + 1].x : SizeX * 16)) {
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
            if (StaticObjects[tarX][tarY + i + 1] == null) {
                return true;
            }
            if (StaticObjects[tarX][tarY + i + 1].solid) {
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
            if (StaticObjects[tarX + i][tarY] == null || StaticObjects[tarX + tarXSize][tarY] == null) {
                return true;
            }
            if ((y - sizeY <= (StaticObjects[tarX + i][tarY].solid ? StaticObjects[tarX + i][tarY].y : 0)) || x + sizeX >= (StaticObjects[tarX + tarXSize][tarY].solid ? StaticObjects[tarX + tarXSize][tarY].x : SizeX * 16)) {
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
            if (StaticObjects[tarX + i][tarY + tarYSize] == null) {
                return true;
            }
            if ((y + sizeY >= (StaticObjects[tarX + i][tarY + tarYSize].solid ? StaticObjects[tarX + i][tarY + tarYSize].y : SizeY * 16)) || x + sizeX >= (StaticObjects[tarX + tarXSize][tarY + tarYSize].solid ? StaticObjects[tarX + tarXSize][tarY + tarYSize].x : SizeX * 16)) {
                return true;
            }
        }
        return false;
    }

    public static StaticWorldObjects checkIntersectionsInside(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / 16);
        int tarY = (int) (y / 16);
        int tarYSize = (int) Math.ceil(sizeY / 16f);
        int tarXSize = (int) Math.ceil(sizeX / 16f);

        for (int xPos = 0; xPos < tarXSize; xPos++) {
            for (int yPos = 0; yPos < tarYSize; yPos++) {
                if (tarX + tarXSize > StaticObjects.length || tarY + tarYSize > StaticObjects.length || StaticObjects[tarX + xPos][tarY + yPos] == null || StaticObjects[tarX + tarXSize][tarY + tarYSize] == null) {
                    continue;
                }
                if (StaticObjects[tarX + xPos][tarY + yPos].solid) {
                    return StaticObjects[tarX + xPos][tarY + yPos];
                }
                if (StaticObjects[tarX + tarXSize][tarY + tarYSize].solid) {
                    return StaticObjects[tarX +tarXSize][tarY + tarYSize];
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
