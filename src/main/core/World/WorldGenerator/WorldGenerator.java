package core.World.WorldGenerator;

import core.*;
import core.UI.menu.CreatePlanet;
import core.World.PerlinNoiseGenerator;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.World;
import core.World.WorldUtils;
import core.content.blocks.Block;
import core.content.blocks.Block.Type;
import core.graphic.ShadowMap;
import core.math.MathUtil;
import core.math.Point2i;
import core.util.Debug;
import core.util.FixedBitset;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

import static core.Global.*;
import static core.World.World.findSurfaceY;
import static core.World.WorldGenerator.WorldGeneratorConstants.COPY_SIZE;
import static core.World.WorldGenerator.WorldGeneratorConstants.INTERPOLATE_SIZE;
import static core.WorldCoordinates.toBlock;

public class WorldGenerator {
    private static final Logger log = LogManager.getLogger("WorldGen");
    public static boolean useExpGen = false;

    public static void generateWorld(CreatePlanet.GenerationParameters params) {
        if (useExpGen) {
            WorldGeneratorTMP.generateWorld(params);
            return;
        }

        long startTime = System.currentTimeMillis();
        int sizeX = params.sizeX;
        int sizeY = params.sizeY;

        World world = new World(
                new World.Meta(sizeX, sizeY, params.seed, null, params.description,
                        System.currentTimeMillis()/1000, 0));
        Global.world = world;

        boolean simple = params.simple;

        log("version: 2.2");
        log("World generator: starting generating world with size: " + world.sizeX + "x" + world.sizeY);

        var playGameScene = new PlayGameScene();

        gameScene.addPreload(playGameScene);

        log("generating relief " + (System.currentTimeMillis() - startTime) + "ms");
        if (!simple) {
            CompletableFuture.runAsync(() -> generateRelief(world))
                    .thenCompose(__ -> {
                        log("generating environment " + (System.currentTimeMillis() - startTime) + "ms");
                        return generateEnvironments(world);
                    })
                    .thenRun(() -> {
                        log("generating caves " + (System.currentTimeMillis() - startTime) + "ms");
                        generateCaves();
                    })
                    .thenRun(() -> {
                        log("generating: copy " + (System.currentTimeMillis() - startTime) + "ms");
                        copy();
                    })
                    // TODO(Ociz): тени можно продолжать рисовать после спавна
                    //  Skat: отличная идея, особенно если рендер теней будет происходит визуально гладко для игрока
                    //  Самый простой способ: в области видимости прогрузить как есть
                    //  (область маленькая и навряд ли даже потоки нужны будут)
                    //  а остальное уже потом крутить в фоне
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
                        player = WorldUtils.spawn(content.creatureById("player"), true);
                    })
                    .thenRun(() -> {
                        log("generating done! " + (System.currentTimeMillis() - startTime) + "ms");
                        Debug.saveWorldImage();
                        scheduler.post(() -> startGame(playGameScene));
                    })
                    .whenComplete((__, e) -> {
                        if (e != null) {
                            log.error("Failed to generate world", e);
                        }
                    });
        } else {
            CompletableFuture.runAsync(() -> generateFlatWorld(world))
                    .thenRun(() -> {
                        log("regenerating shadow map " + (System.currentTimeMillis() - startTime) + "ms");
                        ShadowMap.generate();
                    })
                    .thenRun(() -> {
                        log("generating temperature map " + (System.currentTimeMillis() - startTime) + "ms");
                        TemperatureMap.create();
                    })
                    .thenRun(() -> {
                        Global.player = WorldUtils.spawn(content.creatureById("player"), true);
                    })
                    .thenRun(() -> {
                        log("generating done! " + (System.currentTimeMillis() - startTime) + "ms");
                        Debug.saveWorldImage();
                        scheduler.post(() -> startGame(playGameScene));
                    })
                    .whenComplete((__, e) -> {
                        if (e != null) {
                            log.error("Failed to generate world", e);
                        }
                    });
        }
    }

    private static void log(String text) {
        log.info(text);
        scheduler.post(() -> UIMenus.createPlanet().appendText(text), 0.5f * Time.ONE_SECOND);
    }

    private static void copy() {
        int height = world.sizeY;
        int width = world.sizeX;

        for (int x = 0; x < COPY_SIZE; x++) {
            for (int y = 0; y < height; y++) {
                var obj = world.getBlock(x, y);
                if (obj != null) {
                    world.copyFromTo(x, y, width - COPY_SIZE + x, y, obj, false);
                }
            }
        }
    }

    private static void generateFlatWorld(World world) {
        Biomes defaultBiome = Biomes.getDefault();
        short[] availableBlocks = defaultBiome.getBlocks();
        int maxBlockIdx = availableBlocks.length - 1;

        int cores = Runtime.getRuntime().availableProcessors();
        int chunkSize = (world.sizeX / cores) + 1;

        world.genPool.submit(() -> {
            IntStream.range(0, cores).parallel().forEach(p -> {
                int startChunkX = p * chunkSize;
                int endChunkX = Math.min(world.sizeX, startChunkX + chunkSize);

                for (int x = startChunkX; x < endChunkX; x++) {
                    world.setBiomes(x, defaultBiome);

                    int endY = world.sizeY / 2;
                    for (int y = 0; y < endY; y++) {
                        int blockId = availableBlocks[Math.min(maxBlockIdx, endY - y - 1)];
                        world.set(x, y, content.blocksRegistry.typeById(blockId), false);
                    }
                }
            });
        }).join();
    }

    private static void generateRelief(World world) {
        int worldWidth = world.sizeX;
        //карта высот
        float[] worldHeights = new float[world.sizeX];
        //карта биомов
        Biomes[] worldXBiomes = new Biomes[world.sizeX];
        //карта смешиваний
        long[] useLastBiomeFlag = FixedBitset.createBitSet(world.sizeX);

        Biomes lastBiomes = Biomes.getDefault();
        Biomes currentBiomes = Biomes.getRand();

        float lastX = 0;
        float lastY = world.sizeY / 2f;
        float angle = 90;

        int upperBorder = currentBiomes.getUpperBorder();
        int bottomBorder = currentBiomes.getBottomBorder();
        int blockGradient = currentBiomes.getBlockGradientChance();
        int lastSwapBiomes = 0;
        //todo динамически
        final int minSwapBiomes = 200;

        var rnd = ThreadLocalRandom.current();
        do {
            angle = Math.clamp(
                    angle + (rnd.nextFloat() * blockGradient - blockGradient / 2f),
                    Math.clamp(upperBorder + (lastY - world.sizeY / 2f), upperBorder, 90),
                    Math.clamp(bottomBorder - (world.sizeY / 2f - lastY), 90, bottomBorder)
            );

            int iters = (int) (rnd.nextFloat() * 150 / (90 - Math.abs(90 - angle)));
            float deltaX = (float) (Math.sin(Math.toRadians(angle)));
            float deltaY = (float) (Math.cos(Math.toRadians(angle)));

            for (int j = 0; j < iters; j++) {
                lastY += deltaY;
                lastX += deltaX;
                int currentIntX = (int) lastX;

                if (currentIntX < worldWidth && lastY > 0) {
                    worldXBiomes[currentIntX] = currentBiomes;
                    worldHeights[currentIntX] = lastY;
                    lastSwapBiomes++;

                    if (lastSwapBiomes > minSwapBiomes && rnd.nextFloat() * lastSwapBiomes - minSwapBiomes > 30) {
                        lastBiomes = currentBiomes;
                        currentBiomes = Biomes.getRand();
                        lastSwapBiomes = 0;
                        upperBorder = currentBiomes.getUpperBorder();
                        bottomBorder = currentBiomes.getBottomBorder();
                        blockGradient = currentBiomes.getBlockGradientChance();
                    }

                    if (lastSwapBiomes < 20 && rnd.nextFloat() * lastSwapBiomes < 5) {
                        FixedBitset.setBit(useLastBiomeFlag, currentIntX);
                    } else {
                        FixedBitset.unsetBit(useLastBiomeFlag, currentIntX);
                    }
                } else {
                    break;
                }
            }
        } while (!(lastX + COPY_SIZE + INTERPOLATE_SIZE > world.sizeX));

        doItAgainToArrays(lastY, worldHeights);

        int cores = Runtime.getRuntime().availableProcessors();
        int chunkSize = (worldWidth / cores) + 1;
        final Biomes finalLastBiomes = lastBiomes;

        world.genPool.submit(() -> {
            IntStream.range(0, cores).parallel().forEach(p -> {
                int startChunkX = p * chunkSize;
                int endChunkX = Math.min(worldWidth, startChunkX + chunkSize);
                Biomes defaultBiome = Biomes.getDefault();

                for (int x = startChunkX; x < endChunkX; x++) {
                    float targetY = worldHeights[x];
                    if (targetY <= 0) {
                        continue;
                    }

                    Biomes blockBiome = worldXBiomes[x];
                    if (blockBiome == null) {
                        blockBiome = defaultBiome;
                    }

                    short[] availableBlocks = blockBiome.getBlocks();
                    world.setBiomes(x, blockBiome);

                    short[] activeBlocksSet = FixedBitset.isSet(useLastBiomeFlag, x) ? finalLastBiomes.getBlocks() : availableBlocks;
                    int maxBlockIdx = activeBlocksSet.length - 1;

                    for (int y = 0; y < targetY; y++) {
                        int blockId = activeBlocksSet[Math.min(maxBlockIdx, (int) targetY - y)];
                        world.set(x, y, content.blocksRegistry.typeById(blockId), false);
                    }
                }
            });
        }).join();
    }

    private static void doItAgainToArrays(float lastY, float[] worldHeights) {
        float lastX = world.sizeX - COPY_SIZE - INTERPOLATE_SIZE;
        float delta = 90;
        float delt = worldHeights[0] - lastY;
        float angle = (float) Math.toDegrees(Math.atan2(delta, delt)); // хех, ладно
        float deltaX = MathUtil.sin(Math.toRadians(angle));
        float deltaY = MathUtil.cos(Math.toRadians(angle));

        do {
            for (int j = 0; j < 90; j++) {
                lastY += deltaY;
                lastX += deltaX;
                int currentIntX = Math.max((int) lastX - 1, 0);
                if (currentIntX < world.sizeX && lastY > 0) {
                    worldHeights[currentIntX] = lastY;
                } else {
                    break;
                }
            }
        } while (!(lastX + COPY_SIZE > world.sizeX));
    }

    private static void generateCaves() {
        int upper = 0;
        int iters = 0;

        int upperX = 100;
        int downedX = 100;
        var rnd = ThreadLocalRandom.current();
        int caves = (int) (world.sizeX / ((rnd.nextFloat() * 30) + 50));

        for (int b = 0; b < caves; b++) {
            int minRadius = 2;
            int maxRadius = 8;
            int startRadius = Math.max(minRadius, rnd.nextInt(maxRadius));
            boolean isUpper = rnd.nextFloat() * 1.4f > 1 || (upper < caves / 6);

            //за 0 градусов принята вертикаль
            if (isUpper) {
                upper++;
                //пещеры с выходом на поверхность
                iters += generateCave(upperX, findSurfaceY(upperX, 5),
                        startRadius, minRadius, maxRadius - 2, 100, 260, rnd.nextInt(40, 170), 200);
                upperX += (int) ((rnd.nextFloat() * (world.sizeX / (caves / 2f))) + (world.sizeX / (caves / 4f)));
            } else {
                //пещеры в глубине
                iters += generateCave(downedX, (int) (findSurfaceY(downedX, 3) - rnd.nextFloat() * (world.sizeY / 2.4f)),
                        startRadius, minRadius, maxRadius, 80, 280, rnd.nextInt(360), 240);
                downedX += (int) ((rnd.nextFloat() * (world.sizeX / (caves / 2f))) + (world.sizeX / (caves / 4f)));
            }
        }
        log.debug("spawned {} caves with {} iters", caves, iters);

        clearFloatingIslands(world.tiles, world.sizeX, world.sizeY, 100);
    }

    private static int generateCave(int bx, int by,
                                    float radius, int minRadius, int maxRadius,
                                    int minAngle, int maxAngle, int startAngle,
                                    int shotChance) {
        if (minRadius < 1 || minRadius == maxRadius) {
            return 0;
        }

        float angle = startAngle;
        int totalIters = 0;

        var rnd = ThreadLocalRandom.current();
        float wx = bx;
        float wy = by;

        do {
            int maxAngleChange = Math.clamp((int) ((wy / (float) world.sizeY) * 80), 10, 50);
            if (rnd.nextInt(25) < 1 || radius > maxRadius) {
                radius = (int) Math.clamp(radius + rnd.nextFloat(-1f, 1f), minRadius, maxRadius);
            }

            int iters = rnd.nextInt(5);
            angle = Math.clamp(angle + rnd.nextFloat(-maxAngleChange, maxAngleChange), minAngle, maxAngle);
            float deltaY = MathUtil.cos(Math.toRadians(angle));
            float deltaX = MathUtil.sin(Math.toRadians(angle));

            for (int j = 0; j < iters; j++) {
                totalIters += iters;
                wx += deltaX;
                wy += deltaY;
                destroyAround(toBlock(wx), toBlock(wy), toBlock(radius));

                //пещера со случайным направлением
                if (rnd.nextFloat() * shotChance < 1) {
                    // TODO равносильно shotChance *= 2
                    // shotChance *= (int) (rnd.nextFloat(2.0f, 2.8f));
                    shotChance *= 2;
                    int sAngle = (int) ((angle + rnd.nextFloat(-45f, 45f)) % 360);

                    generateCave(toBlock(wx), toBlock(wy), radius - 1, minRadius, (int) radius,
                            sAngle - 50, sAngle + 50, sAngle, Math.min(1200, shotChance));
                }

                //вправо влево
                if (rnd.nextFloat() * (shotChance * 1.2f) < 1) {
                    // TODO равносильно shotChance *= 2
                    // shotChance *= (int) (rnd.nextFloat(2f, 2.8f));
                    shotChance *= 2;

                    int sAngle = (int) ((angle + rnd.nextFloat(-45f, 45f)) % 360);
                    boolean left = rnd.nextBoolean();

                    generateCave(toBlock(wx), toBlock(wy), radius, minRadius, (int) radius, left ? 265 : 20, left ? 340 : 95,
                            sAngle, Math.min(1500, shotChance));
                }
            }
        } while (world.inBounds(toBlock(wx), toBlock(wy - (rnd.nextFloat() * (world.sizeY / 15f)))) &&
                totalIters < 5000 && rnd.nextFloat() * world.sizeY > totalIters / 200f);

        return totalIters;
    }

    private static void destroyAround(int bx, int by, int radius) {
        int beginX = bx - radius;
        int beginY = by - radius;
        if (beginX <= 0 || beginY <= 0) {
            return;
        }

        int endX = bx + radius;
        int endY = by + radius;

        for (int y = beginY; y <= endY; y++) {
            for (int x = beginX; x <= endX; x++) {
                float dx = x - bx;
                float dy = y - by;

                if ((dx * dx) + (dy * dy) <= (radius * radius)) {
                    world.destroy(x, y);
                }
            }
        }
    }

    private static final int[][] directions = {
            {0,  1}, {0,  -1},
            {1,  0}, {-1,  0},
            {1,  1}, {-1,  1},
            {1, -1}, {-1, -1}
    };

    public static void clearFloatingIslands(short[] tiles, int sizeX, int sizeY, int minSize) {
        int totalColumns = sizeX;
        int cores = Runtime.getRuntime().availableProcessors();
        int chunkSize = (totalColumns / cores) + 1;

        world.genPool.submit(() -> {
            IntStream.range(0, cores).parallel().forEach(p -> {
                int startChunkX = p * chunkSize;
                int endChunkX = Math.min(sizeX, startChunkX + chunkSize);

                long[] localVisited = FixedBitset.createBitSet(chunkSize * sizeY);
                int[] islandBuffer = new int[minSize + 1];
                int[] localStack = new int[4096];

                for (int y = 0; y < sizeY; y++) {
                    for (int x = startChunkX; x < endChunkX; x++) {
                        int localX = x - startChunkX;
                        int globalIndex = x + sizeX * y;
                        int localIndex = localX + chunkSize * y;

                        if (tiles[globalIndex] != 0 && !FixedBitset.isSet(localVisited, localIndex)) {
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

                            FixedBitset.setBit(localVisited, localIndex);
                            if (!hasNeighbors) {
                                tiles[globalIndex] = 0;
                                world.destroy(x, y);
                                continue;
                            }

                            int islandSize = 0;
                            int stackPointer = 0;
                            localStack[stackPointer++] = globalIndex;

                            boolean touchesBorder = false;

                            while (stackPointer > 0) {
                                int currentIdx = localStack[--stackPointer];
                                if (islandSize < minSize) {
                                    islandBuffer[islandSize] = currentIdx;
                                }
                                islandSize++;

                                int cx = currentIdx % sizeX;
                                int cy = currentIdx / sizeX;

                                if (cx == startChunkX || cx == endChunkX - 1) {
                                    touchesBorder = true;
                                }

                                for (int[] dir : directions) {
                                    int nx = cx + dir[0];
                                    int ny = cy + dir[1];
                                    if (nx >= startChunkX && nx < endChunkX && ny >= 0 && ny < sizeY) {
                                        int nextIdx = nx + sizeX * ny;
                                        int nextLocalIdx = (nx - startChunkX) + chunkSize * ny;
                                        if (tiles[nextIdx] != 0 && !FixedBitset.isSet(localVisited, nextLocalIdx)) {
                                            FixedBitset.setBit(localVisited, nextLocalIdx);
                                            if (stackPointer < localStack.length) {
                                                localStack[stackPointer++] = nextIdx;
                                            }
                                        }
                                    }
                                }
                            }

                            if (islandSize < minSize && !touchesBorder) {
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
            });
        }).join();
    }

    private static CompletableFuture<Void> generateEnvironments(World world) {
        return CompletableFuture.runAsync(() -> {
            generateTrees(world);
            generateDecorStones(world);
            generateHerb(world);
        });
    }

    private static void generateTrees(World world) {
        //todo проверить
        //generateForest(80, 2, 20, 4, 8, "tree0", "tree1");
    }

    private static void generateDecorStones(World world) {
        var smallStone = Global.content.blockById("smallStone");
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
                        Block object = Global.content.blockById(name);
                        world.set(xStruct, yStruct, object, true);
                    }
                }
            }
        }
    }

    private static int findFreeVerticalCell(int x) {
        if (x <= 0 || x >= world.sizeX) {
            return -1;
        }
        for (int y = 0; y < world.sizeY; y++) {
            Block block = world.getBlock(x, y);

            if (block == null || block.type == Type.GAS) {
                return y;
            }
        }
        return -1;
    }

    private static void generateResources() {
        //вынес для удобства
        Block obj = Global.content.blockById("aluminium");

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
