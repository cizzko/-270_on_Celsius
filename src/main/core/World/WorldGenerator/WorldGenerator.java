package core.World.WorldGenerator;

import core.*;
import core.UI.menu.CreatePlanet;
import core.World.PerlinNoiseGenerator;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.StaticObjectsConst.Type;
import core.World.StaticWorldObjects.Structures.Structures;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.Textures.ShadowMap;
import core.World.Textures.TextureDrawing;
import core.World.World;
import core.World.WorldUtils;
import core.math.Point2i;
import core.util.DebugTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static core.Global.*;

public class WorldGenerator {
    private static final Logger log = LogManager.getLogger("WorldGen");

    public static float intersDamageMultiplier = 40f, minVectorIntersDamage = 1.8f;
    public static final int copySize = 50;

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
        entityPool.worldIndex().bounds.set(0,0, SizeX*TextureDrawing.blockSize, SizeY*TextureDrawing.blockSize);
        Global.world = world;

        boolean simple = params.simple;
        boolean randomSpawn = params.randomSpawn;
        boolean creatures = params.creatures;

        log("version: 2.2");
        log("World generator: starting generating world with size: " + world.sizeX + "x" + world.sizeY);

        var playGameScene = new PlayGameScene();

        gameScene.addPreload(playGameScene);

