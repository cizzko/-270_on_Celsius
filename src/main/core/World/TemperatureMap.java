package core.World;

import core.World.WorldGenerator.Biomes;
import core.World.Weather.Sun;
import core.content.blocks.Block;
import core.content.entity.LivingEntity;
import core.math.MathUtil;
import core.util.BatchScope;
import core.util.Debug;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static core.Global.*;
import static core.World.WorldGenerator.WorldGeneratorConstants.*;
import static java.lang.Math.clamp;

//todo есть куда стремиться
//а еще есть быстрая гпу версия, но она фризит рендер
//тут надо что то/кто то умное
//todo формула хеттрансфера для сущностей уехала кудат
public final class TemperatureMap {
    private static float avgFrameTimeNs = 0f;
    private static int consecutiveGoodFrames = 0;
    private static boolean simplifyNextFrame = false;

    private static float[] temps;
    private static float[] tempsNext;

    private static float[] density;

    private static float[] vx;
    private static float[] vxNext;

    private static float[] vy;
    private static float[] vyNext;

    private static float[] divergence;

    private static byte[] blockSolid;
    private static byte[] boundaryMask;

    public static int chunkShift = 5;

    public static float SLEEP_TEMP_THRESHOLD = 0.3f;
    public static float SLEEP_VELOCITY_THRESHOLD = 0.1f;
    public static final float SLEEP_PRESSURE_THRESHOLD = 1f;

    public static boolean USE_GRAVITY = true;
    public static boolean USE_COOLING = true;
    public static boolean USE_RADIATIVE_COOLING = true;
    public static boolean USE_ATMOSPHERIC_COOLING = true;
    public static boolean USE_SOLAR_HEATING = true;
    public static boolean USE_SIMPLE_VELOCITY_ADVECTION = false;
    public static boolean DISABLE_VELOCITY_DIFFUSION = false;

    public static float RADIATIVE_COOLING_RATE = 0.0001f;
    public static float ATMOSPHERIC_COOLING_RATE = 0.000001f;
    public static float SOLAR_FLUX = 13000f;

    public static final float SPACE_TEMP = -270.0f;
    public static final float RADIATIVE_T_REF = 313.0f;

    private static final float SOR_OMEGA = 1.8f;
    public static int SOR_MIN_ITERATIONS = 4;
    public static int SOR_MAX_ITERATIONS = 10;
    private static final float FIRST_SOR_OMEGA = SOR_OMEGA;

    private static final float BUOYANCY_K = 0.023f;
    private static final float PRESSURE_K = 0.7f;
    private static final float DAMPING = 0.99f;
    private static final float DT = 0.1f;

    public static final float VELOCITY_CUTOFF = 0.015f;
    private static final float PRESSURE_DECAY = 0.95f;
    private static final float PREDICT_VELOCITY_MULT = 3.0f;
    private static final float CHUNK_SPREAD_TEMP_THRESHOLD = SLEEP_TEMP_THRESHOLD * 1.5f;

    //todo нормальная теплопередача
    private static final float CAP_AIR = 1.0f;
    private static final float CAP_SOLID = 0.8f;
    private static final float COND_AIR = 0.4f;
    private static final float COND_SOLID = 0.8f;
    private static final float COND_INTERFACE = 0.9f;
    private static final float COND_SKY = 0.1f;
    private static final float COND_GROUND = 0.1f;

    private static final float VELOCITY_DIFFUSION = 0.05f;

    private static final float GRAVITY_STRENGTH = -0.002f;
    private static final float COOLING_SLOPE = 0.005f;

    private static final float INV_CAP_AIR = 1.0f / CAP_AIR;
    private static final float INV_CAP_SOLID = 1.0f / CAP_SOLID;
    private static final float DIVERGENCE = -0.5f * PRESSURE_K / DT;
    private static final float PRESSURE_GRAD = 0.5f * DT;

    public static long MAX_FRAME_BUDGET_NANOS = 90_000_000L;

    public static boolean SIMPLIFY_THERMAL = true;
    public static boolean SIMPLIFY_VELOCITY_ADVECTION = true;
    public static boolean SIMPLIFY_TEMPERATURE_ADVECTION = true;
    public static boolean SIMPLIFY_SOR = true;

    private static final int MASK_THERMAL = 1;
    private static final int MASK_ADVECTION = 2;
    private static int frameCounter = 0;

    private static int WORLD_WIDTH;
    private static int WORLD_HEIGHT;
    private static int chunksX;
    private static int chunksY;
    private static boolean[] activeChunks;
    private static boolean[] nextActiveChunks;
    private static int[] activeChunkIndices;
    private static volatile int activeChunkCount = 0;

    public static int fps = 0, targetFPS = 10, accumFPS = 0;
    private static long lastSwapFPS = System.currentTimeMillis();

    private static int wrapX(int x) {
        int w = WORLD_WIDTH;
        if (x < 0) {
            return x + w;
        }
        if (x >= w) {
            return x - w;
        }
        return x;
    }

    private static float wrapFloatX(float x) {
        float w = WORLD_WIDTH;
        float m = x % w;
        if (m < 0) {
            m += w;
        }
        return m;
    }

    private static int adaptiveIterations(int activeDiameter) {
        return clamp(activeDiameter >> 6, SOR_MIN_ITERATIONS, SOR_MAX_ITERATIONS);
    }

    public static int pos2index(int x, int y) {
        return wrapX(x) * WORLD_HEIGHT + y;
    }

    public static boolean isFluidBoundary(int x, int y) {
        x = wrapX(x);
        if (y <= 0 || y >= WORLD_HEIGHT - 1) {
            return true;
        }
        return world.isBlockType(x, y, Block.Type.SOLID);
    }

