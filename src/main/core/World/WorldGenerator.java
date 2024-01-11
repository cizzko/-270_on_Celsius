package core.World;

import core.EventHandling.Logging.Json;
import core.UI.GUI.Menu.CreatePlanet;
import core.Window;
import core.World.Creatures.CreaturesGenerate;
import core.World.Creatures.Physics;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.StaticWorldObjects.Structures.ElectricCables;
import core.World.StaticWorldObjects.Structures.Factories;
import core.World.Creatures.Player.Player;
import core.World.Creatures.DynamicWorldObjects;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.Textures.ShadowMap;
import core.Utils.SimpleColor;
import core.World.StaticWorldObjects.StaticBlocksEvents;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.Structures.Structures;
import core.World.Textures.TextureDrawing;
import core.World.Weather.Sun;
import java.awt.Point;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import static core.EventHandling.Logging.Logger.log;
import static core.UI.GUI.CreateElement.*;
import static core.Window.*;
import static core.World.StaticWorldObjects.StaticWorldObjects.*;

public class WorldGenerator {
    public static int SizeX, SizeY, dayCount = 0;
    public static float intersDamageMultiplier = 40f, minVectorIntersDamage = 1.8f;
    public static short[] StaticObjects;
    private static final CopyOnWriteArrayList<StaticBlocksEvents> listeners = new CopyOnWriteArrayList<>();
    public static ArrayDeque<DynamicWorldObjects> DynamicObjects = new ArrayDeque<>();

    public static HashMap<String, Object> getWorldData() {
        HashMap<String, Object> objects = TemperatureMap.getTemperatures();

        objects.put("StaticWorldObjects", convertNames(StaticObjects));
        objects.put("DynamicWorldObjects", DynamicObjects);
        objects.put("ShadowsData", ShadowMap.getShadowData());
        objects.put("Inventory", Inventory.inventoryObjects);

        objects.put("WorldSizeX", SizeX);
        objects.put("WorldSizeY", SizeY);
        objects.put("WorldIntersDamageMultiplier", intersDamageMultiplier);
        objects.put("WorldMinVectorIntersDamage", minVectorIntersDamage);
        objects.put("WorldDayCount", dayCount);
        objects.put("WorldCurrentTime", Sun.currentTime);
        objects.put("WorldGenerateCreatures", buttons.get(Json.getName("GenerateCreatures")).isClicked);

        return objects;
    }

    private static String[] convertNames(short[] blocks) {
        String[] names = new String[blocks.length];

        for (int i = 0; i < names.length; i++) {
            names[i] = StaticWorldObjects.getFileName(blocks[i]);
        }
        return names;
    }

    public static void registerListener(StaticBlocksEvents listener) {
        listeners.add(listener);
    }

    public static void setObject(int x, int y, short object) {
        assert object != -1;

        if (StaticObjectsConst.getConst(getId(object)).optionalTiles != null) {
            new Thread(() -> {
                short[][] tiles = StaticObjectsConst.getConst(getId(object)).optionalTiles;

                for (int blockX = 0; blockX < tiles.length; blockX++) {
                    for (int blockY = 0; blockY < tiles[0].length; blockY++) {
                        if (getType(tiles[blockX][blockY]) != StaticObjectsConst.Types.GAS && tiles[blockX][blockY] != 0) {
                            StaticObjects[(x + blockX) + SizeX * (y + blockY)] = tiles[blockX][blockY];
                        }
                    }
                }
                StaticObjects[x + SizeX * y] = object;
            }).start();
        } else {
            StaticObjects[x + SizeX * y] = object;
        }

        if (start) {
            for (StaticBlocksEvents listener : listeners) {
                listener.placeStatic(x, y, object);
            }
        }
    }

    public static short getObject(int x, int y) {
        if (x < 0 || x > SizeX || y < 0 || y > SizeY) {
            return -1;
        }
        return StaticObjects[x + SizeX * y];
    }

    public static void destroyObject(int cellX, int cellY) {
        short id = WorldGenerator.getObject(cellX, cellY);

        if (id != 0) {
            if (StaticObjectsConst.getConst(getId(id)).hasMotherBlock || StaticObjectsConst.getConst(getId(id)).optionalTiles != null) {
                Point root = Player.findRoot(cellX, cellY);

                if (root != null) {
                    deleteTiles(getObject(root.x, root.y), root.x, root.y);
                }
            } else {
                WorldGenerator.setObject(cellX, cellY, (short) 0);
                ShadowMap.update();
            }

            if (start) {
                for (StaticBlocksEvents listener : listeners) {
                    listener.destroyStatic(cellX, cellY, id);
                }
            }
        }
    }

