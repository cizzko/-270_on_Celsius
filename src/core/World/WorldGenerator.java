package core.World;

import core.EventHandling.Logging.logger;
import core.UI.GUI.CreateElement;
import core.World.Textures.DynamicWorldObjects;
import core.World.Textures.StaticWorldObjects;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import static core.Window.defPath;

public class WorldGenerator {
    public static int SizeX, SizeY;
    public static StaticWorldObjects[][] StaticObjects;
    public static DynamicWorldObjects[] DynamicObjects;

    public static void generateWorld(int SizeX, int SizeY, boolean simple) {
        logger.log("World generator version: 1.0, written at dev 0.0.0.5" + "\nWorld generator: starting generating world at size: x - " + SizeX + ", y - " + SizeY + "; with arguments 'simple: " + simple + "' ");

        StaticObjects = new StaticWorldObjects[SizeX + 1][SizeY + 1];
        WorldGenerator.SizeX = SizeX;
        WorldGenerator.SizeY = SizeY;

        generateFlatWorld();
        if (!simple) {
            generateMountains();
            smoothWorld();
            fillHollows();
        }
    }

    private static void generateFlatWorld() {
        logger.log("World generator: generating flat world");

        CreateElement.createButton(40, 150, 20, 20, "Generating float world..", false, new Color(0, 0, 0, 0));
        int rand;

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (y > SizeY / 2) {
                    StaticObjects[x][y] = new StaticWorldObjects(null, defPath + "\\src\\assets\\World\\blocks\\air.png", x * 16, y * 16);
                    StaticObjects[x][y].gas = true;
                    StaticObjects[x][y].notForDrawing = true;
                } else {
                    rand = 1 + (int) (Math.random() * 3);
                    StaticObjects[x][y] = new StaticWorldObjects(null, defPath + "\\src\\assets\\World\\blocks\\grass" + rand + ".png", x * 16, y * 16);
                    StaticObjects[x][y].solid = true;
                }
            }
        }
    }

    private static void generateMountains() {
        float randGrass = 1.4f;
        float randAir = 7f;

        for (int i = 0; i != 2; i++) {
            for (int x = 1; x < SizeX - 1; x++) {
                for (int y = SizeY / 3; y < SizeY - 1; y++) {
                    // меньше число - меньше шанс генерации высоких гор
                    randGrass += y / 5500000f;

                    if ((StaticObjects[x + 1][y].solid || StaticObjects[x - 1][y].solid || StaticObjects[x][y + 1].solid || StaticObjects[x][y - 1].solid) && Math.random() * randGrass < 1) {
                        StaticObjects[x][y] = new StaticWorldObjects(null, defPath + "\\src\\assets\\World\\blocks\\grass1.png", x * 16, y * 16);
                        StaticObjects[x][y].solid = true;
                    }
                    if (Math.random() * randAir < 1) {
                        StaticObjects[x][y].gas = true;
                        StaticObjects[x][y].solid = false;
                    }
                }
            }
        }
    }

    private static void fillHollows() {
        boolean[][] visited = new boolean[SizeX][SizeY];

        for (int x = 1; x < SizeX - 1; x++) {
            for (int y = 1; y < SizeY - 1; y++) {
                if (StaticObjects[x][y].gas && !visited[x][y]) {
                    List<int[]> area = new ArrayList<>();
                    boolean isClosed = search(x, y, visited, area);
                    if (isClosed) {
                        fillAreaWithGrass(area);
                    }
                }
            }
        }
    }

    private static void smoothWorld() {
        for (int x = 1; x < SizeX - 1; x++) {
            for (int y = 1; y < SizeY - 1; y++) {
                if (StaticObjects[x][y].gas && (StaticObjects[x + 1][y].solid && StaticObjects[x - 1][y].solid && StaticObjects[x][y - 1].solid) || (StaticObjects[x + 1][y + 1].solid && StaticObjects[x - 1][y - 1].solid) || (StaticObjects[x - 1][y + 1].solid && StaticObjects[x + 1][y - 1].solid) && Math.random() * 3 < 1) {
                    StaticObjects[x][y] = new StaticWorldObjects(null, defPath + "\\src\\assets\\World\\blocks\\grass1.png", x * 16, y * 16);
                    StaticObjects[x][y].solid = true;
                }
            }
        }
    }

    private static boolean search(int x, int y, boolean[][] visited, List<int[]> area) {
        java.util.Queue<int[]> queue = new LinkedList<>();
        queue.offer(new int[]{x, y});
        visited[x][y] = true;

        boolean isClosed = true;
        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int cx = current[0];
            int cy = current[1];
            area.add(current);

            if (cx == 0 || cx == SizeX - 1 || cy == 0 || cy == SizeY - 1) {
                isClosed = false;
            }

            if (cx > 0 && StaticObjects[cx - 1][cy].gas && !visited[cx - 1][cy]) {
                queue.offer(new int[]{cx - 1, cy});
                visited[cx - 1][cy] = true;
            }
            if (cx < SizeX - 1 && StaticObjects[cx + 1][cy].gas && !visited[cx + 1][cy]) {
                queue.offer(new int[]{cx + 1, cy});
                visited[cx + 1][cy] = true;
            }
            if (cy > 0 && StaticObjects[cx][cy - 1].gas && !visited[cx][cy - 1]) {
                queue.offer(new int[]{cx, cy - 1});
                visited[cx][cy - 1] = true;
            }
            if (cy < SizeY - 1 && StaticObjects[cx][cy + 1].gas && !visited[cx][cy + 1]) {
                queue.offer(new int[]{cx, cy + 1});
                visited[cx][cy + 1] = true;
            }
        }

        return isClosed;
    }

    private static void fillAreaWithGrass(List<int[]> area) {
        for (int[] coord : area) {
            int x = coord[0];
            int y = coord[1];
            StaticObjects[x][y] = new StaticWorldObjects(null, defPath + "\\src\\assets\\World\\blocks\\grass1.png", x * 16, y * 16);
            StaticObjects[x][y].solid = true;
        }
    }

    private static void generateCanyons() {
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