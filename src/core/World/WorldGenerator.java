package core.World;

public class WorldGenerator {
    public static WorldObjects[][] Generate(int SizeX, int SizeY) {
        WorldObjects[][] StaticObjects = new WorldObjects[SizeX + 1][SizeY + 1];
        int rand = 1;

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (y < SizeY / 2) {
                    WorldObjects air = new WorldObjects(false, true, true, false, false, false, false, false, false, null, "D:\\-270_on_Celsius\\src\\assets\\World\\air.png", x * 16, y * 16);
                    StaticObjects[x][y] = air;
                } else {
                    rand = 1 + (int) (Math.random() * 3);
                    WorldObjects grass = new WorldObjects(false, true, false, false, true, false, false, false, false, null, "D:\\-270_on_Celsius\\src\\assets\\World\\grass" + rand + ".png", x * 16, y * 16);
                    StaticObjects[x][y] = grass;
                }
            }
        }
        return StaticObjects;
    }
}