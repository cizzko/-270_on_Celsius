package core.World;

public class WorldGenerator {
    public static  int SizeX, SizeY;
    public static WorldObjects StaticObjects[][];
    public static WorldObjects DynamicObjects[];
    public static void generateStaticObjects(int SizeX, int SizeY) {
        StaticObjects = new WorldObjects[SizeX + 1][SizeY + 1];
        int rand;
        WorldGenerator.SizeX = SizeX;
        WorldGenerator.SizeY = SizeY;


        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (y > SizeY / 2) {
                    WorldObjects air = new WorldObjects(false, true, true, false, false, false, false, false, null, ".\\src\\assets\\World\\air.png", x * 16, y * 16);
                    StaticObjects[x][y] = air;
                } else {
                    rand = 1 + (int) (Math.random() * 3);
                    WorldObjects grass = new WorldObjects(false, true, false, false, true, false, false, false, null, ".\\src\\assets\\World\\grass" + rand + ".png", x * 16, y * 16);
                    StaticObjects[x][y] = grass;
                }
            }
        }
    }

    public static void generateDynamicsObjects() {
        DynamicObjects = new WorldObjects[SizeX * SizeY + 1];
        WorldObjects player = new WorldObjects(false, true, false, false, false, false, true, false, null, "", 0, SizeY * 8 + 16);
        DynamicObjects[0] = player;

    }
}