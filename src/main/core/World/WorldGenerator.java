package core.World;

import core.*;
import core.EventHandling.EventHandler;
import core.ui.menu.CreatePlanet;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.StaticWorldObjects.Structures.ElectricCables;
import core.World.StaticWorldObjects.Structures.Factories;
import core.World.Creatures.Player.Player;
import core.World.Creatures.DynamicWorldObjects;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.Textures.ShadowMap;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.Structures.Structures;
import core.World.Textures.TextureDrawing;
import core.math.MathUtil;
import core.math.Point2i;
import java.util.*;

import static core.EventHandling.Logging.Logger.log;
import static core.Global.*;
import static core.Global.world;
import static core.World.StaticWorldObjects.StaticObjectsConst.getConst;
import static core.World.StaticWorldObjects.StaticWorldObjects.*;

public class WorldGenerator {
    public static float intersDamageMultiplier = 40f, minVectorIntersDamage = 1.8f;

    public static ArrayDeque<DynamicWorldObjects> DynamicObjects = new ArrayDeque<>();

    public static HashMap<String, Object> getWorldData() {
        HashMap<String, Object> objects = TemperatureMap.getTemperatures();

        // objects.put("StaticWorldObjects", convertNames(world.tiles));
        // objects.put("DynamicWorldObjects", DynamicObjects);
        // objects.put("ShadowsData", ShadowMap.getShadowData());
        // objects.put("Inventory", Inventory.inventoryObjects);

        // objects.put("WorldSizeX", SizeX);
        // objects.put("WorldSizeY", SizeY);
        // objects.put("WorldIntersDamageMultiplier", intersDamageMultiplier);
        // objects.put("WorldMinVectorIntersDamage", minVectorIntersDamage);
        // objects.put("WorldDayCount", dayCount);
        // objects.put("WorldCurrentTime", Sun.currentTime);
        // TODO Это не должно читаться с кнопки. Нужно переместить во внутреннее состояние объекта
        // objects.put("WorldGenerateCreatures", buttons.get(Json.getName("GenerateCreatures")).isClicked);

        return objects;
    }

    private static String[] convertNames(short[] blocks) {
        String[] names = new String[blocks.length];

        for (int i = 0; i < names.length; i++) {
            names[i] = StaticWorldObjects.getFileName(blocks[i]);
        }
        return names;
    }

