package core.World.WorldGenerator;

import core.*;
import core.EventHandling.EventHandler;
import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;
import core.World.PerlinNoiseGenerator;
import core.World.World;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static core.Global.*;
import static core.World.StaticWorldObjects.StaticObjectsConst.getConst;
import static core.World.StaticWorldObjects.StaticWorldObjects.*;

public class WorldGenerator {
    private static final Logger log = LogManager.getLogger();

    public static float intersDamageMultiplier = 40f, minVectorIntersDamage = 1.8f;
    public static final int copySize = 50;

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
        log.debug("first step");

        int SizeX = params.size;
        int SizeY = params.size;
        World world = new World(SizeX, SizeY);
        Global.world = world;

        boolean simple = params.simple;
        boolean randomSpawn = params.randomSpawn;
        boolean creatures = params.creatures;

        log.debug("version: 2.0");
        log.debug("World generator: starting generating world with size: {}x{}", world.sizeX, world.sizeY);

        var playGameScene = new PlayGameScene();

        gameScene.addPreload(playGameScene);
        StaticObjectsConst.setDestroyed();

        step(() -> {
            log.debug("generating relief");
            generateRelief(world);
        });

//        кажется, теперь это лишнее
//        step(() -> {
//            log.debug("generating shadow map");
//            ShadowMap.generate();
//        });

        step(() -> {
            //log.debug("generating resources");
            //generateResources();
        });

        step(() -> {
            log.debug("generating envrionments");
            generateEnvironments(world);
        });

        step(() -> {
            log.debug("generating: copy");
            copy();
        });

        step(() -> {
            log.debug("generating caves");
            generateCaves();
        });

        step(() -> {
            log.debug("regenerating shadow map");
            ShadowMap.generate();
        });

        step(() -> {
            log.debug("generating temperature map");
            TemperatureMap.create(playGameScene);
        });

        step(() -> {
            log.debug("generating player");
            Player.createPlayer(randomSpawn);
        });

