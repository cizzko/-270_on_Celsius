package core.World;

import core.EventHandling.Logging.Json;
import core.UI.GUI.Menu.CreatePlanet;
import core.Window;
import core.World.Creatures.CreaturesGenerate;
import core.World.Creatures.Physics;
import core.World.Textures.DynamicWorldObjects;
import core.World.Textures.ShadowMap;
import core.World.Textures.StaticWorldObjects;
import core.World.Textures.TextureDrawing;
import core.World.Weather.Sun;
import core.World.Textures.StaticWorldObjects.Types;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import static core.EventHandling.Logging.Logger.log;
import static core.UI.GUI.CreateElement.*;
import static core.Window.defPath;

public class WorldGenerator {
    public static int SizeX, SizeY;
    public static StaticWorldObjects[][] StaticObjects;
    public static DynamicWorldObjects[] DynamicObjects = new DynamicWorldObjects[20];

    public static void generateWorld() {
        int SizeX = getSliderPos("worldSize") + 20;
        int SizeY = getSliderPos("worldSize") + 20;
        boolean simple = buttons.get(Json.getName("GenerateSimpleWorld")).isClicked;
        boolean randomSpawn = buttons.get(Json.getName("RandomSpawn")).isClicked;
        boolean creatures = buttons.get(Json.getName("GenerateCreatures")).isClicked;

        log("\nWorld generator: version: 1.0, written at dev 0.0.0.5" + "\nWorld generator: starting generating world at size: x - " + SizeX + ", y - " + SizeY + " (" + SizeX * SizeY + "); with arguments 'simple: " + simple + ", random spawn: " + randomSpawn + "'");

        StaticObjects = new StaticWorldObjects[SizeX + 1][SizeY + 1];
        WorldGenerator.SizeX = SizeX;
        WorldGenerator.SizeY = SizeY;

        generateFlatWorld();
        if (!simple) {
            generateMountains();
            smoothWorld();
            fillHollows();
        }
        ShadowMap.generate();

        WorldGenerator.generateResources();
        WorldGenerator.generateDynamicsObjects(randomSpawn);
        Sun.createSun();

        log("World generator: generating done!\n");
        createText(42, 50, "generatingDone", "Done! Starting world..", new Color(147, 51, 0, 255), "WorldGeneratorState");

        start(creatures);
    }

