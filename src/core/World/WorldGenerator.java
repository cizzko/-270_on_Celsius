package core.World;

import core.Logging.logger;
import core.Window;
import core.World.Textures.DynamicWorldObjects;
import core.World.Textures.StaticWorldObjects;

import static core.Window.defPath;

public class WorldGenerator {
    public static int SizeX, SizeY;
    public static StaticWorldObjects[][] StaticObjects;
    public static DynamicWorldObjects[] DynamicObjects;
    public static void generateWorld(int SizeX, int SizeY) {
        StaticObjects = new StaticWorldObjects[SizeX + 1][SizeY + 1];
        int rand;
        WorldGenerator.SizeX = SizeX;
        WorldGenerator.SizeY = SizeY;

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (y > SizeY / 2) {
                    StaticObjects[x][y] = new StaticWorldObjects(true, true, false, false, false, false, null, defPath + "\\src\\assets\\World\\blocks\\air.png", x * 16, y * 16);;
                } else {
                    rand = 1 + (int) (Math.random() * 3);
                    StaticObjects[x][y] = new StaticWorldObjects(true, false, false, true, false, false, null, defPath + "\\src\\assets\\World\\blocks\\grass" + rand + ".png", x * 16, y * 16);
                }
            }
        }
        for (int x = 0; x < StaticObjects.length - 1; x++) {
            for (int y = 0; y < StaticObjects[x].length - 1; y++) {
                if (y > SizeY / 2 && StaticObjects[x][y - 1].solid && Math.random() * 1 == 1) {
                    rand = 1 + (int) (Math.random() * 3);
                    StaticObjects[x][y] = new StaticWorldObjects(true, false, false, true, false, false, null, defPath + "\\src\\assets\\World\\blocks\\grass" + rand + ".png", x * 16, y * 16);
                }
            }
        }
    }

    public static void generateDynamicsObjects() {
        try {
            DynamicObjects = new DynamicWorldObjects[SizeX * SizeY / 2 + 1];
        } catch (OutOfMemoryError e) {
            logger.log("Failed to allocate memory for DynamicObjects: " + "DynamicObjects size < 2147483647: DynamicObject created as size 4096");
            DynamicObjects = new DynamicWorldObjects[4096];
        }

        DynamicWorldObjects player = new DynamicWorldObjects(1, 0f, defPath + "\\src\\assets\\World\\creatures\\player.png", true, true, 0, SizeY * 8 + 16);
        DynamicObjects[0] = player;
    }
}