        step(() -> {
            log.debug("generating done!");
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

    private static void log(String text) {
        // scheduler.post(() -> texts.get("WorldGeneratorState").text += text, 0.5f * Time.ONE_SECOND);
    }

    private static void copy() {
        int height = world.sizeY;
        int width = world.sizeX;

        for (int x = 0; x < copySize; x++) {
            for (int y = 0; y < height; y++) {
                short value = world.get(x, y);

                world.set(width - copySize + x, y, value, false);
            }
        }
    }

    private static void generateRelief(World world) {
        //last biomes для плавного перетекания биомов
        Biomes lastBiomes = Biomes.getDefault();
        Biomes currentBiomes = Biomes.getRand();

        float lastX = 0;
        float lastY = world.sizeY / 2f;
        float angle = 90;

        //чем ближе к 90 тем меньше максимальный угол наклона линии генерации
        int upperBorder = currentBiomes.getUpperBorder();
        int bottomBorder = currentBiomes.getBottomBorder();
        int blockGradient = currentBiomes.getBlockGradientChance();
        //в блоках
        int lastSwapBiomes = 0;
        int minSwapBiomes = 200;

        short[] availableBlocks = currentBiomes.getBlocks();

        do {
            angle = Math.clamp(angle + ((float) (Math.random() * blockGradient) - blockGradient / 2f), Math.clamp(upperBorder + (lastY - world.sizeY / 2f), upperBorder, 90), Math.clamp(bottomBorder - (world.sizeY / 2f - lastY), 90, bottomBorder));

            int iters = (int) (Math.random() * (90 - Math.abs(90 - angle)));
            float deltaX = (float) (Math.sin(Math.toRadians(angle)));
            float deltaY = (float) (Math.cos(Math.toRadians(angle)));

            for (int j = 0; j < iters; j++) {
                lastY += deltaY;
                lastX += deltaX;

                if (lastX < world.sizeX && lastY > 0) {
                    world.setBiomes((int) lastX, currentBiomes);
                    lastSwapBiomes++;

                    if (lastSwapBiomes > minSwapBiomes && Math.random() * lastSwapBiomes - minSwapBiomes > 30) {
                        lastBiomes = currentBiomes;
                        currentBiomes = Biomes.getRand();
                        availableBlocks = currentBiomes.getBlocks();
                        lastSwapBiomes = 0;

                        upperBorder = currentBiomes.getUpperBorder();
                        bottomBorder = currentBiomes.getBottomBorder();
                        blockGradient = currentBiomes.getBlockGradientChance();
                    }

                    if (lastSwapBiomes < 20 && Math.random() * lastSwapBiomes < 5) {
                        for (int y = 0; y < lastY; y++) {
                            world.set((int) lastX, y, lastBiomes.getBlocks()[(int) Math.min(lastBiomes.getBlocks().length - 1, lastY - y)], false);
                        }
                    } else {
                        for (int y = 0; y < lastY; y++) {
                            world.set((int) lastX, y, availableBlocks[(int) Math.min(availableBlocks.length - 1, lastY - y)], false);
                        }
                    }
                } else {
                    break;
                }
            }
            //90 расстояние между миром и скопированным куском, чтоб рельеф был более менее правильный
        } while (!(lastX + copySize + 90 > world.sizeX));

        doItAgain(lastY, currentBiomes);
    }

    //что то типа сглаживания
    //todo просто чтоб работало, потом сделаю красиво
    private static void doItAgain(float lastY, Biomes currentBiome) {
        float lastX = world.sizeX - copySize - 90;
        double delta = 90;
        double delt = findTopmostSolidBlock(0, 2) - lastY;

        float angle = (float) Math.toDegrees(Math.atan2(delta, delt));

        float deltaX = (float) (Math.sin(Math.toRadians(angle)));
        float deltaY = (float) (Math.cos(Math.toRadians(angle)));

        do {
            for (int j = 0; j < 90; j++) {
                lastY += deltaY;
                lastX += deltaX;
                //todo доделать бесшовный переход задников (backdrop 10 привязка)
                world.setBiomes(Math.max((int) lastX - 1, 0), world.getBiomes(j));

                if (lastX < world.sizeX && lastY > 0) {
                    for (int y = 0; y < lastY; y++) {
                        world.set((int) lastX, y, currentBiome.getBlocks()[(int) Math.min(currentBiome.getBlocks().length - 1, lastY - y)], true);
                    }
                } else {
                    break;
                }
            }
        } while (!(lastX + copySize > world.sizeX));
    }

    private static void generateCaves() {
        for (int b = 0; b < world.sizeX / 600; b++) {
            int minRadius = 2;
            int maxRadius = 4;
            int startRadius = Math.max(minRadius, (int) (Math.random() * maxRadius));
            int x = (int) (Math.random() * world.sizeX);
            int y = findTopmostSolidBlock(x, 5);

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

            float deltaY = (float) (Math.cos(Math.toRadians(angle)));
            float deltaX = (float) (Math.sin(Math.toRadians(angle)));

            if (x + deltaX * (iters + maxRadius) > world.sizeX || x + deltaX * (iters + maxRadius) < 0) {
                break;
            }

            for (int j = 0; j < iters; j++) {
                //todo позже
                //отростки от основной пещеры, надо настроить
//                if (Math.random() * 500 < 1) {
//                    generateCave(x, y, radius, 2, (int) radius);
//                }

                y += deltaY;
                x += deltaX;

                if (world.inBounds((int) x, (int) y) && start == 0) {
                    if (world.get((int) x, (int) y) != -1) {
                        start = (int) y;
                    }
                    world.destroy((int) x, (int) y);

                    for (int i = (int) (x - radius); i <= x + radius; i++) {
                        for (int k = (int) (y - radius); k <= y + radius; k++) {
                            if (i > 0 && i < world.sizeX && k > 0 && k < world.sizeY) {
                                world.destroy(i, k);
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
        generateTrees(world);
        generateDecorStones(world);
        generateHerb(world);
        Structures.clearStructuresMap();
    }

    private static void generateTrees(World world) {
        //todo проверить
        //generateForest(80, 2, 20, 4, 8, "tree0", "tree1");
    }

    private static void generateDecorStones(World world) {
        float chance = 40;
        for (int x = 0; x < world.sizeX; x++) {
            if (Math.random() * chance < 1) {
                int y = findFreeVerticalCell(x);

                if (y - 1 > 0 && getType(world.get(x, y - 1)) == StaticObjectsConst.Types.SOLID) {
                    if (getResistance(world.get(x, y - 1)) >= 100) {
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

    private static void generateResources() {
        //вынес для удобства
        //todo почему то дублирует путь, типа C:\other\-270_On_Celsius\-270_On_Celsius\src\
        //причем с условной грязью все нормально, причину не знаю, надо разобраться
        short obj = createStatic("Blocks/aluminium");

        for (int i = 0; i < Math.random() * ((world.sizeX + world.sizeY) / 100f); i++) {

            boolean[][] noise = PerlinNoiseGenerator.createBoolNoise((int) (Math.random() * 30), (int) (Math.random() * 30), 1.3f);
            Point2i randPos = randAtGround();

            for (int x = 0; x < noise.length; x++) {
                for (int y = 0; y < noise[0].length; y++) {
                    if (noise[x][y] && StaticWorldObjects.getType(world.get(x + randPos.x, y + randPos.y)) == StaticObjectsConst.Types.SOLID) {
                        world.set(x + randPos.x, y + randPos.y, obj, false);
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
        world.registerListener(new WorkbenchLogic());
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

    private static Point2i randAtGround() {
        int randX = (int) (Math.random() * world.sizeX);

        for (int i = world.sizeY; i > 0; i--) {
            if (StaticWorldObjects.getType(world.get(randX, i)) == StaticObjectsConst.Types.SOLID) {
                return new Point2i(randX, (int) (Math.random() * world.sizeY - i) + i);
            }
        }
        return null;
    }
}
