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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static core.Global.*;
import static core.World.World.findTopmostSolidBlock;
import static core.World.WorldGenerator.WorldGeneratorConstants.*;
import static core.WorldCoordinates.toBlock;

public class WorldGeneratorTMP {
    private static final Logger log = LogManager.getLogger("WorldGen");

    public static void generateWorld(CreatePlanet.GenerationParameters params) {
        int sizeX = params.sizeX;
        int sizeY = params.sizeY;
        World world = new World(
                new World.Meta(sizeX, sizeY, params.seed, null, params.description,
                        System.currentTimeMillis() / 1000, 0));
        entityPool.worldIndex().bounds.set(0, 0, sizeX, sizeY);
        Global.world = world;

        boolean simple = params.simple;

        log("version: 2.43 unstable");
        log("World generator: starting generating world with size: " + world.sizeX + "x" + world.sizeY);

        var playGameScene = new PlayGameScene();
        gameScene.addPreload(playGameScene);

        long totalStartTime = System.currentTimeMillis();

        if (!simple) {
            CompletableFuture.runAsync(
                            timedRun("generating relief",
                            () -> generateRelief(world)))
                    .thenCompose(
                            timedCompose("generating environment",
                            () -> generateEnvironments(world)))
                    .thenRun(
                            timedRun("generating caves",
                            () -> generateCaves()))
                    .thenRun(
                            timedRun("generating: copy",
                            () -> copy()))
                    .thenCompose(
                            timedCompose("regenerating shadow map",
                            () -> ShadowMap.generate()))
                    .thenRun(
                            timedRun("generating temperature map",
                            () -> TemperatureMap.create()))
                    .thenRun(
                            timedRun("generating player",
                             () -> player = WorldUtils.spawn(content.creatureById("player"), true)))
                    .thenRun(() -> {
                        log("generating done! Total time: " + (System.currentTimeMillis() - totalStartTime) + "ms");
                        Debug.saveWorldImage();
                        scheduler.post(() -> startGame(playGameScene));
                    })
                    .whenComplete((__, e) -> {
                        if (e != null) {
                            log.error("Failed to generate world", e);
                        }
                    });
        } else {
            CompletableFuture.runAsync(timedRun("generating flat world", () -> generateFlatWorld(world)))
                    .thenRun(
                            timedRun("regenerating shadow map",
                            () -> ShadowMap.generate()))
                    .thenRun(
                            timedRun("generating temperature map",
                            () -> TemperatureMap.create()))
                    .thenRun(
                            timedRun("generating player",
                            () -> Global.player = WorldUtils.spawn(content.creatureById("player"), true)))
                    .thenRun(() -> {
                        log("generating done! Total time: " + (System.currentTimeMillis() - totalStartTime) + "ms");
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

    private static Runnable timedRun(String stepName, Runnable task) {
        return () -> {
            long stepStart = System.currentTimeMillis();
            task.run();
            log(stepName + " took: " + (System.currentTimeMillis() - stepStart) + "ms");
        };
    }

    private static <T> Function<T, CompletableFuture<Void>> timedCompose(String stepName, Supplier<CompletableFuture<Void>> taskSupplier) {
        return __ -> {
            long stepStart = System.currentTimeMillis();
            return taskSupplier.get().thenRun(() -> {
                log(stepName + " took: " + (System.currentTimeMillis() - stepStart) + "ms");
            });
        };
    }

    private static void log(String text) {
        log.info(text);
        scheduler.post(() -> UIMenus.createPlanet().appendText(text), Time.ONE_SECOND);
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

    //todo оптимально раскидать на динамическую генерацию, но так лень этим заниматься
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
        float lastY = world.sizeY / 2;
        float angle = RELIEF_START_ANGLE;

        int upperBorder = currentBiomes.getUpperBorder();
        int bottomBorder = currentBiomes.getBottomBorder();
        int blockGradient = currentBiomes.getBlockGradientChance();
        int lastSwapBiomes = 0;
        final int minSwapBiomes = MIN_SWAP_BIOMES;

        var rnd = ThreadLocalRandom.current();
        do {
            angle = Math.clamp(
                    angle + (rnd.nextFloat() * blockGradient - blockGradient / 2),
                    Math.clamp(upperBorder + (lastY - world.sizeY / 2), upperBorder, RELIEF_BASE_ANGLE),
                    Math.clamp(bottomBorder - (world.sizeY / 2 - lastY), RELIEF_BASE_ANGLE, bottomBorder)
            );

            int iters = (int) (rnd.nextFloat() * RELIEF_ITERS_MULTIPLIER / (RELIEF_BASE_ANGLE - Math.abs(RELIEF_BASE_ANGLE - angle)));
            float deltaX = (float) (Math.sin(Math.toRadians(angle)));
            float deltaY = (float) (Math.cos(Math.toRadians(angle)));

            int currentIntX = (int) lastX;

            for (int j = 0; j < iters; j++) {
                lastY += deltaY;
                lastX += deltaX;

                if ((int) lastX == currentIntX) {
                    continue;
                }
                currentIntX = (int) lastX;

                if (currentIntX < worldWidth && lastY > 0) {
                    worldXBiomes[currentIntX] = currentBiomes;
                    worldHeights[currentIntX] = lastY;
                    lastSwapBiomes++;

                    if (lastSwapBiomes > minSwapBiomes && rnd.nextFloat() * lastSwapBiomes - minSwapBiomes > BIOME_SWAP_MULTIPLIER) {
                        lastBiomes = currentBiomes;
                        currentBiomes = Biomes.getRand();
                        lastSwapBiomes = 0;
                        upperBorder = currentBiomes.getUpperBorder();
                        bottomBorder = currentBiomes.getBottomBorder();
                        blockGradient = currentBiomes.getBlockGradientChance();
                    }

                    if (lastSwapBiomes < BIOME_SWAP_MAX_THRESHOLD && rnd.nextFloat() * lastSwapBiomes < BIOME_SWAP_CHANCE) {
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
        float delta = DO_IT_AGAIN_DELTA;
        float delt = worldHeights[0] - lastY;
        float angle = (float) Math.toDegrees(Math.atan2(delta, delt));
        float deltaX = MathUtil.sin(Math.toRadians(angle));
        float deltaY = MathUtil.cos(Math.toRadians(angle));

        do {
            for (int j = 0; j < DO_IT_AGAIN_ITERS; j++) {
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
        int upperX = CAVES_INITIAL_X;
        int downedX = CAVES_INITIAL_X;
        var rnd = ThreadLocalRandom.current();
        int caves = (int) (world.sizeX / ((rnd.nextFloat() * CAVES_COUNT_RAND_MULT) + CAVES_COUNT_BASE));

        for (int b = 0; b < caves; b++) {
            int minRadius = CAVES_MIN_RADIUS;
            int maxRadius = CAVES_MAX_RADIUS;
            int startRadius = Math.max(minRadius, rnd.nextInt(maxRadius));
            boolean isUpper = rnd.nextFloat() * CAVES_UPPER_CHANCE > 1 || (upper < caves / CAVES_UPPER_DIVISOR);

            if (isUpper) {
                upper++;
                //пещеры с выходом на поверхность
                generateCave(upperX, findTopmostSolidBlock(upperX, 3),
                        startRadius, minRadius, maxRadius - 2, CAVE_UPPER_MIN_ANGLE, CAVE_UPPER_MAX_ANGLE, rnd.nextInt(CAVE_UPPER_START_MIN, CAVE_UPPER_START_MAX), CAVE_UPPER_SHOT_CHANCE);
                upperX += (int) ((rnd.nextFloat() * (world.sizeX / (caves / CAVES_X_DIVISOR_UPPER))) + (world.sizeX / (caves / CAVES_X_DIVISOR_UPPER)));
            } else {
                //пещеры в глубине
                generateCave(downedX, (int) (findTopmostSolidBlock(downedX, 3) - rnd.nextFloat() * (world.sizeY / CAVES_DOWNED_Y_DIVISOR)),
                        startRadius, minRadius, maxRadius, CAVE_DOWNED_MIN_ANGLE, CAVE_DOWNED_MAX_ANGLE, rnd.nextInt(CAVE_DOWNED_START_ANGLE), CAVE_DOWNED_SHOT_CHANCE);
                downedX += (int) ((rnd.nextFloat() * (world.sizeX / (caves / CAVES_X_DIVISOR_DOWNED))) + (world.sizeX / (caves / (CAVES_X_DIVISOR_DOWNED * 2))));
            }
        }
        log.debug("spawned {} caves", caves);

        clearFloatingIslands(world.tiles, world.sizeX, world.sizeY, ISLAND_MAXSIZE_CLEAR_SIZE);
    }

    private static int generateCave(int bx, int by,
                                    float radius, int minRadius, int maxRadius,
                                    int minAngle, int maxAngle, int startAngle,
                                    float shotChance) {
        if (minRadius < 1 || minRadius == maxRadius) {
            return 0;
        }

        float angle = startAngle;
        int totalIters = 0;

        var rnd = ThreadLocalRandom.current();
        float wx = bx;
        float wy = by;
        int lastShot = 0;

        do {
            int maxAngleChange = Math.clamp((int) ((wy / (float) world.sizeY) * CAVE_MAX_ANGLE_MULT), CAVE_MAX_ANGLE_MIN, CAVE_MAX_ANGLE_MAX);
            if (rnd.nextInt(CAVE_RADIUS_CHANCE) < 1 || radius > maxRadius) {
                radius = (int) Math.clamp(radius + rnd.nextFloat(CAVE_RADIUS_MIN, CAVE_RADIUS_MAX), minRadius, maxRadius);
            }

            int iters = rnd.nextInt(CAVE_ITERS_BOUND);
            angle = Math.clamp(angle + rnd.nextFloat(-maxAngleChange, maxAngleChange), minAngle, maxAngle);
            float deltaY = MathUtil.cos(Math.toRadians(angle));
            float deltaX = MathUtil.sin(Math.toRadians(angle));

            int lastBlockX = Integer.MIN_VALUE;
            int lastBlockY = Integer.MIN_VALUE;
            lastShot += iters;

            for (int j = 0; j < iters; j++) {
                //просьба не вытаскивать тоталитерс отсюда
                //он в своей естественной среде обитания
                totalIters += iters;
                wx += deltaX;
                wy += deltaY;

                int currentBlockX = toBlock(wx);
                int currentBlockY = toBlock(wy);

                if (currentBlockX == lastBlockX && currentBlockY == lastBlockY) {
                    continue;
                }
                lastBlockX = currentBlockX;
                lastBlockY = currentBlockY;

                destroyAround(currentBlockX, currentBlockY, toBlock(radius));

                //todo тут потенциальное место для проверки глубины пещеры, чтоб на поверхности не бывало каши
                //пещера в рандомном направлении
                if (rnd.nextFloat() * (shotChance * CAVES_EVERY_CHANCE) < 1 && lastShot > CAVE_EVERY_MIN_ITERS) {
                    shotChance *= rnd.nextFloat(CAVE_SHOT_MULT_MIN, CAVE_SHOT_MULT_MAX);
                    int sAngle = (int) ((angle + rnd.nextFloat(CAVE_SHOT_ANGLE_MIN, CAVE_SHOT_ANGLE_MAX)) % 360);

                    generateCave(toBlock(wx), toBlock(wy), radius - 1, minRadius, (int) radius, sAngle - CAVE_EVERY_ANGLE_OFFSET, sAngle + CAVE_EVERY_ANGLE_OFFSET, sAngle, Math.min(CAVE_EVERY_CHANCE_SHOT_MAX, shotChance));
                    lastShot = 0;
                }

                //пещера влево вправо
                if (rnd.nextFloat() * (shotChance * CAVES_LR_CHANCE) < 1 && lastShot > CAVE_LR_MIN_ITERS) {
                    shotChance *= (int) (rnd.nextFloat(CAVE_SHOT_MULT_MIN, CAVE_SHOT_MULT_MAX));

                    int sAngle = (int) ((angle + rnd.nextFloat(CAVE_SHOT_ANGLE_MIN, CAVE_SHOT_ANGLE_MAX)) % 360);
                    boolean left = rnd.nextBoolean();

                    generateCave(toBlock(wx), toBlock(wy), radius, minRadius, (int) radius, left ? CAVE_BRANCH_LEFT_MIN_ANGLE : CAVE_BRANCH_RIGHT_MIN_ANGLE, left ? CAVE_BRANCH_LEFT_MAX_ANGLE : CAVE_BRANCH_RIGHT_MAX_ANGLE, sAngle, Math.min(CAVE_LR_CHANCE_SHOT_MAX, shotChance));
                    lastShot = 0;
                }
            }
        } while (world.inBounds(toBlock(wx), toBlock(wy - (rnd.nextFloat() * (world.sizeY / CAVE_Y_BOUND_DIVISOR)))) &&
                totalIters < CAVE_TOTAL_ITERS_MAX && rnd.nextFloat() * world.sizeY > totalIters / CAVE_TOTAL_ITERS_DIVISOR);

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
            {0, 1}, {0, -1},
            {1, 0}, {-1, 0}
    };

    //какая то сложнючая но быстрая дичь которую наверняка можно сделать лучше/красивее
    public static void clearFloatingIslands(short[] tiles, int sizeX, int sizeY, int minSize) {
        int totalColumns = sizeX;
        int cores = Runtime.getRuntime().availableProcessors();
        int chunkSize = (totalColumns / cores) + 1;

        world.genPool.submit(() -> {
            IntStream.range(0, cores).parallel().forEach(p -> {
                int startChunkX = p * chunkSize;
                int endChunkX = Math.min(sizeX, startChunkX + chunkSize);

                IntArrayList localStack = new IntArrayList(LOCAL_STACK_CAPACITY);
                int[] islandBuffer = new int[minSize];

                for (int y = 0; y < sizeY; y++) {
                    for (int x = startChunkX; x < endChunkX; x++) {
                        int globalIndex = x + sizeX * y;

                        short tile = tiles[globalIndex];
                        if (tile != 0 && (tile & 0x8000) == 0) {
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

                            tiles[globalIndex] |= 0x8000;

                            if (!hasNeighbors) {
                                tiles[globalIndex] = 0;
                                world.destroy(x, y);
                                continue;
                            }

                            int islandSize = 0;
                            localStack.clear();
                            localStack.add(globalIndex);
                            boolean touchesBorder = false;

                            while (!localStack.isEmpty()) {
                                int currentIdx = localStack.removeInt(localStack.size() - 1);

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
                                        short nextTile = tiles[nextIdx];

                                        if (nextTile != 0 && (nextTile & 0x8000) == 0) {
                                            tiles[nextIdx] |= 0x8000;
                                            localStack.add(nextIdx);
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

            IntStream.range(0, cores).parallel().forEach(p -> {
                int startChunkX = p * chunkSize;
                int endChunkX = Math.min(sizeX, startChunkX + chunkSize);
                for (int y = 0; y < sizeY; y++) {
                    for (int x = startChunkX; x < endChunkX; x++) {
                        int globalIndex = x + sizeX * y;
                        if (tiles[globalIndex] != 0) {
                            tiles[globalIndex] &= 0x7FFF;
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
        float chance = DECOR_STONE_SPAWN_CHANCE;

        for (int x = 0; x < world.sizeX; x++) {
            if (Math.random() * chance < 1) {
                int y = findTopmostSolidBlock(x, 3);
                if (y - 1 > 0) {
                    var block = world.getBlock(x, y - 1);
                    if (block != null && block.type == Type.SOLID) {
                        world.set(x, y, smallStone, false);
                    }
                }
            }
        }
    }

    private static void generateHerb(World world) {
        generateForest(HERB_SPAWN_CHANCE, HERB_MIN_FOREST_SIZE, HERB_MAX_FOREST_SIZE, HERB_MIN_SPAWN_DIST, HERB_MAX_SPAWN_DIST, "herb");
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
                lastForestSize = (float) ((forests[x] * Math.random() * FOREST_SIZE_MULT) + FOREST_SIZE_OFFSET);
            }
        }

        //second stage - plants structures by seeds
        for (int x = 0; x < forests.length; x++) {
            if (forests[x] > 0) {
                for (int i = 0; i < forests[x]; i++) {
                    String name = structuresName[(int) (Math.random() * structuresName.length)];
                    int distance = (int) (Math.random() * (maxSpawnDistance - minSpawnDistance)) + minSpawnDistance;
                    int xStruct = x + (i * distance);
                    int yStruct = findTopmostSolidBlock(x + (i * distance), 3);

                    if (xStruct > 0 && yStruct > 0 && xStruct < forests.length) {
                        Block object = Global.content.blockById(name);
                        world.set(xStruct, yStruct, object, true);
                    }
                }
            }
        }
    }

    private static void generateResources() {
        //вынес для удобства
        Block obj = Global.content.blockById("aluminium");

        for (int i = 0; i < Math.random() * ((world.sizeX + world.sizeY) / RESOURCES_ITER_DIVISOR); i++) {
            boolean[][] noise = PerlinNoiseGenerator.createBoolNoise((int) (Math.random() * RESOURCES_NOISE_BOUND), (int) (Math.random() * RESOURCES_NOISE_BOUND), RESOURCES_NOISE_SCALE);
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