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
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static core.Global.*;
import static core.Global.world;
import static core.PlayGameScene.CAMERA_OFFSET_X;
import static core.PlayGameScene.CAMERA_OFFSET_Y;
import static core.World.World.findSurfaceY;
import static core.World.WorldGenerator.WorldGeneratorConstants.*;
import static core.WorldCoordinates.toBlock;

public class WorldGeneratorTMP {
    private static final Logger log = LogManager.getLogger("WorldGen");

    /**
     * Запускает генерацию мира
     * @param params параметры генерации
     */
    public static void generateWorld(CreatePlanet.GenerationParameters params) {
        int sizeX = params.sizeX;
        int sizeY = params.sizeY;
        World world = new World(
                new World.Meta(sizeX, sizeY, params.seed, null, params.description,
                        System.currentTimeMillis() / 1000, 0));
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
                            () -> generateRelief(world)), world.genPool)
                    .thenRun(
                            timedRun("generating environment",
                            () -> generateEnvironments(world)))
                    .thenRun(
                            timedRun("generating caves",
                            () -> generateCaves()))
                    .thenRun(
                            timedRun("generating: copy",
                            () -> copy()))
                    .thenRun(
                            timedRun("regenerating shadow map",
                            () -> ShadowMap.generate()))
                    .thenRun(
                            timedRun("generating temperature map",
                            () -> TemperatureMap.create()))
                    .thenRun(
                            timedRun("generating player",
                            () -> spawnPlayer()))
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
            CompletableFuture.runAsync(
                            timedRun("generating flat world",
                            () -> generateFlatWorld(world)), world.genPool)
                    .thenRun(
                            timedRun("regenerating shadow map",
                            () -> ShadowMap.generate()))
                    .thenRun(
                            timedRun("generating temperature map",
                            () -> TemperatureMap.create()))
                    .thenRun(
                            timedRun("generating player",
                            () -> spawnPlayer()))
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

    private static void spawnPlayer() {
        Global.player = WorldUtils.spawn(content.creatureById("player"), true);
        camera.position.set(player.x() + CAMERA_OFFSET_X, player.y() + CAMERA_OFFSET_Y);
        camera.update();
    }

    /**
     * Обертка для замера времени куска генерации
     * @param stepName этап
     * @param task таск
     * @return {@code Runnable} с временем
     */
    private static Runnable timedRun(String stepName, Runnable task) {
        return () -> {
            long stepStart = System.currentTimeMillis();
            task.run();
            log(stepName + " took: " + (System.currentTimeMillis() - stepStart) + "ms");
        };
    }

    /**
     * Обертка для замера времени асинхронной таски генерации
     * <p>
     * То же что {@link #timedRun} но для {@code CompletableFuture}.
     * </p>
     * @param stepName этап
     * @param taskSupplier таск
     * @param <T> тип
     * @return фьючр для цепочки
     */
    private static <T> Function<T, CompletableFuture<Void>> timedCompose(String stepName, Supplier<CompletableFuture<Void>> taskSupplier) {
        return __ -> {
            long stepStart = System.currentTimeMillis();
            return taskSupplier.get().thenRun(() -> {
                log(stepName + " took: " + (System.currentTimeMillis() - stepStart) + "ms");
            });
        };
    }

    /**
     * лог в инфо и в консоль
     * @param text сообщение
     */
    private static void log(String text) {
        log.info(text);
        scheduler.post(() -> UIMenus.createPlanet().appendText(text), Time.ONE_SECOND);
    }

    /**
     * Склеивает края мира для бесшовности
     * <p>
     * Отражает левый кусок мира от {@code 0} до {@link WorldGeneratorConstants#COPY_SIZE COPY_SIZE}
     * в промежуток от {@link World#sizeX world.sizeX} - {@link WorldGeneratorConstants#COPY_SIZE COPY_SIZE} до {@link World#sizeX world.sizeX}
     * </p>
     */
    private static void copy() {
        int height = world.sizeY;
        int width = world.sizeX;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < COPY_SIZE; x++) {
                var obj = world.getBlock(x, y);
                if (obj != null) {
                    world.copyFromTo(x, y, width - COPY_SIZE + x, y, obj, false);
                }
            }
        }
    }

    /**
     * Генерирует плоский мир без всего для отладки
     * @param world мир
     */
    private static void generateFlatWorld(World world) {
        Biomes defaultBiome = Biomes.getDefault();
        short[] availableBlocks = defaultBiome.getBlocks();
        int maxBlockIdx = availableBlocks.length - 1;

        int cores = Runtime.getRuntime().availableProcessors();
        int chunkSize = (world.sizeX / cores) + 1;

        IntStream.range(0, cores).parallel().forEach(p -> {
            int startChunkX = p * chunkSize;
            int endChunkX = Math.min(world.sizeX, startChunkX + chunkSize);

            for (int x = startChunkX; x < endChunkX; x++) {
                world.setBiomes(x, defaultBiome);

                int endY = world.sizeY / 2;
                for (int y = 0; y < endY; y++) {
                    short blockId = availableBlocks[Math.min(maxBlockIdx, endY - y - 1)];
                    world.set(x, y, content.blocksRegistry.typeById(blockId), false);
                }
            }
        });
    }

    /**
     * Генерирует рельеф блуждающей точкой (с учетом параметров биома)
     * <p>
     * <ul>
     * <li>Угол наклона случайно меняется, но ограничен в рамках текущего биома</li>
     * <li>Чем больше отклонение {@code angle} от {@code 90}, тем меньше он может продолжаться (защита от пилообразного мира)</li>
     * </ul>
     * После прохождения {@link WorldGeneratorConstants#MIN_SWAP_BIOMES MIN_SWAP_BIOMES} биом меняется на {@link Biomes#getRand()},
     * В конце вызывается {@link #doItAgainToArrays} для сглаживания высот
     * </p>
     * @param world мир
     */

    private static void generateRelief(World world) {
        int worldWidth = world.sizeX;
        //карта высот
        float[] worldHeights = new float[worldWidth];
        //карта биомов
        Biomes[] worldXBiomes = new Biomes[worldWidth];
        //карта смешиваний
        Biomes[] blendBiomes = new Biomes[worldWidth];

        Biomes lastBiomes = Biomes.getDefault();
        Biomes currentBiomes = Biomes.getRand();

        float lastX = 0;
        float lastY = world.sizeY / 2f;
        float angle = RELIEF_START_ANGLE;

        int upperBorder = currentBiomes.getUpperBorder();
        int bottomBorder = currentBiomes.getBottomBorder();
        int blockGradient = currentBiomes.getBlockGradientChance();
        int lastSwapBiomes = 0;
        final int minSwapBiomes = MIN_SWAP_BIOMES;

        var rnd = ThreadLocalRandom.current();
        do {
            angle = Math.clamp(
                    angle + (rnd.nextFloat() * blockGradient - blockGradient / 2f),
                    Math.clamp(upperBorder + (lastY - world.sizeY / 2f), upperBorder, RELIEF_BASE_ANGLE),
                    Math.clamp(bottomBorder - (world.sizeY / 2f - lastY), RELIEF_BASE_ANGLE, bottomBorder)
            );

            float denominator = RELIEF_BASE_ANGLE - Math.abs(RELIEF_BASE_ANGLE - angle);
            //чтоб не было приколов если угол вылетит 0
            if (denominator == 0) {
                denominator = 0.01f;
            }

            //todo RELIEF_ITERS_MULTIPLIER динамически
            int iters = (int) (rnd.nextFloat() * RELIEF_ITERS_MULTIPLIER / denominator);

            double rad = Math.toRadians(angle);
            float deltaX = (float) Math.sin(rad);
            float deltaY = (float) Math.cos(rad);
            int currentIntX = -1;

            for (int j = 0; j < iters; j++) {
                lastY += deltaY;
                lastX += deltaX;

                int intX = (int) lastX;
                if (intX == currentIntX) {
                    continue;
                }
                currentIntX = intX;

                if (currentIntX >= 0 && currentIntX < worldWidth && lastY > 0) {
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
                        blendBiomes[currentIntX] = lastBiomes;
                    }
                } else if (currentIntX > worldWidth) {
                    break;
                }
            }
        } while (lastX + COPY_SIZE + INTERPOLATE_SIZE < world.sizeX);

        doItAgainToArrays(lastY, worldHeights);

        int cores = Runtime.getRuntime().availableProcessors();
        int chunkSize = (worldWidth / cores) + 1;

        IntStream.range(0, cores).parallel().forEach(p -> {
            int startChunkX = p * chunkSize;
            int endChunkX = Math.min(worldWidth, startChunkX + chunkSize);
            Biomes defaultBiome = Biomes.getDefault();

            for (int x = startChunkX; x < endChunkX; x++) {
                float targetY = worldHeights[x];

                Biomes blockBiome = worldXBiomes[x];
                if (blockBiome == null) {
                    blockBiome = defaultBiome;
                }

                world.setBiomes(x, blockBiome);

                //плавное смешивание биома
                Biomes blend = blendBiomes[x];
                short[] activeBlocksSet = (blend != null) ? blend.getBlocks() : blockBiome.getBlocks();
                int maxBlockIdx = activeBlocksSet.length - 1;

                //зона перехода (когда уникальные блоки биома кончились)
                int transitionZone = (int) (targetY - maxBlockIdx);
                //ставит блоки биома (напр: слой травы, грязи, итд)
                if (transitionZone > 0) {
                    var fillerBlock = content.blocksRegistry.typeById(activeBlocksSet[maxBlockIdx]);
                    for (int y = 0; y < transitionZone; y++) {
                        world.set(x, y, fillerBlock, false);
                    }
                }

                //просто полоска камня вниз когда блоки кончились
                int startSurfaceY = Math.max(0, transitionZone);
                for (int y = startSurfaceY; y < targetY; y++) {
                    int blockIdx = Math.max(0, Math.min(maxBlockIdx, (int) targetY - y));
                    short blockId = activeBlocksSet[blockIdx];

                    world.set(x, y, content.blocksRegistry.typeById(blockId), false);
                }
            }
        });
    }

    /**
     * Сглаживает перепад высот между краями мира
     * <p>
     * Поскольку левый край мира копируется в правый, между частью справа и скопированной частью может быть огромный перепад высот,
     * метод считает угол от {@link World#sizeX world.sizeX} - {@link WorldGeneratorConstants#COPY_SIZE COPY_SIZE} - {@link WorldGeneratorConstants#INTERPOLATE_SIZE INTERPOLATE_SIZE}
     * до {@link World#sizeX world.sizeX} - {@link WorldGeneratorConstants#COPY_SIZE COPY_SIZE} и заполняет блоками
     * @param lastY последняя высота
     * @param worldHeights карта высот
     */

    //todo переименовать
    private static void doItAgainToArrays(float lastY, float[] worldHeights) {
        //todo INTERPOLATE_SIZE динамически от размера перепада
        float lastX = world.sizeX - COPY_SIZE - INTERPOLATE_SIZE;
        float delta = DO_IT_AGAIN_DELTA;
        float delt = worldHeights[0] - lastY;
        float angle = (float) Math.toDegrees(Math.atan2(delta, delt));
        float deltaX = MathUtil.sin(Math.toRadians(angle));
        float deltaY = MathUtil.cos(Math.toRadians(angle));

        int lastSpawnedX = -1;
        int lastSpawnedY = -1;
        do {
            lastY += deltaY;
            lastX += deltaX;

            if ((int) lastX != lastSpawnedX || (int) lastY != lastSpawnedY) {
                lastSpawnedX = (int) lastX;
                lastSpawnedY = (int) lastY;

                worldHeights[lastSpawnedX] = lastY;
            }
        } while ((int) lastX < world.sizeX - COPY_SIZE);
    }

    /**
     * Создает точки пещер
     * <p>
     * Делит пещеры на поверхностные (генерируются от поверхности) и глубокие, вызывает метод генерации пещер,
     * пещера станет поверхностной если их количество меньше {@code caves} / {@link WorldGeneratorConstants#CAVES_UPPER_DIVISOR CAVES_UPPER_DIVISOR},
     * далее с вероятностью {@code rnd.nextFloat()} * {@link WorldGeneratorConstants#CAVES_UPPER_CHANCE CAVES_UPPER_CHANCE} > 1 (при 1.2 это ~16%).
     * </p>
     */
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

            //todo есть смысл потыкать положение пещер
            //потому что сейчас все верхние спавнятся в начале мира +-
            //остальной мир пустует на верхние
            //todo и убрать умножение в константах, это для тестов
            if (isUpper) {
                upper++;
                //пещеры с выходом на поверхность
                generateCave(upperX, findSurfaceY(upperX, 3),
                        startRadius, minRadius, maxRadius - 2,
                        CAVE_UPPER_MIN_ANGLE, CAVE_UPPER_MAX_ANGLE,
                        rnd.nextInt(CAVE_UPPER_START_MIN, CAVE_UPPER_START_MAX),
                        CAVE_UPPER_SHOT_CHANCE);
                //первое меняет случайный разброс, второе минимальное расстояние
                upperX += (int) ((rnd.nextFloat() * (world.sizeX / (caves / (CAVES_X_DIVISOR_UPPER * 4))) + (world.sizeX / (caves / (CAVES_X_DIVISOR_UPPER / 2)))));
            } else {
                //пещеры в глубине
                generateCave(downedX, (int) (findSurfaceY(downedX, 3) - rnd.nextFloat() * (world.sizeY / CAVES_DOWNED_Y_DIVISOR)),
                        startRadius, minRadius, maxRadius,
                        CAVE_DOWNED_MIN_ANGLE, CAVE_DOWNED_MAX_ANGLE,
                        rnd.nextInt(CAVE_DOWNED_START_ANGLE),
                        CAVE_DOWNED_SHOT_CHANCE);
                downedX += (int) ((rnd.nextFloat() * (world.sizeX / (caves / (CAVES_X_DIVISOR_DOWNED * 2)))) + (world.sizeX / (caves / CAVES_X_DIVISOR_DOWNED)));
            }
        }
        log.debug("spawned {} caves", caves);

        clearFloatingIslands(world.tiles, world.sizeX, world.sizeY, ISLAND_MAXSIZE_CLEAR_SIZE);
    }

    /**
     * Генерирует (копает) пещеру из точки
     * <p>
     * Шатается, меняет радиус и пускает новые отростки в рандомных направлениях
     * </p>
     * <p>
     * @param bx стартовый {@code x}
     * @param by стартовый {@code y}
     * @param radius стартовый радиус пещеры
     * @param minRadius минимальный радиус пещеры
     * @param maxRadius максимальный радиус пещеры
     * @param minAngle минимальный угол куда может пойти
     * @param maxAngle максимальный угол куда может пойти
     * @param startAngle стартовый угол копания
     * @param shotChance шанс отростка. Чем больше это значение,
     * тем меньше итоговый шанс, каждый новый отросток текущей пещеры
     * уменьшает итоговый шанс
     */
    private static void generateCave(int bx, int by, float radius, int minRadius, int maxRadius,
                                    int minAngle, int maxAngle, int startAngle, float shotChance) {

        if (minRadius < 1 || minRadius == maxRadius) {
            return;
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

                    generateCave(toBlock(wx), toBlock(wy), radius - 1, minRadius, (int) radius,
                            sAngle - CAVE_EVERY_ANGLE_OFFSET, sAngle + CAVE_EVERY_ANGLE_OFFSET,
                            sAngle, Math.min(CAVE_EVERY_CHANCE_SHOT_MAX, shotChance));
                    lastShot = 0;
                }

                //пещера влево вправо
                if (rnd.nextFloat() * (shotChance * CAVES_LR_CHANCE) < 1 && lastShot > CAVE_LR_MIN_ITERS) {
                    shotChance *= (rnd.nextFloat(CAVE_SHOT_MULT_MIN, CAVE_SHOT_MULT_MAX));

                    int sAngle = (int) ((angle + rnd.nextFloat(CAVE_SHOT_ANGLE_MIN, CAVE_SHOT_ANGLE_MAX)) % 360);
                    boolean left = rnd.nextBoolean();

                    generateCave(toBlock(wx), toBlock(wy), radius, minRadius, (int) radius,
                            left ? CAVE_BRANCH_LEFT_MIN_ANGLE : CAVE_BRANCH_RIGHT_MIN_ANGLE,
                            left ? CAVE_BRANCH_LEFT_MAX_ANGLE : CAVE_BRANCH_RIGHT_MAX_ANGLE,
                            sAngle, Math.min(CAVE_LR_CHANCE_SHOT_MAX, shotChance));
                    lastShot = 0;
                }
            }
        } while (world.inBounds(toBlock(wx), toBlock(wy - (rnd.nextFloat() * (world.sizeY / CAVE_Y_BOUND_DIVISOR)))) &&
                totalIters < CAVE_TOTAL_ITERS_MAX && rnd.nextFloat() * world.sizeY > totalIters / CAVE_TOTAL_ITERS_DIVISOR);
    }

    /**
     * Убирает блоки в заданной точке с заданным радиусом
     * @param bx точка {@code x}
     * @param by точка {@code y}
     * @param radius радиус
     */
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

    /**
     * Проверяет мир на наличие летающих островов, убирает слишком больше
     * @param tiles блоки
     * @param sizeX ширина карты
     * @param sizeY высота карты
     * @param minSize минимально блоков в острове, при котором он имеет право на жизнь
     */

    public static void clearFloatingIslands(short[] tiles, int sizeX, int sizeY, int minSize) {
        //Какая то сложнючая но быстрая дичь которую наверняка можно сделать лучше/красивее

        int totalColumns = sizeX;
        int cores = Runtime.getRuntime().availableProcessors();
        int chunkSize = (totalColumns / cores) + 1;

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
    }

    /**
     * Обертка для спавна всякой растительности
     * <p>
     * Вызывает посадку деревьев, травы и камешков
     * </p>
     * @param world мир
     */
    private static void generateEnvironments(World world) {
        generateTrees(world);
        generateDecorStones(world);
        generateHerb();
    }

    private static void generateTrees(World world) {
        //todo проверить
        //generateForest(80, 2, 20, 4, 8, "tree0", "tree1");
    }

    /**
     * Разбрасывает мелкие декоративные камни с шансом {@code Math.random()} * {@link WorldGeneratorConstants#DECOR_STONE_SPAWN_CHANCE DECOR_STONE_SPAWN_CHANCE} < 1
     * @param world мир
     */

    //todo а почему тут кстати не генератефорсет
    private static void generateDecorStones(World world) {
        var smallStone = Global.content.blockById("smallStone");
        float chance = DECOR_STONE_SPAWN_CHANCE;

        for (int x = 0; x < world.sizeX; x++) {
            if (Math.random() * chance < 1) {
                int y = findSurfaceY(x, 3);
                if (y - 1 > 0) {
                    if (world.getBlockType(x, y - 1) == Type.SOLID) {
                        world.set(x, y, smallStone, false);
                    }
                }
            }
        }
    }

    /**
     * Засаживает траву
     */
    private static void generateHerb() {
        generateForest(HERB_SPAWN_CHANCE,
                HERB_MIN_FOREST_SIZE, HERB_MAX_FOREST_SIZE,
                HERB_MIN_SPAWN_DIST, HERB_MAX_SPAWN_DIST,
                "herb");
    }

    /**
     * Универсальная штука для сажания всякого
     * <p>
     * Работает в два этапа: сначала раскидывает точки будущих лесов, чтоб они не лезли друг на друга,
     * а потом садит блоки
     * </p>
     * @param chance шанс появления леса
     * @param minForestSize минимальный размер леса
     * @param maxForestSize максимальный размер леса
     * @param minSpawnDistance минимальное расстояние
     * @param maxSpawnDistance максимальное расстояние
     * @param structuresName название структур (или блоков), сажаемое выбирается рандомно из данных
     */
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
                    int yStruct = findSurfaceY(x + (i * distance), 3);

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

    /**
     * Стартует сцену игры (под игрой подразумевается то, что открывается после конца генерации мира)
     * @param playGameScene сцена игры
     */
    private static void startGame(PlayGameScene playGameScene) {
        gameScene.onPreloadCompletion(() -> {
            UIMenus.createPlanet().hide();

            setGameScene(playGameScene);
            gameState = GameState.PLAYING;
        });
    }

    /**
     * @return случайная точка {@code Point2i} в мире на поверхности
     */
    private static Point2i randAtGround() {
        var rnd = ThreadLocalRandom.current();
        int randX = rnd.nextInt(0, world.sizeX);

        return new Point2i(randX, World.findSurfaceY(randX, 2));
    }
}
