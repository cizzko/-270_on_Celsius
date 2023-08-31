package core.World;

import core.EventHandling.Logging.Json;
import core.UI.GUI.Menu.CreatePlanet;
import core.Window;
import core.World.Creatures.CreaturesGenerate;
import core.World.Creatures.Physics;
import core.World.Textures.DynamicWorldObjects;
import core.World.Textures.ShadowMap;
import core.World.Textures.SimpleColor;
import core.World.Textures.StaticWorldObjects.StaticObjectsConst;
import core.World.Textures.StaticWorldObjects.StaticWorldObjects;
import core.World.Weather.Sun;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import static core.EventHandling.Logging.Logger.log;
import static core.UI.GUI.CreateElement.*;
import static core.Window.*;

public class WorldGenerator {
    public static int SizeX, SizeY;
    public static StaticWorldObjects[][] StaticObjects;
    public static ArrayList<DynamicWorldObjects> DynamicObjects = new ArrayList<>();

    public static StaticWorldObjects getObject(int x, int y) {
        return StaticObjects[x][y];
    }

    public static void setObject(int x, int y, StaticWorldObjects object) {
        StaticObjects[x][y] = object;
    }

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

        StaticObjectsConst.setDestroyed();
        generateFlatWorld();
        if (!simple) {
            generateMountains();
            smoothWorld();
            fillHollows();
        }
        ShadowMap.generate();
        generateResources();
        generateDynamicsObjects(randomSpawn);
        ShadowMap.generate();

        Sun.createSun();

        log("World generator: generating done!\n");
        createText(42, 50, "generatingDone", "Done! Starting world..", new SimpleColor(147, 51, 0, 255), "WorldGeneratorState");