    private static void deleteTiles(short id, int cellX, int cellY) {
        for (int blockX = 0; blockX < StaticObjectsConst.getConst(getId(id)).optionalTiles.length; blockX++) {
            for (int blockY = 0; blockY < StaticObjectsConst.getConst(getId(id)).optionalTiles[0].length; blockY++) {
                WorldGenerator.StaticObjects[(cellX + blockX) + SizeX * (cellY + blockY)] = (short) 0;
            }
        }
        ShadowMap.update();
    }

    public static int findX(int x, int y) {
        return ((x + SizeX * y) % SizeX) * TextureDrawing.blockSize;
    }

    public static int findY(int x, int y) {
        return ((x + SizeX * y) / SizeX) * TextureDrawing.blockSize;
    }

    public static void generateWorld() {
        new Thread(() -> {
            createText(42, 170, "WorldGeneratorState", "First iteration: ", SimpleColor.DIRTY_BRIGHT_WHITE, "WorldGeneratorState");

            int SizeX = getSliderPos("worldSize") + 20;
            int SizeY = getSliderPos("worldSize") + 20;

            //todo чтоб не вылетала ошибка, если игрок не переходил в другой раздел настроек генерации, и кнопка не была создана
            boolean simple = buttons.containsKey(Json.getName("GenerateSimpleWorld")) && buttons.get(Json.getName("GenerateSimpleWorld")).isClicked;
            boolean randomSpawn = buttons.containsKey(Json.getName("RandomSpawn")) && buttons.get(Json.getName("RandomSpawn")).isClicked;
            boolean creatures = buttons.containsKey(Json.getName("GenerateCreatures")) && buttons.get(Json.getName("GenerateCreatures")).isClicked;

            log("\nWorld generator: version: 1.0, written at dev 0.0.0.5" + "\nWorld generator: starting generating world with size: x - " + SizeX + ", y - " + SizeY);

            StaticObjects = new short[(SizeX + 1) * (SizeY + 1)];
            WorldGenerator.SizeX = SizeX;
            WorldGenerator.SizeY = SizeY;

            generateBlocks(simple);
            TemperatureMap.create();
            Player.createPlayer(randomSpawn);

            log("World generator: generating done!\n");
            texts.get("WorldGeneratorState").text += "\\nGenerating done! Starting world..";

            try { Thread.sleep(10000); } catch (Exception e) {}
            start(creatures);
        }).start();
    }

    private static void generateBlocks(boolean simple) {
        StaticObjectsConst.setDestroyed();
        generateFlatWorld();

        if (simple) {
            texts.get("WorldGeneratorState").text += "\\nSecond iteration: generating shadows";
            ShadowMap.generate();
            generateResources();
        } else {
            generateMountains();
            smoothWorld();
            fillHollows();

            texts.get("WorldGeneratorState").text += "\\nThird iteration: generating shadows";
            ShadowMap.generate();
            generateResources();
            generateEnvironments();
            ShadowMap.generate();
        }
    }