    public static void generate() {
        if (WORLD_WIDTH == 0) {
            init();
        }

        int maxSurfaceY = 0;
        for (int x = 0; x < WORLD_WIDTH; x++) {
            int sY = world.surfaces[x];
            if (sY > maxSurfaceY) {
                maxSurfaceY = sY;
            }
        }

        final int yLowerBound = (int) (world.sizeY / 2);
        final double c = 5.0 / yLowerBound;

        for (int x = 0; x < WORLD_WIDTH; x++) {
            int surfaceY = world.surfaces[x];
            Biomes biome = world.getBiomes(x);
            if (biome == null) {
                biome = Biomes.getDefault();
            }
            int biomeTemp = biome.getTemp();
            int biomeBottom = Math.max(0, surfaceY - 5);
            int biomeTop = Math.min(WORLD_HEIGHT - 1, surfaceY + 10);

            for (int y = 0; y < WORLD_HEIGHT; y++) {
                boolean solid = world.isBlockType(x, y, Block.Type.SOLID);
                float temp = 20.0f;

                if (y >= biomeBottom && y <= biomeTop) {
                    temp = (float) biomeTemp;
                }
                else if (solid) {
                    if (y <= yLowerBound) {
                        temp = (float) (1000 * Math.exp(-c * y) + 12.0);
                    } else {
                        temp = 0.0f;
                    }
                }
                setTemp(x, y, temp);
                density[pos2index(x, y)] = 1.0f;
            }
        }

//        int total = 550;
//        long delta = 0;
//        int w = 0;
//        var scope = new BatchScope(world.genPool);
//        for (int i = 0; i < total; i++) {
//            long a = System.nanoTime();
//            if (i % 2 == 0) {
//                Debug.saveTemp("Temp" + w, scope);
//                w++;
//            }
//            update(scope);
//            System.out.println((System.nanoTime() - a) / 1_000_000f);
//            delta += (System.nanoTime() - a);
//        }
//
//        double numb = (double) delta / total / 1_000_000f;
//        System.out.println(numb + " | " + (1000 / numb));
//
//        Debug.saveTemp("Temp", scope);
//        Debug.saveWindForce("Power", scope);
//        Debug.saveWindDirection("Dir", scope);
    }

    public static void init() {
        WORLD_WIDTH = world.sizeX;
        WORLD_HEIGHT = world.sizeY;
        int length = WORLD_WIDTH * WORLD_HEIGHT;

        temps = new float[length];
        tempsNext = new float[length];

        density = new float[length];

        vx = new float[length];
        vxNext = new float[length];

        vy = new float[length];
        vyNext = new float[length];

        divergence = new float[length];

        blockSolid = new byte[length];
        boundaryMask = new byte[length];

        int chunkSize = 1 << chunkShift;
        chunksX = (WORLD_WIDTH + chunkSize - 1) >> chunkShift;
        chunksY = (WORLD_HEIGHT + chunkSize - 1) >> chunkShift;
        int chunkCount = chunksX * chunksY;

        activeChunks = new boolean[chunkCount];
        nextActiveChunks = new boolean[chunkCount];
        activeChunkIndices = new int[chunkCount];
        activeChunkCount = 0;
    }