        start(creatures);
    }

    private static void da(byte s) {

    }

    private static void generateFlatWorld() {
        log("World generator: generating flat world");
        createText(42, 170, "WorldGeneratorState", "Generating flat world", new SimpleColor(210, 210, 210, 255), "WorldGeneratorState");

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (y > SizeY / 1.5f) {
                    setObject(x, y, new StaticWorldObjects("Gas", x * 16, y * 16));
                } else {
                    setObject(x, y, new StaticWorldObjects("Grass", x * 16, y * 16));
                }
            }
        }
    }

    private static void generateMountains() {
        log("World generator: generating mountains");
        createText(42, 140, "generateMountainsText", "Generating mountains", new SimpleColor(210, 210, 210, 255), "WorldGeneratorState");

        float randGrass = 2f;           //шанс появления неровности, выше число - ниже шанс
        float randAir = 3.5f;           //шанс появления воздуха вместо блока, выше число - ниже шанс
        float iterations = 3f;          //количество итераций генерации
        float mountainHeight = 24000f;  //шанс появления высоких гор, выше число - выше шанс

        for (int i = 0; i < iterations; i++) {
            for (int x = 1; x < SizeX - 1; x++) {
                for (int y = SizeY / 3; y < SizeY - 1; y++) {
                    randGrass += y / (mountainHeight * SizeY);

                    if ((getObject(x + 1, y).getType() == StaticObjectsConst.Types.SOLID || getObject(x - 1, y).getType() == StaticObjectsConst.Types.SOLID || getObject(x, y + 1).getType() == StaticObjectsConst.Types.SOLID || getObject(x, y - 1).getType() == StaticObjectsConst.Types.SOLID) && Math.random() * randGrass < 1) {
                        setObject(x, y, new StaticWorldObjects("Grass", x * 16, y * 16));
                    } else if (Math.random() * randAir < 1) {
                        getObject(x, y).destroyObject();
                    }
                }
            }
        }
    }

    private static void fillHollows() {
        log("World generator: filling hollows");
        createText(42, 80, "fillHollowsText", "Filling hollows", new SimpleColor(210, 210, 210, 255), "WorldGeneratorState");

        boolean[][] visited = new boolean[SizeX][SizeY];

        for (int x = 1; x < SizeX - 1; x++) {
            for (int y = 1; y < SizeY - 1; y++) {
                if (getObject(x, y).getType() == StaticObjectsConst.Types.GAS && !visited[x][y]) {
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
        createText(42, 110, "smoothWorldText", "Smoothing world", new SimpleColor(210, 210, 210, 255), "WorldGeneratorState");

        float smoothingChance = 3f; //шанс сглаживания, выше число - меньше шанс

        for (int x = 1; x < SizeX - 1; x++) {
            for (int y = 1; y < SizeY - 1; y++) {
                if (getObject(x, y).getType() != StaticObjectsConst.Types.GAS && (getObject(x + 1, y).getType() == StaticObjectsConst.Types.SOLID && getObject(x - 1, y).getType() == StaticObjectsConst.Types.SOLID && getObject(x, y - 1).getType() == StaticObjectsConst.Types.SOLID) || (getObject(x + 1, y + 1).getType() == StaticObjectsConst.Types.SOLID && getObject(x - 1, y - 1).getType() == StaticObjectsConst.Types.SOLID) || (getObject(x - 1, y + 1).getType() == StaticObjectsConst.Types.SOLID && getObject(x + 1, y - 1).getType() == StaticObjectsConst.Types.SOLID) && Math.random() * smoothingChance < 1) {
                    setObject(x, y, new StaticWorldObjects("Grass", x * 16, y * 16));
                } else if ((getObject(x, y + 1).getType() == StaticObjectsConst.Types.SOLID && getObject(x, y - 1).getType() == StaticObjectsConst.Types.SOLID) || (getObject(x + 1, y).getType() == StaticObjectsConst.Types.SOLID && getObject(x - 1, y).getType() == StaticObjectsConst.Types.SOLID) && Math.random() * smoothingChance < 1) {
                    setObject(x, y, new StaticWorldObjects("Grass", x * 16, y * 16));
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

            if (cx > 0 && getObject(cx - 1, cy).getType() == StaticObjectsConst.Types.GAS && !visited[cx - 1][cy]) {
                queue.offer(new int[]{cx - 1, cy});
                visited[cx - 1][cy] = true;
            }
            if (cx < SizeX - 1 && getObject(cx + 1, cy).getType() == StaticObjectsConst.Types.GAS && !visited[cx + 1][cy]) {
                queue.offer(new int[]{cx + 1, cy});
                visited[cx + 1][cy] = true;
            }
            if (cy > 0 && getObject(cx, cy - 1).getType() == StaticObjectsConst.Types.GAS && !visited[cx][cy - 1]) {
                queue.offer(new int[]{cx, cy - 1});
                visited[cx][cy - 1] = true;
            }
            if (cy < SizeY - 1 && getObject(cx, cy + 1).getType() == StaticObjectsConst.Types.GAS && !visited[cx][cy + 1]) {
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
            setObject(x, y, new StaticWorldObjects("Grass", x * 16, y * 16));
        }
    }

    private static void generateCanyons() {

    }

    private static void generateResources() {
        log("World generator: generating resources");

        PerlinNoiseGenerator.main(SizeX, SizeY, 1, 15, 1, 0.8, 4);

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (getObject(x, y + 1) != null && getObject(x, y + 1).getType() != StaticObjectsConst.Types.GAS) { // Генерация земли под блоками травы
                    setObject(x, y, new StaticWorldObjects("Dirt", x * 16, y * 16));
                }

                if (ShadowMap.colorDegree[x][y] >= 3) { // Генерация камня
                    setObject(x, y, new StaticWorldObjects("Stone", x * 16, y * 16));
                }

                if (PerlinNoiseGenerator.noise[x][y] && ShadowMap.colorDegree[x][y] >= 3) { // Генерация руды
                    setObject(x, y, new StaticWorldObjects("IronOre", x * 16, y * 16));
                }

                if (ShadowMap.colorDegree[x][y] == 2) { // Генерация перехода между землёй и камнем
                    if (getObject(x, y + 1) != null && !getObject(x, y + 1).getName().equals("DirtStone")) {
                        setObject(x, y, new StaticWorldObjects("DirtStone", x * 16, y * 16));
                    }
                }
            }
        }
    }

    public static void generateDynamicsObjects(boolean randomSpawn) {
        DynamicObjects.add(new DynamicWorldObjects(false, 0.003f, 1, 0, randomSpawn ? (int) (Math.random() * (SizeX * 16)) : SizeX * 8f, 100, defPath + "\\src\\assets\\World\\creatures\\player.png"));
        DynamicObjects.set(0, new DynamicWorldObjects(false, 0.003f, 1, 0, randomSpawn ? (int) (Math.random() * (SizeX * 16)) : SizeX * 8f, 100, defPath + "\\src\\assets\\World\\creatures\\player.png"));}

    public static void start(boolean generateCreatures) {
        CreatePlanet.delete();

        new Thread(new Physics()).start();
        if (generateCreatures) {
            new Thread(new CreaturesGenerate()).start();
        }
        Window.start = true;
    }
}