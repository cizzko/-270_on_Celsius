package core.World;

public class WorldGenerator {
    public static WorldObjects[][] GenerateWorld(int SizeX, int SizeY) {
        WorldObjects[][] StaticObjects = new WorldObjects[SizeX + 1][SizeY + 1];
        int rand;

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (y < SizeY / 2) {
                    WorldObjects air = new WorldObjects(false, true, true, false, false, false, false, false, null, ".\\src\\assets\\World\\air.png", x * 16, y * 16);
                    StaticObjects[x][y] = air;
                } else {
                    rand = 1 + (int) (Math.random() * 3);
                    WorldObjects grass = new WorldObjects(false, true, false, false, true, false, false, false, null, ".\\src\\assets\\World\\grass" + rand + ".png", x * 16, y * 16);
                    StaticObjects[x][y] = grass;
                }
            }
        }
        return StaticObjects;
    }

    public static void createObject(boolean destroyed, boolean onCamera, boolean gas, boolean liquid, boolean solid, boolean plasma, boolean player, boolean sleeping, String options, String path, float x, float y){

    }
}