    public static void start() {
        var scope = new BatchScope(world.genPool);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            update(scope);
            accumFPS++;
            long now = System.currentTimeMillis();
            if (now - lastSwapFPS >= 1000) {
                lastSwapFPS = now;
                fps = accumFPS;
                accumFPS = 0;
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    public static void updateBlock(int x, int y, Block block) {
        int idx = pos2index(x, y);
        boolean solid = world.isBlockType(x, y, Block.Type.SOLID);
        blockSolid[idx] = solid ? (byte) 1 : 0;
        boundaryMask[idx] = (y <= 0 || y >= WORLD_HEIGHT - 1 || solid) ? (byte) 1 : 0;
        markAreaActive(x, y, 1);
    }

    public static void markChunkActive(int cx, int cy) {
        cx = (cx + chunksX) % chunksX;
        if (cy >= 0 && cy < chunksY) {
            nextActiveChunks[cx * chunksY + cy] = true;
        }
    }

    public static void markAreaActive(int x, int y, int radiusBlocks) {
        int minCx = (wrapX(x - radiusBlocks) >> chunkShift);
        int maxCx = (wrapX(x + radiusBlocks) >> chunkShift);
        if (maxCx < minCx) {
            for (int cx = 0; cx < chunksX; cx++) {
                int minCy = clamp((y - radiusBlocks) >> chunkShift, 0, chunksY - 1);
                int maxCy = clamp((y + radiusBlocks) >> chunkShift, 0, chunksY - 1);
                for (int cy = minCy; cy <= maxCy; cy++) {
                    markChunkActive(cx, cy);
                }
            }
        } else {
            int minCy = clamp((y - radiusBlocks) >> chunkShift, 0, chunksY - 1);
            int maxCy = clamp((y + radiusBlocks) >> chunkShift, 0, chunksY - 1);
            for (int cx = minCx; cx <= maxCx; cx++) {
                for (int cy = minCy; cy <= maxCy; cy++) {
                    markChunkActive(cx, cy);
                }
            }
        }
    }

    private static void prepareActiveChunks() {
        boolean hasNext = false;
        for (boolean b : nextActiveChunks) {
            if (b) {
                hasNext = true;
                break;
            }
        }

        if (hasNext) {
            Arrays.fill(activeChunks, false);
            activeChunkCount = 0;

            for (int cx = 0; cx < chunksX; cx++) {
                for (int cy = 0; cy < chunksY; cy++) {
                    if (nextActiveChunks[cx * chunksY + cy]) {
                        int idx = cx * chunksY + cy;
                        if (!activeChunks[idx]) {
                            activeChunks[idx] = true;
                            activeChunkIndices[activeChunkCount++] = (cx << 16) | cy;
                        }
                    }
                }
            }
            Arrays.fill(nextActiveChunks, false);
        }
    }

    private static float getEdgeConductivity(boolean solid1, boolean solid2) {
        if (solid1 && solid2) {
            return COND_SOLID;
        }
        if (!solid1 && !solid2) {
            return COND_AIR;
        }
        return COND_INTERFACE;
    }

    private static float sampleWithCorrection(float[] field, int idx, int x, int y, float rayX, float rayY, boolean useSolidFallback, float fallback) {
        int sampleX = Math.floorMod((int) rayX, WORLD_WIDTH);
        int sampleY = Math.max(0, Math.min(WORLD_HEIGHT - 1, (int) rayY));

        if (blockSolid[wrapX(sampleX) * WORLD_HEIGHT + y] == 1) {
            rayX = x;
        }
        if (blockSolid[x * WORLD_HEIGHT + sampleY] == 1) {
            rayY = y;
        }

        int finalSampleX = Math.floorMod((int) rayX, WORLD_WIDTH);
        int finalSampleY = Math.max(0, Math.min(WORLD_HEIGHT - 1, (int) rayY));
        if (blockSolid[wrapX(finalSampleX) * WORLD_HEIGHT + finalSampleY] == 1) {
            rayX = x;
            rayY = y;
        }

        int x0 = Math.floorMod((int) rayX, WORLD_WIDTH);
        int x1 = wrapX(x0 + 1);
        int y0 = Math.max(0, Math.min(WORLD_HEIGHT - 2, (int) rayY));
        int y1 = y0 + 1;
        float s1 = rayX - x0;
        float s0 = 1 - s1;
        float t1 = rayY - y0;
        float t0 = 1 - t1;

        int i00 = wrapX(x0) * WORLD_HEIGHT + y0;
        int i01 = wrapX(x0) * WORLD_HEIGHT + y1;
        int i10 = x1 * WORLD_HEIGHT + y0;
        int i11 = x1 * WORLD_HEIGHT + y1;

        float v00 = (useSolidFallback && blockSolid[i00] == 1) ? fallback : field[i00];
        float v01 = (useSolidFallback && blockSolid[i01] == 1) ? fallback : field[i01];
        float v10 = (useSolidFallback && blockSolid[i10] == 1) ? fallback : field[i10];
        float v11 = (useSolidFallback && blockSolid[i11] == 1) ? fallback : field[i11];

        // Use FMA for bilinear interpolation
        float inner0 = MathUtil.fma(t0, v00, t1 * v01);
        float inner1 = MathUtil.fma(t0, v10, t1 * v11);
        return MathUtil.fma(s0, inner0, s1 * inner1);
    }

    private static void sorPass(BatchScope scope, int parity, float omega) {
        float oneMinusOmega = 1.0f - omega;
        int shift = chunkShift;
        scope.submit(0, activeChunkCount, (startChunkIdx, endChunkIdx) -> {
            for (int i = startChunkIdx; i < endChunkIdx; i++) {
                int packed = activeChunkIndices[i];
                int cx = packed >> 16;
                int cy = packed & 0xFFFF;
                int startX = cx << shift;
                int endX = Math.min(WORLD_WIDTH, (cx + 1) << shift);
                int startY = Math.max(1, cy << shift);
                int endY = Math.min(WORLD_HEIGHT - 1, (cy + 1) << shift);

                for (int x = startX; x < endX; x++) {
                    int xIdx = x * WORLD_HEIGHT;
                    int firstY = startY + ((x + startY + parity) & 1);
                    for (int y = firstY; y < endY; y += 2) {
                        int idx = xIdx + y;
                        if (boundaryMask[idx] == 1) {
                            continue;
                        }

                        int leftX = wrapX(x - 1);
                        int rightX = wrapX(x + 1);
                        float pL = boundaryMask[leftX * WORLD_HEIGHT + y] == 1 ? density[idx] : density[leftX * WORLD_HEIGHT + y];
                        float pR = boundaryMask[rightX * WORLD_HEIGHT + y] == 1 ? density[idx] : density[rightX * WORLD_HEIGHT + y];
                        float pD = (y - 1 < 0) ? density[idx] : (boundaryMask[idx - 1] == 1 ? density[idx] : density[idx - 1]);
                        float pU = (y + 1 >= WORLD_HEIGHT) ? 0f : (boundaryMask[idx + 1] == 1 ? density[idx] : density[idx + 1]);

                        float oldP = density[idx];
                        float newP = (divergence[idx] + pL + pR + pD + pU) * 0.25f;
                        density[idx] = MathUtil.fma(oneMinusOmega, oldP, omega * newP);
                    }
                }
            }
        });
    }

    public static void update(BatchScope scope) {
        long frameStartNanos = System.nanoTime();

        boolean simplifyThisFrame = simplifyNextFrame;
        if (!simplifyThisFrame) {
            simplifyThisFrame = (avgFrameTimeNs > MAX_FRAME_BUDGET_NANOS);
        }

        int currentParity = frameCounter & 1;
        frameCounter++;

        updateLivingEntity();
        prepareActiveChunks();
        if (activeChunkCount == 0) {
            avgFrameTimeNs = 0f;
            consecutiveGoodFrames = 0;
            simplifyNextFrame = false;
            return;
        }

        final int shift = chunkShift;
        final int mask = (1 << shift) - 1;
        final float grav = USE_GRAVITY ? GRAVITY_STRENGTH : 0f;
        final boolean simpleVAdvection = USE_SIMPLE_VELOCITY_ADVECTION;
        final boolean noVelDiffusion = DISABLE_VELOCITY_DIFFUSION;

        final byte[] appliedMask = new byte[chunksX * chunksY];
        final AtomicInteger thermalChunks = new AtomicInteger(0);
        final AtomicInteger advectionChunks = new AtomicInteger(0);
        final boolean[] sorSimplified = {false};

        scope.submit(0, chunksX * chunksY, (start, end) -> {
            for (int i = start; i < end; i++) {
                int cx = i / chunksY;
                int cy = i % chunksY;
                int chunkStartX = cx << shift;
                int chunkEndX = Math.min(WORLD_WIDTH, (cx + 1) << shift);
                int chunkStartY = cy << shift;
                int chunkEndY = Math.min(WORLD_HEIGHT, (cy + 1) << shift);
                for (int x = chunkStartX; x < chunkEndX; x++) {
                    int xIdx = x * WORLD_HEIGHT;
                    int realX = wrapX(x);
                    for (int y = chunkStartY; y < chunkEndY; y++) {
                        int idx = xIdx + y;
                        boolean solid = world.isBlockType(realX, y, Block.Type.SOLID);
                        blockSolid[idx] = solid ? (byte) 1 : 0;
                        boundaryMask[idx] = (y <= 0 || y >= WORLD_HEIGHT - 1 || solid) ? (byte) 1 : 0;
                    }
                }
            }
        });

        final boolean simplifyThermal = simplifyThisFrame && SIMPLIFY_THERMAL;
        final int thermalParity = simplifyThermal ? currentParity : -1;
        scope.submit(0, activeChunkCount, (startChunkIdx, endChunkIdx) -> {
            for (int i = startChunkIdx; i < endChunkIdx; i++) {
                int packed = activeChunkIndices[i];
                int cx = packed >> 16;
                int cy = packed & 0xFFFF;
                int chunkIdx = cx * chunksY + cy;
                int startX = cx << shift;
                int endX = Math.min(WORLD_WIDTH, (cx + 1) << shift);
                int startY = Math.max(0, cy << shift);
                int endY = Math.min(WORLD_HEIGHT, (cy + 1) << shift);

                boolean chunkGotThermal = false;
                for (int x = startX; x < endX; x++) {
                    int xIdx = x * WORLD_HEIGHT;
                    int realX = wrapX(x);
                    for (int y = startY; y < endY; y++) {
                        int idx = xIdx + y;

                        int leftX = wrapX(x - 1);
                        int rightX = wrapX(x + 1);
                        int idxL = leftX * WORLD_HEIGHT + y;
                        int idxR = rightX * WORLD_HEIGHT + y;

                        boolean cellThermalSimplified = simplifyThermal && ((x + y) & 1) == thermalParity;
                        if (cellThermalSimplified) {
                            chunkGotThermal = true;
                        }

                        boolean isSolidA = blockSolid[idx] == 1;
                        float capA = isSolidA ? CAP_SOLID : CAP_AIR;
                        float invCapA = isSolidA ? INV_CAP_SOLID : INV_CAP_AIR;
                        float tempA = temps[idx];
                        float totalFlux = 0f;

                        if (!cellThermalSimplified) {
                            boolean solidL = blockSolid[idxL] == 1;
                            boolean solidR = blockSolid[idxR] == 1;
                            totalFlux += getEdgeConductivity(isSolidA, solidL) * (temps[idxL] - tempA);
                            totalFlux += getEdgeConductivity(isSolidA, solidR) * (temps[idxR] - tempA);

                            if (y > 0) {
                                int idxD = idx - 1;
                                boolean solidD = blockSolid[idxD] == 1;
                                totalFlux += getEdgeConductivity(isSolidA, solidD) * (temps[idxD] - tempA);
                            } else {
                                totalFlux -= COND_GROUND * tempA;
                            }
                            if (y < WORLD_HEIGHT - 1) {
                                int idxU = idx + 1;
                                boolean solidU = blockSolid[idxU] == 1;
                                totalFlux += getEdgeConductivity(isSolidA, solidU) * (temps[idxU] - tempA);
                            } else {
                                totalFlux -= COND_SKY * tempA;
                            }

                            if (USE_COOLING) {
                                float heightFraction = (float) y / (WORLD_HEIGHT - 1);
                                totalFlux -= COOLING_SLOPE * heightFraction * tempA;
                            }
                        }

                        float currentT = cellThermalSimplified ? tempA : MathUtil.fma(totalFlux * DT, invCapA, tempA);
                        if (y == WORLD_HEIGHT - 1) {
                            currentT = 0f;
                        }

                        if (USE_ATMOSPHERIC_COOLING && !isSolidA && y >= world.surfaces[realX]) {
                            float heightFraction = (float) y / (WORLD_HEIGHT - 1);
                            float heightFactor = heightFraction * heightFraction;
                            float coolingRate = ATMOSPHERIC_COOLING_RATE / (WORLD_HEIGHT / 1000.0f);
                            float cooling = coolingRate * heightFactor * (currentT - SPACE_TEMP) * DT;
                            currentT = Math.max(currentT - cooling, SPACE_TEMP);
                        }

                        int surfaceY = world.surfaces[realX];
                        int blockY = surfaceY - 1;
                        if (isSolidA && y == blockY) {
                            Block block = world.getBlock(realX, y);

                            if (USE_RADIATIVE_COOLING) {
                                float emissivity = block.emissivity / 100.0f;
                                float T_kelvin = Math.max(currentT + 270.0f, 1.0f);
                                float T2 = T_kelvin * T_kelvin;
                                float T4 = T2 * T2;
                                float T_ref4 = RADIATIVE_T_REF * RADIATIVE_T_REF * RADIATIVE_T_REF * RADIATIVE_T_REF;
                                float normalized = T4 / T_ref4;
                                float cooling = RADIATIVE_COOLING_RATE * emissivity * normalized * DT;
                                currentT = Math.max(currentT - cooling, SPACE_TEMP);
                            }

                            if (USE_SOLAR_HEATING) {
                                float albedo = block.albedo / 100.0f;
                                float effectiveWidth = WORLD_WIDTH - COPY_SIZE;
                                float deltaX = (float) (Sun.globalTime - x);
                                float angle = (deltaX / effectiveWidth) * 2.0f * (float)Math.PI;
                                float cosAngle = (float)Math.cos(angle);
                                float angleFactor = (float)Math.acos(cosAngle) / (float)Math.PI;
                                float sunFactor = 1.0f - angleFactor;
                                float heatGain = SOLAR_FLUX * (1.0f - albedo) * sunFactor * DT;
                                float heatCapacity = Math.max((float)block.thermalCapacity * 1000.0f, 1.0f);
                                currentT += heatGain / heatCapacity;
                            }
                        }
                        tempsNext[idx] = currentT;

                        if (boundaryMask[idx] == 1) {
                            vxNext[idx] = 0;
                            vyNext[idx] = 0;
                            if (Math.abs(currentT) > SLEEP_TEMP_THRESHOLD) {
                                int currCx = x >> shift;
                                int currCy = y >> shift;
                                markChunkActive(currCx, currCy);
                                if (Math.abs(currentT) > CHUNK_SPREAD_TEMP_THRESHOLD) {
                                    int prevCx = (currCx - 1 + chunksX) % chunksX;
                                    int nextCx = (currCx + 1) % chunksX;
                                    if ((x & mask) == 0) {
                                        markChunkActive(prevCx, currCy);
                                    }
                                    if ((x & mask) == mask) {
                                        markChunkActive(nextCx, currCy);
                                    }
                                    if ((y & mask) == 0) {
                                        markChunkActive(currCx, currCy - 1);
                                    }
                                    if ((y & mask) == mask) {
                                        markChunkActive(currCx, currCy + 1);
                                    }
                                }
                            }
                            continue;
                        }

                        float nextVx;
                        float nextVy;
                        if (noVelDiffusion) {
                            nextVx = vx[idx] * DAMPING;
                            nextVy = MathUtil.fma(currentT, BUOYANCY_K, vy[idx]) * DAMPING;
                        } else {
                            float vxSum = vx[idx];
                            float vySum = vy[idx];
                            float vCount = 1f;
                            if (boundaryMask[idxL] == 0) {
                                vxSum += vx[idxL] * VELOCITY_DIFFUSION;
                                vySum += vy[idxL] * VELOCITY_DIFFUSION;
                                vCount += VELOCITY_DIFFUSION;
                            }
                            if (boundaryMask[idxR] == 0) {
                                vxSum += vx[idxR] * VELOCITY_DIFFUSION;
                                vySum += vy[idxR] * VELOCITY_DIFFUSION;
                                vCount += VELOCITY_DIFFUSION;
                            }
                            if (y > 0 && boundaryMask[idx - 1] == 0) {
                                vxSum += vx[idx - 1] * VELOCITY_DIFFUSION;
                                vySum += vy[idx - 1] * VELOCITY_DIFFUSION;
                                vCount += VELOCITY_DIFFUSION;
                            }
                            if (y < WORLD_HEIGHT - 1 && boundaryMask[idx + 1] == 0) {
                                vxSum += vx[idx + 1] * VELOCITY_DIFFUSION;
                                vySum += vy[idx + 1] * VELOCITY_DIFFUSION;
                                vCount += VELOCITY_DIFFUSION;
                            }
                            float invVCount = 1.0f / vCount;
                            nextVx = (vxSum * invVCount) * DAMPING;
                            nextVy = MathUtil.fma(currentT, BUOYANCY_K, vySum * invVCount) * DAMPING;
                        }
                        if (USE_GRAVITY) {
                            nextVy += grav;
                        }

                        if (y > 0 && blockSolid[idx - 1] == 1 && nextVy < 0) {
                            float fallSpeed = Math.abs(nextVy);
                            nextVy = 0f;
                            float splashBoost = fallSpeed * 1.8f;
                            if (Math.abs(nextVx) >= VELOCITY_CUTOFF) {
                                nextVx += Math.copySign(splashBoost, nextVx);
                            }
                        }

                        vxNext[idx] = nextVx;
                        vyNext[idx] = nextVy;

                        boolean isActive = Math.abs(currentT) > SLEEP_TEMP_THRESHOLD
                                || Math.abs(nextVx) > SLEEP_VELOCITY_THRESHOLD
                                || Math.abs(nextVy) > SLEEP_VELOCITY_THRESHOLD
                                || Math.abs(density[idx]) > SLEEP_PRESSURE_THRESHOLD;
                        if (isActive) {
                            int currCx = x >> shift;
                            int currCy = y >> shift;
                            markChunkActive(currCx, currCy);
                            float predTh = SLEEP_VELOCITY_THRESHOLD * PREDICT_VELOCITY_MULT;
                            int prevCx = (currCx - 1 + chunksX) % chunksX;
                            int nextCx = (currCx + 1) % chunksX;
                            if ((x & mask) == 0 && nextVx < -predTh) {
                                markChunkActive(prevCx, currCy);
                            }
                            if ((x & mask) == mask && nextVx > predTh) {
                                markChunkActive(nextCx, currCy);
                            }
                            if ((y & mask) == 0 && nextVy < -predTh) {
                                markChunkActive(currCx, currCy - 1);
                            }
                            if ((y & mask) == mask && nextVy > predTh) {
                                markChunkActive(currCx, currCy + 1);
                            }
                            if (Math.abs(currentT) > CHUNK_SPREAD_TEMP_THRESHOLD) {
                                if ((x & mask) == 0) {
                                    markChunkActive(prevCx, currCy);
                                }
                                if ((x & mask) == mask) {
                                    markChunkActive(nextCx, currCy);
                                }
                                if ((y & mask) == 0) {
                                    markChunkActive(currCx, currCy - 1);
                                }
                                if ((y & mask) == mask) {
                                    markChunkActive(currCx, currCy + 1);
                                }
                            }
                        }
                    }
                }
                if (chunkGotThermal) {
                    appliedMask[chunkIdx] |= MASK_THERMAL;
                    thermalChunks.incrementAndGet();
                }
            }
        });

        swapTemps();
        swapVx();
        swapVy();

        scope.submit(0, activeChunkCount, (startChunkIdx, endChunkIdx) -> {
            for (int i = startChunkIdx; i < endChunkIdx; i++) {
                int packed = activeChunkIndices[i];
                int cx = packed >> 16;
                int cy = packed & 0xFFFF;
                int startX = cx << shift;
                int endX = Math.min(WORLD_WIDTH, (cx + 1) << shift);
                int startY = Math.max(0, cy << shift);
                int endY = Math.min(WORLD_HEIGHT, (cy + 1) << shift);

                int prevCx = (cx - 1 + chunksX) % chunksX;
                int nextCx = (cx + 1) % chunksX;

                for (int y = startY; y < endY; y++) {
                    int idx = startX * WORLD_HEIGHT + y;
                    if (boundaryMask[idx] == 0 &&
                            (Math.abs(temps[idx]) > CHUNK_SPREAD_TEMP_THRESHOLD ||
                                    Math.abs(vx[idx]) > SLEEP_VELOCITY_THRESHOLD ||
                                    Math.abs(vy[idx]) > SLEEP_VELOCITY_THRESHOLD)) {
                        markChunkActive(prevCx, cy);
                        break;
                    }
                }
                int rightX = endX - 1;
                for (int y = startY; y < endY; y++) {
                    int idx = rightX * WORLD_HEIGHT + y;
                    if (boundaryMask[idx] == 0 &&
                            (Math.abs(temps[idx]) > CHUNK_SPREAD_TEMP_THRESHOLD ||
                                    Math.abs(vx[idx]) > SLEEP_VELOCITY_THRESHOLD ||
                                    Math.abs(vy[idx]) > SLEEP_VELOCITY_THRESHOLD)) {
                        markChunkActive(nextCx, cy);
                        break;
                    }
                }
                if (startY > 0) {
                    for (int x = startX; x < endX; x++) {
                        int idx = x * WORLD_HEIGHT + startY;
                        if (boundaryMask[idx] == 0 &&
                                (Math.abs(temps[idx]) > CHUNK_SPREAD_TEMP_THRESHOLD ||
                                        Math.abs(vx[idx]) > SLEEP_VELOCITY_THRESHOLD ||
                                        Math.abs(vy[idx]) > SLEEP_VELOCITY_THRESHOLD)) {
                            markChunkActive(cx, cy - 1);
                            break;
                        }
                    }
                }
                if (endY < WORLD_HEIGHT) {
                    int upY = endY - 1;
                    for (int x = startX; x < endX; x++) {
                        int idx = x * WORLD_HEIGHT + upY;
                        if (boundaryMask[idx] == 0 &&
                                (Math.abs(temps[idx]) > CHUNK_SPREAD_TEMP_THRESHOLD ||
                                        Math.abs(vx[idx]) > SLEEP_VELOCITY_THRESHOLD ||
                                        Math.abs(vy[idx]) > SLEEP_VELOCITY_THRESHOLD)) {
                            markChunkActive(cx, cy + 1);
                            break;
                        }
                    }
                }
            }
        });

        int minCx = chunksX;
        int maxCx = -1;
        int minCy = chunksY;
        int maxCy = -1;
        for (int i = 0; i < activeChunkCount; i++) {
            int packed = activeChunkIndices[i];
            int cx = packed >> 16;
            int cy = packed & 0xFFFF;
            if (cx < minCx) {
                minCx = cx;
            }
            if (cx > maxCx) {
                maxCx = cx;
            }
            if (cy < minCy) {
                minCy = cy;
            }
            if (cy > maxCy) {
                maxCy = cy;
            }
        }

        if (maxCx != -1) {
            int activeDiameter = Math.max((maxCx - minCx + 1) << shift, (maxCy - minCy + 1) << shift);
            int iterations = adaptiveIterations(activeDiameter);

            if (simplifyThisFrame && SIMPLIFY_SOR) {
                iterations = Math.max(SOR_MIN_ITERATIONS, iterations >> 1);
                sorSimplified[0] = true;
            }

            scope.submit(0, activeChunkCount, (startChunkIdx, endChunkIdx) -> {
                for (int i = startChunkIdx; i < endChunkIdx; i++) {
                    int packed = activeChunkIndices[i];
                    int cx = packed >> 16;
                    int cy = packed & 0xFFFF;
                    int startX = cx << shift;
                    int endX = Math.min(WORLD_WIDTH, (cx + 1) << shift);
                    int startY = Math.max(1, cy << shift);
                    int endY = Math.min(WORLD_HEIGHT - 1, (cy + 1) << shift);

                    for (int x = startX; x < endX; x++) {
                        int xIdx = x * WORLD_HEIGHT;
                        for (int y = startY; y < endY; y++) {
                            int idx = xIdx + y;
                            if (boundaryMask[idx] == 1) {
                                density[idx] = 0;
                                divergence[idx] = 0;
                                continue;
                            }
                            int leftX = wrapX(x - 1);
                            int rightX = wrapX(x + 1);
                            float vxL = boundaryMask[leftX * WORLD_HEIGHT + y] == 1 ? 0f : vx[leftX * WORLD_HEIGHT + y];
                            float vxR = boundaryMask[rightX * WORLD_HEIGHT + y] == 1 ? 0f : vx[rightX * WORLD_HEIGHT + y];
                            float vyU = (y + 1 >= WORLD_HEIGHT) ? 0f : (boundaryMask[idx + 1] == 1 ? 0f : vy[idx + 1]);
                            float vyD = boundaryMask[idx - 1] == 1 ? 0f : vy[idx - 1];

                            divergence[idx] = DIVERGENCE * ((vxR - vxL) + (vyU - vyD));
                            density[idx] *= PRESSURE_DECAY;
                            if (Math.abs(density[idx]) < SLEEP_PRESSURE_THRESHOLD) {
                                density[idx] = 0f;
                            }
                        }
                    }
                }
            });

            for (int iter = 0; iter < iterations; iter++) {
                float omega = (iter == 0) ? FIRST_SOR_OMEGA : SOR_OMEGA;
                sorPass(scope, 0, omega);
                sorPass(scope, 1, omega);
            }

            scope.submit(0, activeChunkCount, (startChunkIdx, endChunkIdx) -> {
                for (int i = startChunkIdx; i < endChunkIdx; i++) {
                    int packed = activeChunkIndices[i];
                    int cx = packed >> 16;
                    int cy = packed & 0xFFFF;
                    int startX = cx << shift;
                    int endX = Math.min(WORLD_WIDTH, (cx + 1) << shift);
                    int startY = Math.max(1, cy << shift);
                    int endY = Math.min(WORLD_HEIGHT - 1, (cy + 1) << shift);

                    for (int x = startX; x < endX; x++) {
                        int xIdx = x * WORLD_HEIGHT;
                        for (int y = startY; y < endY; y++) {
                            int idx = xIdx + y;
                            if (boundaryMask[idx] == 1) {
                                continue;
                            }

                            int leftX = wrapX(x - 1);
                            int rightX = wrapX(x + 1);
                            float pL = boundaryMask[leftX * WORLD_HEIGHT + y] == 1 ? density[idx] : density[leftX * WORLD_HEIGHT + y];
                            float pR = boundaryMask[rightX * WORLD_HEIGHT + y] == 1 ? density[idx] : density[rightX * WORLD_HEIGHT + y];
                            float pD = (y - 1 < 0) ? density[idx] : (boundaryMask[idx - 1] == 1 ? density[idx] : density[idx - 1]);
                            float pU = (y + 1 >= WORLD_HEIGHT) ? 0f : (boundaryMask[idx + 1] == 1 ? density[idx] : density[idx + 1]);

                            vx[idx] = MathUtil.fma(-PRESSURE_GRAD, pR - pL, vx[idx]);
                            vy[idx] = MathUtil.fma(-PRESSURE_GRAD, pU - pD, vy[idx]);

                            if (!Float.isFinite(vx[idx])) {
                                vx[idx] = 0f;
                            }
                            if (!Float.isFinite(vy[idx])) {
                                vy[idx] = 0f;
                            }
                            if (!Float.isFinite(density[idx])) {
                                density[idx] = 0f;
                            }
                        }
                    }
                }
            });
        }

        final boolean simplifyAdvection = simplifyThisFrame && (SIMPLIFY_TEMPERATURE_ADVECTION || SIMPLIFY_VELOCITY_ADVECTION);
        final int advectionParity = simplifyAdvection ? currentParity : -1;
        scope.submit(0, activeChunkCount, (startChunkIdx, endChunkIdx) -> {
            for (int i = startChunkIdx; i < endChunkIdx; i++) {
                int packed = activeChunkIndices[i];
                int cx = packed >> 16;
                int cy = packed & 0xFFFF;
                int chunkIdx = cx * chunksY + cy;
                int startX = cx << shift;
                int endX = Math.min(WORLD_WIDTH, (cx + 1) << shift);
                int startY = Math.max(0, cy << shift);
                int endY = Math.min(WORLD_HEIGHT, (cy + 1) << shift);

                boolean chunkGotAdvection = false;
                for (int x = startX; x < endX; x++) {
                    int xIdx = x * WORLD_HEIGHT;
                    for (int y = startY; y < endY; y++) {
                        int idx = xIdx + y;

                        if (blockSolid[idx] == 1 || boundaryMask[idx] == 1) {
                            tempsNext[idx] = (y == WORLD_HEIGHT - 1) ? 0f : temps[idx];
                            vxNext[idx] = 0;
                            vyNext[idx] = 0;
                            continue;
                        }

                        float vxHere = vx[idx];
                        float vyHere = vy[idx];
                        if (!Float.isFinite(vxHere) || !Float.isFinite(vyHere)) {
                            vxHere = 0f;
                            vyHere = 0f;
                        }

                        boolean cellAdvectionSimplified = simplifyAdvection && ((x + y) & 1) == advectionParity;
                        if (cellAdvectionSimplified) {
                            chunkGotAdvection = true;
                        }

                        float rawRayY = y - vyHere * DT;
                        float rayY = clamp(rawRayY, 0.5f, WORLD_HEIGHT - 1.5f);

                        float rayX = x - vxHere * DT;
                        rayX = wrapFloatX(rayX);

                        if (!cellAdvectionSimplified) {
                            tempsNext[idx] = sampleWithCorrection(temps, idx, x, y, rayX, rayY, true, temps[idx]);

                            if (simpleVAdvection) {
                                int srcX = Math.floorMod((int)(x - vxHere * DT + 0.5f), WORLD_WIDTH);
                                int srcY = clamp((int)(y - vyHere * DT + 0.5f), 1, WORLD_HEIGHT - 2);
                                int srcIdx = srcX * WORLD_HEIGHT + srcY;
                                if (blockSolid[srcIdx] == 1) {
                                    vxNext[idx] = 0;
                                    vyNext[idx] = 0;
                                } else {
                                    vxNext[idx] = vx[srcIdx] * DAMPING;
                                    vyNext[idx] = vy[srcIdx] * DAMPING;
                                }
                            } else {
                                vxNext[idx] = sampleWithCorrection(vx, idx, x, y, rayX, rayY, false, 0f);
                                vyNext[idx] = sampleWithCorrection(vy, idx, x, y, rayX, rayY, false, 0f);
                            }
                        } else {
                            if (SIMPLIFY_TEMPERATURE_ADVECTION) {
                                int srcX = Math.floorMod((int)(x - vxHere * DT + 0.5f), WORLD_WIDTH);
                                int srcY = clamp((int)(y - vyHere * DT + 0.5f), 0, WORLD_HEIGHT - 1);
                                int srcIdx = srcX * WORLD_HEIGHT + srcY;
                                tempsNext[idx] = blockSolid[srcIdx] == 1 ? temps[idx] : temps[srcIdx];
                            } else {
                                tempsNext[idx] = sampleWithCorrection(temps, idx, x, y, rayX, rayY, true, temps[idx]);
                            }

                            if (SIMPLIFY_VELOCITY_ADVECTION) {
                                int srcX = Math.floorMod((int)(x - vxHere * DT + 0.5f), WORLD_WIDTH);
                                int srcY = clamp((int)(y - vyHere * DT + 0.5f), 1, WORLD_HEIGHT - 2);
                                int srcIdx = srcX * WORLD_HEIGHT + srcY;
                                if (blockSolid[srcIdx] == 1) {
                                    vxNext[idx] = 0;
                                    vyNext[idx] = 0;
                                } else {
                                    vxNext[idx] = vx[srcIdx] * DAMPING;
                                    vyNext[idx] = vy[srcIdx] * DAMPING;
                                }
                            } else {
                                vxNext[idx] = sampleWithCorrection(vx, idx, x, y, rayX, rayY, false, 0f);
                                vyNext[idx] = sampleWithCorrection(vy, idx, x, y, rayX, rayY, false, 0f);
                            }
                        }
                    }
                }
                if (chunkGotAdvection) {
                    appliedMask[chunkIdx] |= MASK_ADVECTION;
                    advectionChunks.incrementAndGet();
                }
            }
        });

        swapTemps();
        swapVx();
        swapVy();

        long frameTime = System.nanoTime() - frameStartNanos;
        avgFrameTimeNs = avgFrameTimeNs * 0.9f + frameTime * 0.1f;

        if (frameTime < MAX_FRAME_BUDGET_NANOS * 0.8f) {
            consecutiveGoodFrames++;
        } else {
            consecutiveGoodFrames = 0;
        }

        if (consecutiveGoodFrames >= 3) {
            simplifyNextFrame = false;
            avgFrameTimeNs *= 0.5f;
            consecutiveGoodFrames = 0;
        } else {
            simplifyNextFrame = avgFrameTimeNs > MAX_FRAME_BUDGET_NANOS;
        }
    }

    private static void swapTemps() {
        float[] t = temps;
        temps = tempsNext;
        tempsNext = t;
    }

    private static void swapVx() {
        float[] t = vx;
        vx = vxNext;
        vxNext = t;
    }

    private static void swapVy() {
        float[] t = vy;
        vy = vyNext;
        vyNext = t;
    }

    private static void updateLivingEntity() {
        entityPool.forEachType(LivingEntity.class, emitter -> {
            if (!emitter.isEmitting()) {
                return;
            }
            int x = emitter.blockX();
            int y = emitter.blockY();
            int radius = emitter.heatRadius();
            markAreaActive(x, y, radius);

            int inBoundsCount = 0;
            float total = 0f;
            for (int i = 0; i < radius; i++) {
                for (int j = 0; j < radius; j++) {
                    if (world.inBounds(x + i, y + j)) {
                        total += temps[pos2index(x + i, y + j)];
                        inBoundsCount++;
                    }
                }
            }
            if (inBoundsCount == 0) {
                return;
            }
            float avgTemp = total / inBoundsCount;

            float exchange = (avgTemp - emitter.getCurrentTemp()) * emitter.heatTransfer();
            emitter.addTemp(exchange);

            float energyPerCell = -exchange / inBoundsCount;
            for (int i = 0; i < radius; i++) {
                for (int j = 0; j < radius; j++) {
                    if (world.inBounds(x + i, y + j)) {
                        addTemp(x + i, y + j, energyPerCell);
                    }
                }
            }
        });
    }

    public static float getRawPressure(int x, int y) {
        return density[pos2index(x, y)];
    }

    @Deprecated
    public static float getPressure(int x, int y) {
        return getRawPressure(x, y);
    }

    public static float getTempCell(int x, int y) {
        return temps[pos2index(x, y)];
    }

    public static float getTempCell(int x, int y, int radius) {
        float total = 0;
        int count = 0;
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                if (world.inBounds(x + i, y + j)) {
                    total += temps[pos2index(x + i, y + j)];
                    count++;
                }
            }
        }
        return count == 0 ? 0 : total / count;
    }