    private static void generateFlatWorld() {
        log("World generator: generating flat world");
        texts.get("WorldGeneratorState").text += "generating flat world";

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (y > SizeY / 1.5f) {
                    setObject(x, y, createStatic("Blocks/air"));
                } else {
                    setObject(x, y, createStatic("Blocks/grass"));
                }
            }
        }
    }

    private static void generateMountains() {
        log("World generator: generating mountain");
        texts.get("WorldGeneratorState").text += ", generating mountains";

        float randGrass = 2f;           //chance of unevenness, the higher the number - the lower the chance
        float randAir = 3.5f;           //chance of air appearing instead of a block, higher number - lower chance
        float iterations = 3f;          //iterations of generations
        float mountainHeight = 24000f;  //chance of high mountains appearing, higher number - higher chance

        for (int i = 0; i < iterations; i++) {
            for (int x = 1; x < SizeX - 1; x++) {
                for (int y = SizeY / 3; y < SizeY - 1; y++) {
                    randGrass += y / (mountainHeight * SizeY);

                    if ((getType(getObject(x + 1, y)) == StaticObjectsConst.Types.SOLID || getType(getObject(x - 1, y)) == StaticObjectsConst.Types.SOLID || getType(getObject(x, y + 1)) == StaticObjectsConst.Types.SOLID || getType(getObject(x, y - 1)) == StaticObjectsConst.Types.SOLID) && Math.random() * randGrass < 1) {
                        setObject(x, y, createStatic("Blocks/grass"));
                    } else if (Math.random() * randAir < 1) {
                        destroyObject(x, y);
                    }
                }
            }
        }
    }

    private static void fillHollows() {
        log("World generator: filling hollows");
        texts.get("WorldGeneratorState").text += ", filling hollows";

        boolean[][] visited = new boolean[SizeX][SizeY];

        for (int x = 1; x < SizeX - 1; x++) {
            for (int y = 1; y < SizeY - 1; y++) {
                if (getType(getObject(x, y)) == StaticObjectsConst.Types.GAS && !visited[x][y]) {
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
        texts.get("WorldGeneratorState").text += "\\nSecond step: smoothing world";

        float smoothingChance = 3f; //chance of smoothing, higher number - lower chance

        for (int x = 1; x < SizeX - 1; x++) {
            for (int y = 1; y < SizeY - 1; y++) {
                if ((getType(getObject(x, y)) != StaticObjectsConst.Types.GAS && getType(getObject(x + 1, y)) == StaticObjectsConst.Types.SOLID && getType(getObject(x - 1, y)) == StaticObjectsConst.Types.SOLID && getType(getObject(x, y - 1)) == StaticObjectsConst.Types.SOLID) || (getType(getObject(x + 1, y + 1)) == StaticObjectsConst.Types.SOLID && getType(getObject(x - 1, y - 1)) == StaticObjectsConst.Types.SOLID) || getType(getObject(x - 1, y + 1)) == StaticObjectsConst.Types.SOLID && getType(getObject(x + 1, y - 1)) == StaticObjectsConst.Types.SOLID && Math.random() * smoothingChance < 1) {
                    setObject(x, y, createStatic("Blocks/grass"));
                } else if ((getType(getObject(x, y + 1)) == StaticObjectsConst.Types.SOLID && getType(getObject(x, y - 1)) == StaticObjectsConst.Types.SOLID) || (getType(getObject(x + 1, y)) == StaticObjectsConst.Types.SOLID && getType(getObject(x - 1, y)) == StaticObjectsConst.Types.SOLID) && Math.random() * smoothingChance < 1) {
                    setObject(x, y, createStatic("Blocks/grass"));
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

            if (cx > 0 && getType(getObject(cx - 1, cy)) == StaticObjectsConst.Types.GAS && !visited[cx - 1][cy]) {
                queue.offer(new int[]{cx - 1, cy});
                visited[cx - 1][cy] = true;
            }
            if (cx < SizeX - 1 &&  getType(getObject(cx + 1, cy)) == StaticObjectsConst.Types.GAS && !visited[cx + 1][cy]) {
                queue.offer(new int[]{cx + 1, cy});
                visited[cx + 1][cy] = true;
            }
            if (cy > 0 &&  getType(getObject(cx, cy - 1)) == StaticObjectsConst.Types.GAS && !visited[cx][cy - 1]) {
                queue.offer(new int[]{cx, cy - 1});
                visited[cx][cy - 1] = true;
            }
            if (cy < SizeY - 1 && getType( getObject(cx, cy + 1)) == StaticObjectsConst.Types.GAS && !visited[cx][cy + 1]) {
                queue.offer(new int[]{cx, cy + 1});
                visited[cx][cy + 1] = true;
            }
        }

        return isClosed;
    }

    public static void createStructure(int cellX, int cellY, String name) {
        Structures struct = Structures.getStructure(name);
        if (struct != null) {
            short[][] objects = Structures.bindStructures(struct.blocks);

            for (int x = 0; x < objects.length; x++) {
                for (int y = 0; y < objects[x].length; y++) {
                    if (cellX + x < SizeX && cellY + y < SizeY && cellX + x > 0 && cellY + y > 0 && getId(objects[x][y]) != 0 && getType(objects[x][y]) != StaticObjectsConst.Types.GAS) {
                        setObject(cellX + x, cellY + y, createStatic(getFileName(objects[x][y])));
                    }
                }
            }
        }
    }

    private static void fillAreaWithGrass(List<int[]> area) {
        for (int[] coord : area) {
            int x = coord[0];
            int y = coord[1];
            setObject(x, y, createStatic("Blocks/grass"));
        }
    }

    private static void generateCanyons() {

    }

    private static void generateEnvironments() {
        texts.get("WorldGeneratorState").text += "\\nFourth step: ";

        generateTrees();
        generateDecorStones();
        Structures.clearStructuresMap();
    }

    private static void generateTrees() {
        log("World generator: generating trees");
        texts.get("WorldGeneratorState").text += "generating trees";

        byte[] forests = new byte[SizeX];
        float lastForest = 0;
        float lastForestSize = 0;

        //(maximum size + minimum) should not exceed 127
        float chance = 80;
        float maxForestSize = 20;
        float minForestSize = 2;

        //the first stage - plants seeds for the forests and sets the size
        for (int x = 0; x < SizeX; x++) {
            if (Math.random() * chance < 1 && lastForest != x && lastForest + lastForestSize < x) {
                forests[x] = (byte) ((Math.random() * maxForestSize) + minForestSize);
                lastForest = x;
                lastForestSize = (float) ((forests[x] * Math.random() * 8) + 4);
            }
        }

        //second stage - plants trees by seeds
        for (int x = 0; x < forests.length; x++) {
            if (forests[x] > 0) {
                for (int i = 0; i < forests[x]; i++) {
                    String name = "tree" + (int) (Math.random() * 2);
                    int distance = (int) ((Math.random() * 8) + 4);
                    int xTree = x + (i * distance);
                    int yTree = findFreeVerticalCell(x + (i * distance));

                    if (xTree > 0 && yTree > 0 && xTree < forests.length && !checkInterInsideSolid(xTree, yTree, name) && xTree + Structures.getStructure(name).lowestSolidBlock < SizeX && yTree - 1 < SizeY && getType(getObject(xTree + Structures.getStructure(name).lowestSolidBlock, yTree - 1)) == StaticObjectsConst.Types.SOLID) {
                        createStructure(xTree, yTree, name);
                    }
                }
            }
        }
    }

    private static void generateDecorStones() {
        texts.get("WorldGeneratorState").text += ", generating decor stones";
        log("World generator: generating decor stones");

        float chance = 40;
        for (int x = 0; x < SizeX; x++) {
            if (Math.random() * chance < 1) {
                int y = findFreeVerticalCell(x);

                if (y - 1 > 0 && getType(getObject(x, y - 1)) == StaticObjectsConst.Types.SOLID && getResistance(getObject(x, y - 1)) == 100) {
                    setObject(x, y, StaticWorldObjects.createStatic("Blocks/decorStone"));
                }
            }
        }
    }

    private static boolean checkInterInsideSolid(int xCell, int yCell, String structName) {
        short[][] objects = Structures.bindStructures(Structures.getStructure(structName).blocks);

        for (int x = xCell; x < xCell + objects.length; x++) {
            for (int y = yCell; y < yCell + objects[0].length; y++) {
                if (x > 0 && y > 0 && x < SizeX && y < SizeY && getType(getObject(x, y)) == StaticObjectsConst.Types.SOLID && getType(objects[x - xCell][y - yCell]) == StaticObjectsConst.Types.SOLID) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int findFreeVerticalCell(int x) {
        if (x > 0 && x < SizeX) {
            for (int y = 0; y < SizeY; y++) {
                if (getType(getObject(x, y)) == StaticObjectsConst.Types.GAS) {
                    return y;
                }
            }
        }
        return -1;
    }

    private static void generateResources() {
        log("World generator: generating resources");

        PerlinNoiseGenerator.main(SizeX, SizeY, 1, 15, 1, 0.8f, 4);

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (getType(getObject(x, y + 1)) != StaticObjectsConst.Types.GAS) { //Generating ground under grass blocks
                    setObject(x, y, createStatic("Blocks/dirt"));
                }

                if (ShadowMap.getDegree(x, y) >= 3) { //Generating stone
                    setObject(x, y, createStatic("Blocks/stone"));
                }

                if (PerlinNoiseGenerator.noise[x][y] && ShadowMap.getDegree(x, y) >= 3) { //Generating ore
                    setObject(x, y, createStatic("Blocks/ironOre"));
                }

                if (ShadowMap.getDegree(x, y) == 2) { //Generation of transitions between earth and stone
                    if (!getFileName(getObject(x, y + 1)).equals("Blocks/dirtStone")) {
                        setObject(x, y, createStatic("Blocks/dirtStone"));
                    }
                }
            }
        }
    }

    //jabadoc
    // Looks for the topmost solid block in the strip.
    // Checks each `period` block, and if it is solid, searches for air above it.
    // This increases the search speed by a factor of `period`,
    // but decreases the chance of finding single blocks in the strip by the same amount
    // return -1 if not found
    public static int findTopmostSolidBlock(int cellX, int period) {
        for (int y = SizeY; y > 0; y -= period) {
            if (StaticWorldObjects.getType(getObject(cellX, y)) == StaticObjectsConst.Types.SOLID) {
                for (int i = y; i < y + period; i++) {
                    if (StaticWorldObjects.getType(getObject(cellX, i + 1)) == StaticObjectsConst.Types.GAS && StaticWorldObjects.getType(getObject(cellX, i)) == StaticObjectsConst.Types.SOLID) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public static void start(boolean generateCreatures) {
        CreatePlanet.delete();

        WorldGenerator.registerListener(new Factories());
        Inventory.registerListener(new ElectricCables());
        Sun.createSun();
        Physics.initPhysics();
        if (generateCreatures) {
            CreaturesGenerate.initGenerating();
        }
        Window.start = true;
    }
}