    public static boolean checkPlaceRules(int x, int y, short root) {
        if (getId(world.get(x, y)) != 0) {
            return false;
        }
        if (getConst(getId(root)).optionalTiles != null) {
            short[][] tiles = getConst(getId(root)).optionalTiles;

            for (int xBlock = 0; xBlock < tiles.length; xBlock++) {
                if (getResistance(world.get(x + xBlock, y - 1)) < 100) {
                    return false;
                }
                for (int yBlock = 0; yBlock < tiles[0].length; yBlock++) {
                    if (getId(world.get(x + xBlock, y)) != 0) {
                        return false;
                    }
                }
            }
        } else {
            for (Point2i d : MathUtil.CROSS_OFFSETS) {
                if (!(getResistance(world.get(x + d.x, y + d.y)) < 100)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    public static int findX(int x, int y) {
        return ((x + world.sizeX * y) % world.sizeX) * TextureDrawing.blockSize;
    }

    public static int findY(int x, int y) {
        return ((x + world.sizeX * y) / world.sizeX) * TextureDrawing.blockSize;
    }

    public static void generateWorld(CreatePlanet.GenerationParameters params) {
        // createText(42, 170, "WorldGeneratorState", "First step: ", SimpleColor.DIRTY_BRIGHT_WHITE, "WorldGeneratorState");

        int SizeX = params.size;
        int SizeY = params.size;
        World world = new World(SizeX, SizeY);
        Global.world = world;

        //todo чтоб не вылетала ошибка, если игрок не переходил в другой раздел настроек генерации, и кнопка не была создана
        boolean simple = params.simple;
        boolean randomSpawn = params.randomSpawn;
        boolean creatures = params.creatures;

        log("\nWorld generator: version: 1.0, written at dev 0.0.0.5" + "\nWorld generator: starting generating world with size: x - " + world.sizeX + ", y - " + world.sizeY);

        var playGameScene = new PlayGameScene();
        gameScene.addPreload(playGameScene);

        StaticObjectsConst.setDestroyed();
        step(() -> generateRelief(world));

        step(() -> ShadowMap.generate());
        step(() -> generateResources(world));

        //todo пещеры независимо от радиуса создаются толщиной в один блок
        //generateCaves();
        step(() -> generateEnvironments(world));
        step(() -> ShadowMap.generate());

        step(() -> TemperatureMap.create(playGameScene));
        step(() -> Player.createPlayer(randomSpawn));

        step(() -> {
            log("World generator: generating done!\n");
            appendLog("\\nGenerating done! Starting world..");

            scheduler.post(() -> startGame(playGameScene), Time.ONE_SECOND);
        });
    }

    private static void step(Runnable step) {
        scheduler.post(step)
                .whenComplete((v, e) -> {
                    if (e != null) {
                        e.printStackTrace();
                    }
                });
    }

    private static void appendLog(String text) {
        // scheduler.post(() -> texts.get("WorldGeneratorState").text += text, 0.5f * Time.ONE_SECOND);
    }

    private static void generateRelief(World world) {
        float lastX = 0;
        float lastY = world.sizeY / 2f;
        float angle = 90;

        //чем ближе к 90 тем меньше максимальный угол наклона линии генерации
        int upperBorder = 20;
        int bottomBorder = 160;

        do {
            angle = Math.clamp(angle + ((float) (Math.random() * 60) - 30), Math.clamp(upperBorder + (lastY - world.sizeY / 2f), 20, 90), Math.clamp(bottomBorder - (world.sizeY / 2f - lastY), 90, 160));

            int iters = (int) (Math.random() * (90 - Math.abs(90 - angle)));

            float deltaX = (float) (Math.sin(Math.toRadians(angle)));
            float deltaY = (float) (Math.cos(Math.toRadians(angle)));
            for (int j = 0; j < iters; j++) {
                lastY += deltaY;
                lastX += deltaX;

                if (lastX < world.sizeX && lastY > 0) {
                    for (int y = 0; y < lastY; y++) {
                        short object = createStatic("Blocks/grass");
                        world.set((int) lastX, y, object, false);
                    }
                } else {
                    break;
                }
            }
        } while (!(lastX + 1 > world.sizeX));
    }

    private static void generateCaves() {
        for (int b = 0; b < world.sizeX / 600; b++) {
            int minRadius = 2;
            int maxRadius = 8;
            int startRadius = Math.max(minRadius, (int) (Math.random() * maxRadius));
            int x = (int) (Math.random() * world.sizeX);
            int y = 0;

            for (int i = 0; i < world.sizeY; i++) {
                if (world.get(x, i) != -1) {
                    y = i;
                    break;
                }
            }

            generateCave(x, y, startRadius, minRadius, maxRadius);
        }
    }

    private static void generateCave(float x, float y, float radius, int minRadius, int maxRadius) {
        if (minRadius <= 1 || minRadius == maxRadius) {
            return;
        }

        float startRadius = radius;
        float angle = (float) ((Math.random() * 180) + 90);

        do {
            if (Math.random() * 30 < 1) {
                radius = (int) Math.clamp(radius + (Math.random() * 2) - 1, minRadius, startRadius);
            }
            float start = 0;
            float iters = (int) (Math.random() * 30);
            angle = (float) Math.clamp(angle + ((Math.random() * 100) - 50), 87, 267);

            float deltaY = (float) (Math.cos(Math.toRadians(angle + 180)));
            float deltaX = (float) (Math.sin(Math.toRadians(angle)));

            if (x + deltaX * (iters + maxRadius) > world.sizeX || x + deltaX * (iters + maxRadius) < 0) {
                break;
            }

            for (int j = 0; j < iters; j++) {
                if (Math.random() * 50 < 1) {
                    generateCave(x, y, radius, 1, (int) radius);
                }

                y += deltaY;
                x += deltaX;

                if (x < world.sizeX && y < world.sizeY && x > 0 && y > 0) {
                    if (start == 0) {
                        if (world.get((int) x, (int) y) != -1) {
                            start = (int) y;
                        }
                    }
                    world.destroy((int) x, (int) y);

                    for (int i = (int) (x - radius); i <= x + radius; i++) {
                        for (int k = (int) (y - radius); k <= y + radius; k++) {
                            if (i > 0 && i < world.sizeX && k > 0 && k < world.sizeY) {
                                world.destroy((int) x, (int) y);
                            }
                        }
                    }
                } else {
                    break;
                }
            }
            if (start > 0 && Math.random() * world.sizeY < y / 150f) {
                break;
            }
        } while (y < world.sizeY);
    }

    private static void generateEnvironments(World world) {
        appendLog("\\nFourth step: ");

        generateTrees(world);
        generateDecorStones(world);
        generateHerb(world);
        Structures.clearStructuresMap();
    }

    private static void generateTrees(World world) {
        log("World generator: generating trees");
        appendLog("generating trees");

        //todo проверить
        //generateForest(80, 2, 20, 4, 8, "tree0", "tree1");
    }

    private static void generateDecorStones(World world) {
        log("World generator: generating decor stones");
        appendLog(", generating decor stones");

        float chance = 40;
        for (int x = 0; x < world.sizeX; x++) {
            if (Math.random() * chance < 1) {
                int y = findFreeVerticalCell(x);

                if (y - 1 > 0 && getType(world.get(x, y - 1)) == StaticObjectsConst.Types.SOLID) {
                    if (getResistance(world.get(x, y - 1)) == 100) {
                        short object = StaticWorldObjects.createStatic("Blocks/smallStone");
                        world.set(x, y, object, false);
                    }
                }
            }
        }
    }

    private static void generateHerb(World world) {
        generateForest(10, 1, 20, 1, 0, "Blocks/herb");
    }

    private static void generateForest(int chance, int minForestSize, int maxForestSize, int minSpawnDistance, int maxSpawnDistance, String... structuresName) {
        byte[] forests = new byte[world.sizeX];
        float lastForest = 0;
        float lastForestSize = 0;

        //(maximum size + minimum) should not exceed 127
        //the first stage - plants seeds for the forests and sets the size
        for (int x = 0; x < world.sizeX; x++) {
            if (Math.random() * chance < 1 && lastForest != x && lastForest + lastForestSize < x) {
                forests[x] = (byte) ((Math.random() * maxForestSize) + minForestSize);
                lastForest = x;
                lastForestSize = (float) ((forests[x] * Math.random() * 8) + 4);
            }
        }

        //second stage - plants structures by seeds
        for (int x = 0; x < forests.length; x++) {
            if (forests[x] > 0) {
                for (int i = 0; i < forests[x]; i++) {
                    String name = structuresName[(int) (Math.random() * structuresName.length)];
                    int distance = (int) (Math.random() * (maxSpawnDistance - minSpawnDistance)) + minSpawnDistance;
                    int xStruct = x + (i * distance);
                    int yStruct = findFreeVerticalCell(x + (i * distance));

                    if (xStruct > 0 && yStruct > 0 && xStruct < forests.length) {
                        short object = StaticWorldObjects.createStatic(name);
                        world.set(xStruct, yStruct, object, true);
                    }
                }
            }
        }
    }

    private static boolean checkInterInsideSolid(int xCell, int yCell, String structName) {
        Structures structure = Structures.getStructure(structName);
        if (structure == null) {
            return false;
        }
        short[][] objects = Structures.bindStructures(structure.blocks);

        for (int x = xCell; x < xCell + objects.length; x++) {
            for (int y = yCell; y < yCell + objects[0].length; y++) {
                if (x > 0 && y > 0 && x < world.sizeX && y < world.sizeY) {
                    if (getType(world.get(x, y)) == StaticObjectsConst.Types.SOLID && getType(objects[x - xCell][y - yCell]) == StaticObjectsConst.Types.SOLID) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean checkInterInsideSolid(int xCell, int yCell, short[][] blocks) {
        for (int x = xCell; x < xCell + blocks.length; x++) {
            for (int y = yCell; y < yCell + blocks[0].length; y++) {
                if (x > 0 && y > 0 && x < world.sizeX && y < world.sizeY) {
                    if (getType(world.get(x, y)) == StaticObjectsConst.Types.SOLID && getType(blocks[x - xCell][y - yCell]) == StaticObjectsConst.Types.SOLID) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int findFreeVerticalCell(int x) {
        if (x <= 0 || x >= world.sizeX) {
            return -1;
        }
        for (int y = 0; y < world.sizeY; y++) {
            if (getType(world.get(x, y)) == StaticObjectsConst.Types.GAS) {
                return y;
            }
        }
        return -1;
    }

    private static void generateResources(World world) {
        log("World generator: generating resources");

        PerlinNoiseGenerator.main(world.sizeX, world.sizeY, 1, 15, 1, 0.8f, 4);

        for (int x = 0; x < world.sizeX; x++) {
            for (int y = 0; y < world.sizeY; y++) {
                if (getType(world.get(x, y + 1)) != StaticObjectsConst.Types.GAS) { // Generating ground under grass blocks
                    short object = createStatic("Blocks/dirt");
                    world.set(x, y, object, false);
                }

                if (ShadowMap.getDegree(x, y) >= 3) { // Generating stone
                    short object = createStatic("Blocks/stone");
                    world.set(x, y, object, false);
                }

                if (PerlinNoiseGenerator.noise[x][y] && ShadowMap.getDegree(x, y) >= 3) { //Generating ore
                    short object = createStatic("Blocks/aluminum");
                    world.set(x, y, object, false);
                }

                if (ShadowMap.getDegree(x, y) == 2) { // Generation of transitions between earth and stone
                    if (!getFileName(world.get(x, y + 1)).equals("Blocks/dirtStone")) {
                        short object = createStatic("Blocks/dirtStone");
                        world.set(x, y, object, false);
                    }
                }
            }
        }
    }

    // jabadoc
    // Looks for the topmost solid block in the strip.
    // Checks each `period` block, and if it is solid, searches for air above it.
    // This increases the search speed by a factor of `period`,
    // but decreases the chance of finding single blocks in the strip by the same amount
    // return -1 if not found
    public static int findTopmostSolidBlock(int cellX, int period) {
        for (int y = world.sizeY; y > 0; y -= period) {
            if (StaticWorldObjects.getType(world.get(cellX, y)) == StaticObjectsConst.Types.SOLID) {
                for (int i = y; i < y + period; i++) {
                    if (StaticWorldObjects.getType(world.get(cellX, i + 1)) == StaticObjectsConst.Types.GAS) {
                        if (StaticWorldObjects.getType(world.get(cellX, i)) == StaticObjectsConst.Types.SOLID) {
                            return i;
                        }
                    }
                }
            }
        }
        return -1;
    }

    private static void startGame(PlayGameScene playGameScene) {
        world.registerListener(new Factories());
        Inventory.registerListener(new ElectricCables());
        Inventory.registerListener(new Factories());
        Inventory.create();

        EventHandler.setDebugValue(() -> {
            if (DynamicObjects.isEmpty()) {
                return null;
            }
            var player = DynamicObjects.getFirst();
            return "[Player] x: " + player.getX() + ", y: " + player.getY();
        });

        gameScene.onPreloadCompletion(() -> {
            UI.createPlanet().hide();

            setGameScene(playGameScene);
            gameState = GameState.PLAYING;
        });
    }
}