    public static void addTemp(int x, int y, float temp) {
        if (!world.inBounds(x, y)) {
            return;
        }
        temps[pos2index(x, y)] += temp;
        markAreaActive(x, y, 1);
    }

    public static void setTemp(int x, int y, float temp) {
        if (!world.inBounds(x, y)) {
            return;
        }
        temps[pos2index(x, y)] = temp;
        markAreaActive(x, y, 1);
    }

    public record Wind(float force, float angle) {
        public static final Wind CALM = new Wind(0f, 0f);

        public boolean isCalm() {
            return force < 0.001f;
        }
    }

    public static Wind getWind(int x, int y) {
        if (isFluidBoundary(x, y)) {
            return Wind.CALM;
        }
        int idx = pos2index(x, y);
        float cvx = vx[idx];
        float cvy = vy[idx];
        float forceSq = cvx * cvx + cvy * cvy;
        if (forceSq < VELOCITY_CUTOFF * VELOCITY_CUTOFF) {
            return Wind.CALM;
        }
        float force = (float) Math.sqrt(forceSq);
        float angle = (float) Math.toDegrees(Math.atan2(cvy, cvx));
        if (angle < 0f) {
            angle += 360f;
        }
        return new Wind(force, angle);
    }

    public static float[] getTemps() {
        return temps;
    }

    public static float[] getDensity() {
        return density;
    }

    public static float[] getVx() {
        return vx;
    }

    public static float[] getVy() {
        return vy;
    }
}