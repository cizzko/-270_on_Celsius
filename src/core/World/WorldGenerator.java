package core.World;

import core.EventHandling.Logging.logger;
import core.UI.GUI.CreateElement;
import core.World.Textures.DynamicWorldObjects;
import core.World.Textures.StaticWorldObjects;

import java.awt.*;

import static core.Window.defPath;

public class WorldGenerator {
    public static int SizeX, SizeY;
    public static StaticWorldObjects[][] StaticObjects;
    public static DynamicWorldObjects[] DynamicObjects;

    public static void generateWorld(int SizeX, int SizeY, boolean flat) {
        StaticObjects = new StaticWorldObjects[SizeX + 1][SizeY + 1];
        WorldGenerator.SizeX = SizeX;
        WorldGenerator.SizeY = SizeY;

        generateFlatWorld();
        if (!flat) {
            //generateMountains();
            //generateHollows();
        }
    }

    private static void generateFlatWorld() {
        CreateElement.createButton(40, 150, 20, 20, "Generating float world..", false, new Color(0, 0, 0, 0));

        int rand;

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (y > SizeY / 2) {
                    StaticWorldObjects obj = new StaticWorldObjects(null, defPath + "\\src\\assets\\World\\blocks\\air.png", x * 16, y * 16);
                    obj.gas = true;
                    StaticObjects[x][y] = obj;
                } else {
                    rand = 1 + (int) (Math.random() * 3);
                    StaticWorldObjects obj = new StaticWorldObjects(null, defPath + "\\src\\assets\\World\\blocks\\grass" + rand + ".png", x * 16, y * 16);
                    obj.solid = true;
                    StaticObjects[x][y] = obj;
                }
            }
        }
    }

    private static void generateMountains() {
        for (int x = 0; x < SizeX; x++) {
            int height = (int) (Math.random() * 2);

            for (int i = 0; i < height + 1; i++) {
                StaticWorldObjects obj = new StaticWorldObjects(null, defPath + "\\src\\assets\\World\\blocks\\grass1.png", x * 16, (SizeY / 2f + i) * 16);
                obj.solid = true;
                StaticObjects[x][SizeY / 2 + i] = obj;
            }
        }
    }

    private static void generateHollows() {
        for (int x = 0; x < SizeX; x++) {
            for (int y = 1; y < SizeY; y++) {
                if (StaticObjects[x][y].gas && StaticObjects[x][y - 1].solid && (int) (Math.random() * 50) == 1) {
                    int width = (int)(Math.random() * 20) + 5; // случайная ширина овала
                    int height = (int)(Math.random() * 20) + 15; // случайная высота овала
                    int radiusX = (int)((y * 2 + SizeY) / (SizeY / 2.0) * Math.random() / 4) + y / 2; // случайный радиус по X
                    int radiusY = (int)((y * 2 + SizeY) / (SizeY / 2.0) * Math.random() / 4) + y / 2; // случайный радиус по Y
                    radiusX = Math.min(radiusX, SizeY / 2); // ограничиваем радиусы половиной размера мира
                    radiusY = Math.min(radiusY, SizeY / 2);

                    for (int X = Math.max(0, x - radiusX); X < Math.min(SizeX, x + radiusX); X++) {
                        for (int Y = Math.max(1, y - radiusY); Y < Math.min(SizeY, y + radiusY); Y++) {
                            double distance = Math.hypot((X - x) * width, (Y - y) * height);

                            if (distance <= radiusX * radiusY) { // проверяем, попадает ли блок внутрь овала
                                StaticWorldObjects obj = new StaticWorldObjects(null, defPath + "\\src\\assets\\World\\blocks\\air.png", X * 16, Y * 16);
                                obj.solid = true;
                                StaticObjects[X][Y] = obj;
                            }
                        }
                    }
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