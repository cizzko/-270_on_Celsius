package core.World;

public class WorldGenerator {
    public static WorldObjects[][] Generate(int SizeY, int SizeX) {
        WorldObjects[][] StaticObjects = new WorldObjects[SizeX + 1][SizeY + 1];

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (y < SizeX / 2) {
                    WorldObjects grass = new WorldObjects(false, true, false, false, true, false, false, false, false, null, "D:\\-270_on_Celsius\\src\\assets\\World\\grass1.png", x * 16, y * 32);
                    StaticObjects[x][y] = grass;
                } else {
                    WorldObjects air = new WorldObjects(false, true, true, false, false, false, false, false, false, null, "D:\\-270_on_Celsius\\src\\assets\\World\\air.png", x * 16, y * 32);
                    StaticObjects[x][y] = air;
                }
            }
        }
        return StaticObjects;
    }
}