        log("generating relief " + (System.currentTimeMillis() - startTime) + "ms");
        CompletableFuture.runAsync(() -> generateRelief(world))
                .thenCompose(__ -> {
                    log("generating environment " + (System.currentTimeMillis() - startTime) + "ms");
                    return generateEnvironments(world);
                })
                .thenCompose(__ -> {
                    log("generating caves " + (System.currentTimeMillis() - startTime) + "ms");
                    return generateCaves();
                })
                .thenRun(() -> {
                    log("generating: copy " + (System.currentTimeMillis() - startTime) + "ms");
                    copy();
                })
                .thenRun(() -> {
                    log("regenerating shadow map " + (System.currentTimeMillis() - startTime) + "ms");
                    ShadowMap.generate();
                })
                .thenRun(() -> {
                    log("generating temperature map " + (System.currentTimeMillis() - startTime) + "ms");
                    TemperatureMap.create();
                })
                .thenRun(() -> {
                    log("generating player " + (System.currentTimeMillis() - startTime) + "ms");
                    Global.player = WorldUtils.spawn(content.creatureById("player"), true);
                })
                .thenRun(() -> {
                    log("generating done! " + (System.currentTimeMillis() - startTime) + "ms");
                    DebugTools.saveWorldImage();
                    scheduler.post(() -> startGame(playGameScene), Time.ONE_SECOND);
                })
                .whenComplete((__, e) -> {
                    if (e != null) {
                        log.error("Failed to generate world", e);
                    }
                });
    }

    private static void log(String text) {
        log.info(text);
        scheduler.post(() -> UIMenus.createPlanet().appendText(text), 0.5f * Time.ONE_SECOND);
    }

    private static void copy() {
        int height = world.sizeY;
        int width = world.sizeX;

        for (int x = 0; x < copySize; x++) {
            for (int y = 0; y < height; y++) {
                var obj = world.getBlock(x, y);
                if (obj != null) {
                    world.copyFromTo(x, y, width - copySize + x, y, obj, false);
                }
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
                            world.set((int) lastX, y, content.blocksRegistry.typeById(lastBiomes.getBlocks()[(int) Math.min(lastBiomes.getBlocks().length - 1, lastY - y)]), false);
                        }
                    } else {
                        for (int y = 0; y < lastY; y++) {
                            world.set((int) lastX, y, content.blocksRegistry.typeById(availableBlocks[(int) Math.min(availableBlocks.length - 1, lastY - y)]), false);
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
                        world.set((int) lastX, y, Global.content.blocksRegistry.typeById(blockId), true);
                    }
                } else {
                    break;
                }
            }
        } while (!(lastX + copySize > world.sizeX));
    }

    private static CompletableFuture<Void> generateCaves() {
        int upper = 0;
        int iters = 0;
        int upperX = 100;
        int downedX = 100;

        int caves = (int) (world.sizeX / ((Math.random() * 30) + 50));

        for (int b = 0; b < caves; b++) {
            int minRadius = 2;
            int maxRadius = 8;
            int startRadius = Math.max(minRadius, (int) (Math.random() * maxRadius));
            boolean isUpper = Math.random() * 1.4f > 1 || (upper < caves / 6);

            //за 0 градусов принята вертикаль
            if (isUpper) {
                upper++;
                iters += generateCave(upperX, findTopmostSolidBlock(upperX, 5), startRadius, minRadius, maxRadius - 2, 100, 260, (int) ((Math.random() * 130) + 40), 40, 200);
                upperX += (int) (((Math.random() * (world.sizeX / (caves / 2f))) + (world.sizeX / (caves / 4f))) + Math.random() * 150);
            } else {
                iters += generateCave(downedX, (int) (findTopmostSolidBlock(downedX, 3) - Math.random() * (world.sizeY / 2.4f)), startRadius, minRadius, maxRadius, 80, 280, (int) (Math.random() * 360), 40, 240);
                downedX += (int) (((Math.random() * (world.sizeX / (caves / 2f))) + (world.sizeX / (caves / 4f))) + Math.random() * 150);
            }

            //магическое число после которого пещеры постепенно превращаются в кашу
            if (iters > 70000) {
                //break;
            }
        }
        log.debug("spawned {} caves with {} iters", caves, iters);

        clearFloatingIslands(world.tiles, world.sizeX, world.sizeY, 30);

        return CompletableFuture.completedFuture(null);
    }

    private static int generateCave(float x, float y, float radius, int minRadius, int maxRadius, int minAngle, int maxAngle, int startAngle, int maxAngleChange, int shotChance) {
        if (minRadius < 1 || minRadius == maxRadius) {
            return 0;
        }

        float angle = startAngle;
        int totalIters = 0;

        do {
            //todo
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
        int length = tiles.length;
        boolean[] visited = new boolean[length];
        int[] islandBuffer = new int[minSize + 1];
        int[] stack = new int[length];
        int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {-1, 1}, {1, -1}, {-1, -1}};

        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                int index = x + sizeX * y;

                if (tiles[index] != 0 && !visited[index]) {
                    boolean hasNeighbors = false;

                    for (int[] dir : directions) {
                        int nx = x + dir[0];
                        int ny = y + dir[1];
                        if (nx >= 0 && nx < sizeX && ny >= 0 && ny < sizeY) {
                            if (tiles[nx + sizeX * ny] != 0) {
                                hasNeighbors = true;
                                break;
                            }
                        }
                    }

                    if (!hasNeighbors) {
                        tiles[index] = 0;
                        world.destroy(x, y);
                        visited[index] = true;
                        continue;
                    }

                    int islandSize = 0;
                    int stackPointer = 0;

                    stack[stackPointer++] = index;
                    visited[index] = true;

                    while (stackPointer > 0) {
                        int currentIdx = stack[--stackPointer];
                        if (islandSize < minSize) {
                            islandBuffer[islandSize] = currentIdx;
                        }
                        islandSize++;

                        int cx = currentIdx % sizeX;
                        int cy = currentIdx / sizeX;
                        for (int[] dir : directions) {
                            int nx = cx + dir[0];
                            int ny = cy + dir[1];

                            if (nx >= 0 && nx < sizeX && ny >= 0 && ny < sizeY) {
                                int nextIdx = nx + sizeX * ny;
                                if (tiles[nextIdx] != 0 && !visited[nextIdx]) {
                                    visited[nextIdx] = true;
                                    if (stackPointer < stack.length) {
                                        stack[stackPointer++] = nextIdx;
                                    }
                                }
                            }
                        }
                    }

                    if (islandSize < minSize) {
                        for (int i = 0; i < islandSize; i++) {
                            int idx = islandBuffer[i];
                            int px = idx % sizeX;
                            int py = idx / sizeX;

                            tiles[idx] = 0;
                            world.destroy(px, py);
                        }
                    }
                }
            }
        }
    }


    private static CompletableFuture<Void> generateEnvironments(World world) {
        return CompletableFuture.runAsync(() -> {
            generateTrees(world);
            generateDecorStones(world);
            generateHerb(world);
            Structures.clearStructuresMap();
        });
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
                    if (block != null && block.type == Type.SOLID && block.resistance >= 100) {
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

                    if (block != null && block.type == Type.SOLID && blocks[x - xCell][y - yCell].type == Type.SOLID) {
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

            if (block == null || block.type == Type.GAS) {
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
                    if (noise[x][y] && world.getBlock(x + randPos.x, y + randPos.y).type == Type.SOLID) {
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

            if (block != null && block.type == Type.SOLID) {
                for (int i = y; i < y + period; i++) {
                    if (world.getBlock(cellX, i + 1).type == Type.GAS && world.getBlock(cellX, i).type == Type.SOLID) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private static void startGame(PlayGameScene playGameScene) {

        gameScene.onPreloadCompletion(() -> {
            UIMenus.createPlanet().hide();

            setGameScene(playGameScene);
            gameState = GameState.PLAYING;
        });
    }

    private static Point2i randAtGround() {
        var rnd = ThreadLocalRandom.current();
        int randX = rnd.nextInt(0, world.sizeX);

        for (int i = world.sizeY; i > 0; i--) {
            if (world.getBlock(randX, i).type == Type.SOLID) {
                return new Point2i(randX, rnd.nextInt(i, world.sizeY));
            }
        }
        return null;
    }
}
