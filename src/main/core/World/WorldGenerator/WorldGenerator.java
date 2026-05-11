package core.World.WorldGenerator;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.*;
import core.UI.menu.CreatePlanet;
import core.World.Creatures.DynamicWorldObjects;
import core.World.Creatures.Player.Player;
import core.World.PerlinNoiseGenerator;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.StaticObjectsConst.Types;
import core.World.StaticWorldObjects.Structures.Structures;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.Textures.ShadowMap;
import core.World.Textures.TextureDrawing;
import core.World.World;
import core.math.Point2i;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static core.Global.*;

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

    //для рисования
    public static int findX(int x, int y) {
        return ((x + world.sizeX * y) % world.sizeX) * TextureDrawing.blockSize;
    }

    public static int findY(int x, int y) {
        return ((x + world.sizeX * y) / world.sizeX) * TextureDrawing.blockSize;
    }

    public static void generateWorld(CreatePlanet.GenerationParameters params) {
        long startTime = System.currentTimeMillis();
        int SizeX = params.size;
        int SizeY = params.size;
        World world = new World(SizeX, SizeY);
        Global.world = world;

        boolean simple = params.simple;
        boolean randomSpawn = params.randomSpawn;
        boolean creatures = params.creatures;

        log.debug("version: 2.2");
        log.debug("World generator: starting generating world with size: {}x{}", world.sizeX, world.sizeY);

        var playGameScene = new PlayGameScene();

        gameScene.addPreload(playGameScene);

        step(() -> {
            log.debug("generating relief {}ms", System.currentTimeMillis() - startTime);
            generateRelief(world);
        });

        step(() -> {
            log.debug("generating environment {}ms", System.currentTimeMillis() - startTime);
            generateEnvironments(world);
        });

        step(() -> {
            log.debug("generating caves {}ms", System.currentTimeMillis() - startTime);
            generateCaves();
        });

        step(() -> {
            log.debug("generating: copy {}ms", System.currentTimeMillis() - startTime);
            copy();
        });

        step(() -> {
            log.debug("regenerating shadow map {}ms", System.currentTimeMillis() - startTime);
            ShadowMap.generate();
        });

        step(() -> {
            log.debug("generating temperature map {}ms", System.currentTimeMillis() - startTime);
            TemperatureMap.create();
        });

        step(() -> {
            log.debug("generating player {}ms", System.currentTimeMillis() - startTime);
            Player.createPlayer(randomSpawn);
        });

        step(() -> {
            log.debug("generating done! {}ms", System.currentTimeMillis() - startTime);
            scheduler.post(() -> startGame(playGameScene), Time.ONE_SECOND);
            saveWorldImage(world.tiles, world.sizeX, world.sizeY);
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
                world.set(width - copySize + x, y, world.getBlock(x, y), false);
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

        int upperBorder = currentBiomes.getUpperBorder();
        int bottomBorder = currentBiomes.getBottomBorder();
        int blockGradient = currentBiomes.getBlockGradientChance();
        //в блоках
        int lastSwapBiomes = 0;
        int minSwapBiomes = 200;

        short[] availableBlocks = currentBiomes.getBlocks();

        do {
            angle = Math.clamp(angle + ((float) (Math.random() * blockGradient) - blockGradient / 2f), Math.clamp(upperBorder + (lastY - world.sizeY / 2f), upperBorder, 90), Math.clamp(bottomBorder - (world.sizeY / 2f - lastY), 90, bottomBorder));

            int iters = (int) (Math.random() * 150 / (90 - Math.abs(90 - angle)));
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
                            world.set((int) lastX, y, Global.content.getConstByBlockId(lastBiomes.getBlocks()[(int) Math.min(lastBiomes.getBlocks().length - 1, lastY - y)]), false);
                        }
                    } else {
                        for (int y = 0; y < lastY; y++) {
                            world.set((int) lastX, y, Global.content.getConstByBlockId(availableBlocks[(int) Math.min(availableBlocks.length - 1, lastY - y)]), false);
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
                        int blockId = currentBiome.getBlocks()[(int) Math.min(currentBiome.getBlocks().length - 1, lastY - y)];
                        world.set((int) lastX, y, Global.content.getConstByBlockId(blockId), true);
                    }
                } else {
                    break;
                }
            }
        } while (!(lastX + copySize > world.sizeX));
    }

    private static void generateCaves() {
        int upper = 0;
        int iters = 0;

        int upperX = 100;
        int downedX = 100;

        double caves = world.sizeX / ((Math.random() * 30) + 50);

        for (int b = 0; b < caves; b++) {
            int minRadius = 2;
            int maxRadius = 8;
            int startRadius = Math.max(minRadius, (int) (Math.random() * maxRadius));
            boolean isUpper = Math.random() * 1.4f > 1 || (upper < caves / 6);

            //за 0 градусов принята вертикаль
            if (isUpper) {
                upper++;
                iters += generateCave(upperX, findTopmostSolidBlock(upperX, 5), startRadius, minRadius, maxRadius - 2, 100, 260, (int) ((Math.random() * 130) + 40), 40, 200);
                upperX += (int) ((Math.random() * (world.sizeX / (caves / 2))) + (world.sizeX / (caves / 4)));
            } else {
                iters += generateCave(downedX, (int) (findTopmostSolidBlock(downedX, 3) - Math.random() * (world.sizeY / 2.4f)), startRadius, minRadius, maxRadius, 80, 280, (int) (Math.random() * 360), 40, 240);
                downedX += (int) ((Math.random() * (world.sizeX / (caves / 2))) + (world.sizeX / (caves / 4)));
            }

            //магическое число после которого пещеры постепенно превращаются в кашу
            if (iters > 70000) {
                //break;
            }
        }
        System.out.println("Caves: " + caves + " | " + iters);
        clearFloatingIslands(world.tiles, world.sizeX, world.sizeY, 50);
    }

    private static int generateCave(float x, float y, float radius, int minRadius, int maxRadius, int minAngle, int maxAngle, int startAngle, int maxAngleChange, int shotChance) {
        if (minRadius < 1 || minRadius == maxRadius) {
            return 0;
        }

        float angle = startAngle;
        int totalIters = 0;

        do {
            maxAngleChange = Math.clamp((int) ((y / (float) world.sizeY) * 80), 10, 50);

            if (Math.random() * 25 < 1 || radius > maxRadius) {
                radius = (int) Math.clamp(radius + (Math.random() * 2) - 1, minRadius, maxRadius);
            }
            float iters = (int) (Math.random() * 5);
            angle = (float) Math.clamp(angle + ((Math.random() * (maxAngleChange * 2)) - maxAngleChange), minAngle, maxAngle);

            float deltaY = (float) (Math.cos(Math.toRadians(angle)));
            float deltaX = (float) (Math.sin(Math.toRadians(angle)));

            for (int j = 0; j < iters; j++) {
                totalIters += iters;

                y += deltaY;
                x += deltaX;

                destroyAround(x, y, radius);

                //пещера со случайным направлением
                if (Math.random() * shotChance < 1) {
                    shotChance *= (int) ((Math.random() * 0.8) + 2);

                    int sAngle = (int) ((angle + ((Math.random() * 90) - 45)) % 360);
                    generateCave(x, y, radius - 1, minRadius, (int) radius, sAngle - 50, sAngle + 50, sAngle, 40, (int) Math.min(1200, shotChance));
                    continue;
                }

                //вправо влево
                if (Math.random() * (shotChance * 1.2f) < 1) {
                    shotChance *= (int) ((Math.random() * 0.8) + 2);

                    int sAngle = (int) ((angle + ((Math.random() * 90) - 45)) % 360);
                    boolean left = Math.random() * 2 < 1;

                    generateCave(x, y, radius, minRadius, (int) radius, left ? 265 : 20, left ? 340 : 95, sAngle, 40, (int) Math.min(1500, shotChance));
                }
            }

        } while (world.inBounds((short) x, (short) (y - (Math.random() * (world.sizeY / 15f)))) && totalIters < 5000 && Math.random() * world.sizeY > totalIters / 200f);

        return totalIters;
    }

    private static void destroyAround(float x, float y, float radius) {
        for (int i = (int) (x - radius); i <= x + radius; i++) {
            for (int k = (int) (y - radius); k <= y + radius; k++) {
                float dx = i - x;
                float dy = k - y;

                if (i > 0 && k > 0 && (dx * dx + dy * dy <= radius * radius)) {
                    world.destroy(i, k);
                }
            }
        }
    }

    //возможно плохо работает
    public static void clearFloatingIslands(short[] tiles, int sizeX, int sizeY, int minSize) {
        boolean[] visited = new boolean[tiles.length];

        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                int index = x + sizeX * y;

                if (tiles[index] != 0 && !visited[index]) {
                    List<Integer> islandIndices = new ArrayList<>();
                    findIsland(tiles, x, y, sizeX, sizeY, visited, islandIndices);

                    if (islandIndices.size() < minSize) {
                        for (int idx : islandIndices) {
                            int px = idx % sizeX;
                            int py = idx / sizeX;
                            world.destroy(px, py);
                        }
                    }
                }
            }
        }
    }

    private static void findIsland(short[] tiles, int startX, int startY, int sizeX, int sizeY, boolean[] visited, List<Integer> islandIndices) {
        Queue<Integer> queue = new LinkedList<>();

        int startIdx = startX + sizeX * startY;
        queue.add(startIdx);
        visited[startIdx] = true;

        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

        while (!queue.isEmpty()) {
            int currentIdx = queue.poll();
            islandIndices.add(currentIdx);

            int cx = currentIdx % sizeX;
            int cy = currentIdx / sizeX;

            for (int[] dir : directions) {
                int nx = cx + dir[0];
                int ny = cy + dir[1];

                if (nx >= 0 && nx < sizeX && ny >= 0 && ny < sizeY) {
                    int nextIdx = nx + sizeX * ny;

                    if (tiles[nextIdx] != 0 && !visited[nextIdx]) {
                        visited[nextIdx] = true;
                        queue.add(nextIdx);
                    }
                }
            }
        }
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
        var smallStone = Global.content.getConstById("smallStone");
        float chance = 40;

        for (int x = 0; x < world.sizeX; x++) {
            if (Math.random() * chance < 1) {
                int y = findFreeVerticalCell(x);
                if (y - 1 > 0) {
                    var block = world.getBlock(x, y - 1);
                    if (block != null && block.type == Types.SOLID && block.resistance >= 100) {
                        world.set(x, y, smallStone, false);
                    }
                }
            }
        }
    }

    private static void generateHerb(World world) {
        generateForest(10, 1, 20, 1, 0, "herb");
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
                        StaticObjectsConst object = Global.content.getConstById(name);
                        world.set(xStruct, yStruct, object, true);
                    }
                }
            }
        }
    }

    //todo оно обязательно заработает..
    private static boolean checkInterInsideSolid(int xCell, int yCell, String structName) {
        Structures structure = Structures.getStructure(structName);
        if (structure == null) {
            return false;
        }
//        StaticObjectsConst_V2[][] objects = Structures.bindStructures(structure.blocks);
//
//        for (int x = xCell; x < xCell + objects.length; x++) {
//            for (int y = yCell; y < yCell + objects[0].length; y++) {
//                if (x > 0 && y > 0 && x < world.sizeX && y < world.sizeY) {
//                    if (world.getBlock(x, y).type == Types.SOLID && objects[x - xCell][y - yCell].type == Types.SOLID) {
//                        return true;
//                    }
//                }
//            }
//        }
        return false;
    }

    private static boolean checkInterInsideSolid(int xCell, int yCell, StaticObjectsConst[][] blocks) {
        for (int x = xCell; x < xCell + blocks.length; x++) {
            for (int y = yCell; y < yCell + blocks[0].length; y++) {
                if (x > 0 && y > 0 && x < world.sizeX && y < world.sizeY) {
                    var block = world.getBlock(x, y);

                    if (block != null && block.type == Types.SOLID && blocks[x - xCell][y - yCell].type == Types.SOLID) {
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
            StaticObjectsConst block = world.getBlock(x, y);

            if (block == null || block.type == Types.GAS) {
                return y;
            }
        }
        return -1;
    }

    private static void generateResources() {
        //вынес для удобства
        StaticObjectsConst obj = Global.content.getConstById("aluminium");

        for (int i = 0; i < Math.random() * ((world.sizeX + world.sizeY) / 100f); i++) {

            boolean[][] noise = PerlinNoiseGenerator.createBoolNoise((int) (Math.random() * 30), (int) (Math.random() * 30), 1.3f);
            Point2i randPos = randAtGround();

            for (int x = 0; x < noise.length; x++) {
                for (int y = 0; y < noise[0].length; y++) {
                    if (noise[x][y] && world.getBlock(x + randPos.x, y + randPos.y).type == Types.SOLID) {
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
            var block = world.getBlock(cellX, y);

            if (block != null && block.type == Types.SOLID) {
                for (int i = y; i < y + period; i++) {
                    if (world.getBlock(cellX, i + 1).type == Types.GAS && world.getBlock(cellX, i).type == Types.SOLID) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private static void startGame(PlayGameScene playGameScene) {
        //todo а починить
//        world.registerListener(new WorkbenchLogic());
//        world.registerListener(new Factories());
//        world.registerListener(new Chests());
//        Inventory.registerListener(new ElectricCables());
//        Inventory.registerListener(new Factories());
//        Inventory.registerListener(new Chests());
//
//        Inventory.create();

        EventHandler.setDebugValue(() -> {
            if (DynamicObjects.isEmpty()) {
                return null;
            }
            var player = DynamicObjects.getFirst();
            return "[Player] x: " + player.getX() + ", y: " + player.getY();
        });

        gameScene.onPreloadCompletion(() -> {
            UIMenus.createPlanet().hide();

            setGameScene(playGameScene);
            gameState = GameState.PLAYING;
        });
    }

    private static Point2i randAtGround() {
        int randX = (int) (Math.random() * world.sizeX);

        for (int i = world.sizeY; i > 0; i--) {
            if (world.getBlock(randX, i).type == Types.SOLID) {
                return new Point2i(randX, (int) (Math.random() * world.sizeY - i) + i);
            }
        }
        return null;
    }

    public static void saveWorldImage(short[] tiles, int sizeX, int sizeY) {
        if (Config.getFromConfigInt("Debug") >= 2) {
            BufferedImage image = new BufferedImage(sizeX, sizeY, BufferedImage.TYPE_INT_RGB);
            Path path = assets.assetsDir().resolve("worldImage.png");

            for (int y = 0; y < sizeY; y++) {
                for (int x = 0; x < sizeX; x++) {
                    short block = tiles[x + sizeX * y];

                    if (block != 0) {
                        image.setRGB(x, (sizeY - 1) - y, 0xFFFFFF);
                    } else {
                        image.setRGB(x, (sizeY - 1) - y, 0x000000);
                    }
                }
            }

            try {
                File outputFile = new File(path.toUri());
                if (outputFile.getParentFile() != null) {
                    outputFile.getParentFile().mkdirs();
                }
                ImageIO.write(image, "png", outputFile);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }
}