    private static void generateFlatWorld() {
        log("World generator: generating flat world");
        createText(42, 170, "WorldGeneratorState", "Generating flat world", new Color(210, 210, 210, 255), "WorldGeneratorState");

        int rand;

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (y > SizeY / 1.5f) {
                    StaticObjects[x][y] = new StaticWorldObjects(null, x * 16, y * 16, Types.GAS);
                    StaticObjects[x][y].gas = true;
                } else {
                    rand = 1 + (int) (Math.random() * 3);
                    StaticObjects[x][y] = new StaticWorldObjects(defPath + "\\src\\assets\\World\\blocks\\grass" + rand + ".png", x * 16, y * 16, Types.GRASS);
                    StaticObjects[x][y].solid = true;
                }
            }
        }
    }

    private static void generateMountains() {
        log("World generator: generating mountains");
        createText(42, 140, "generateMountainsText", "Generating mountains", new Color(210, 210, 210, 255), "WorldGeneratorState");

        float randGrass = 2f;           //шанс появления неровности, выше число - ниже шанс
        float randAir = 3.5f;             //шанс появления воздуха вместо блока, выше число - ниже шанс
        float iterations = 3f;          //количество итераций генерации
        float mountainHeight = 24000f;  //шанс появления высоких гор, выше число - выше шанс

        for (int i = 0; i < iterations; i++) {
            for (int x = 1; x < SizeX - 1; x++) {
                for (int y = SizeY / 3; y < SizeY - 1; y++) {
                    randGrass += y / (mountainHeight * SizeY);

                    if ((StaticObjects[x + 1][y].solid || StaticObjects[x - 1][y].solid || StaticObjects[x][y + 1].solid || StaticObjects[x][y - 1].solid) && Math.random() * randGrass < 1) {
                        StaticObjects[x][y] = new StaticWorldObjects(defPath + "\\src\\assets\\World\\blocks\\grass" + (int) (Math.random() * 3 + 1) + ".png", x * 16, y * 16, Types.GRASS);
                        StaticObjects[x][y].solid = true;
                    }
                    if (Math.random() * randAir < 1) {
                        StaticObjects[x][y].destroyObject();
                    }
                }
            }
        }
    }

    private static void fillHollows() {
        log("World generator: filling hollows");
        createText(42, 80, "fillHollowsText", "Filling hollows", new Color(210, 210, 210, 255), "WorldGeneratorState");

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
        log("World generator: smoothing world");
        createText(42, 110, "smoothWorldText", "Smoothing world", new Color(210, 210, 210, 255), "WorldGeneratorState");

        float smoothingChance = 3f; //шанс сглаживания, выше число - меньше шанс

        for (int x = 1; x < SizeX - 1; x++) {
            for (int y = 1; y < SizeY - 1; y++) {
                int random = (int) (Math.random() * 3 + 1);

                if (StaticObjects[x][y].gas && (StaticObjects[x + 1][y].solid && StaticObjects[x - 1][y].solid && StaticObjects[x][y - 1].solid) || (StaticObjects[x + 1][y + 1].solid && StaticObjects[x - 1][y - 1].solid) || (StaticObjects[x - 1][y + 1].solid && StaticObjects[x + 1][y - 1].solid) && Math.random() * smoothingChance < 1) {
                    StaticObjects[x][y] = new StaticWorldObjects(defPath + "\\src\\assets\\World\\blocks\\grass" + random + ".png", x * 16, y * 16, Types.GRASS);
                    StaticObjects[x][y].solid = true;
                } else if ((StaticObjects[x][y + 1].solid && StaticObjects[x][y - 1].solid) || (StaticObjects[x + 1][y].solid && StaticObjects[x - 1][y].solid) && Math.random() * smoothingChance < 1) {
                    StaticObjects[x][y] = new StaticWorldObjects(defPath + "\\src\\assets\\World\\blocks\\grass" + random + ".png", x * 16, y * 16, Types.GRASS);
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
            StaticObjects[x][y] = new StaticWorldObjects(defPath + "\\src\\assets\\World\\blocks\\grass" + (int) (Math.random() * 3 + 1) + ".png", x * 16, y * 16, Types.GRASS);
            StaticObjects[x][y].solid = true;
        }
    }

    private static void generateCanyons() {

    }

    private static void generateResources() {
        log("World generator: generating resources");

        PerlinNoiseGenerator.main(SizeX, SizeY, 1, 15, 1, .8, 4);

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                int random = 1 + (int) (Math.random() * 3);
                if (StaticObjects[x][y + 1] != null && !StaticObjects[x][y + 1].gas) { // Генерация земли под блоками травы
                    StaticObjects[x][y] = new StaticWorldObjects(defPath + "\\src\\assets\\World\\blocks\\dirt" + random + ".png", x * 16, y * 16, Types.DIRT);
                    StaticObjects[x][y].solid = true;
                }

                if (ShadowMap.colorDegree[x][y] >= 3) { // Генерация камня
                    StaticObjects[x][y] = new StaticWorldObjects(defPath + "\\src\\assets\\World\\blocks\\stone" + random + ".png", x * 16, y * 16, Types.STONE);
                    StaticObjects[x][y].solid = true;
                }

                if (PerlinNoiseGenerator.noise[x][y] && ShadowMap.colorDegree[x][y] >= 3) { // Генерация руды WIP
                    StaticObjects[x][y] = new StaticWorldObjects(defPath + "\\src\\assets\\World\\blocks\\iron_ore.png", x * 16, y * 16, Types.IRON_ORE);
                    StaticObjects[x][y].solid = true;
                }

                if (ShadowMap.colorDegree[x][y] == 2) { // Генерация перехода между землёй и камнем
                    if (StaticObjects[x][y + 1] != null && StaticObjects[x][y + 1].type != Types.DIRT_STONE) {
                        StaticObjects[x][y] = new StaticWorldObjects(defPath + "\\src\\assets\\World\\blocks\\dirt-stone" + random + ".png", x * 16, y * 16, Types.DIRT_STONE);
                    } else {
                        StaticObjects[x][y] = new StaticWorldObjects(defPath + "\\src\\assets\\World\\blocks\\flag.png", x * 16, y * 16, Types.DIRT_STONE);
                    }
                    StaticObjects[x][y].solid = true;
                }
            }
        }
    }

    public static void generateDynamicsObjects(boolean randomSpawn) {
        DynamicObjects[0] = new DynamicWorldObjects(1, false, defPath + "\\src\\assets\\World\\creatures\\player.png", 0, randomSpawn ? (int) (Math.random() * (SizeX * 16)) : SizeX * 8f);
    }

    public static void start(boolean generateCreatures) {
        TextureDrawing.loadObjects();
        CreatePlanet.delete();

        new Thread(new Physics()).start();
        if (generateCreatures) {
            new Thread(new CreaturesGenerate()).start();
        }
        Window.start = true;
    }
}