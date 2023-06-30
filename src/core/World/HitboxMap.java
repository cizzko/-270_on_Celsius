package core.World;

import static core.World.WorldGenerator.*;

public class HitboxMap {
    //private static HashMap<String, Dimension> sizes = new HashMap<>(); he for check intersection with non-static object

    public static boolean checkIntersStaticR(float x, float y, int sizeX, int sizeY) {
        int tarX = (int) (x / 16);
        int tarY = y % 16 > 15.4 ? (int) (y / 16) : (int) (y / 16 - 1);
        int tarXSize = (int) Math.ceil(sizeX / 16f);
        int tarYSize = (int) Math.ceil(sizeY / 16f);

        for (int i = 0; i < tarYSize; i++) {
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
            if (y + sizeY >= (StaticObjects[tarX + i][tarY + tarYSize].solid ? StaticObjects[tarX + i][tarY + tarYSize].y : SizeY * 16)) {
                return true;
            }
        }
        return false;
    }
}
