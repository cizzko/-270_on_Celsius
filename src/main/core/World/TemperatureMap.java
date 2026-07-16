package core.World;

import core.Application;
import core.content.blocks.Block;
import core.content.entity.LivingEntity;
import core.util.BatchScope;
import core.util.Debug;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.Arrays;

import static core.Global.*;
import static core.World.WorldGenerator.WorldGeneratorConstants.*;
import static core.World.WorldGenerator.WorldGeneratorTMP.generateCave;
import static java.lang.Math.clamp;

//это намеренно искаженная физика,
//она должна работать красиво, а не правильно

//счас по упрощенной модели
//возможно, в будущем появится галочка обычной или упрощенной
public final class TemperatureMap {
    // Column-major массив температур
    private static float[] temps;
    //private static final VarHandle TS_ARRAY = MethodHandles.arrayElementVarHandle(float[].class);

    private static IntArrayList activeChunks = new IntArrayList();
    private static int[] chunkActiveOnFrame;
    private static int currentFrame = 0;

    //todo
    private static final float BASE_DENSITY = 1.2f;
    //чем выше, тем 'взрывоопаснее' газы (температура больше влияет на расширение)
    private static final float R = 350.05f;
    //тепло сосед-сосед
    //тестовое значение
    private static final float HEAT_DIFFUSION_K = 0.006f;
    private static final float SOLID_BASE_PRESSURE = 10000.0f;
    //Множитель теплоемкости блоков
    private static final float SOLID_HEAT_CAPACITY_MULT = 3f;
    //Множитель теплопроводности блоков
    //тестовое значение
    private static final float SOLID_CONDUCTIVITY_MULT = 0.01f;
    //верт тяга
    //вообще это были разные значения, но вышло что cs+gs
    private static final float cs = 20f;
    private static final float gs = 20f;
    //как быстро газ разлетается вбок всегда
    //todo @test подобрать
    private static final float hb = 2.4f;
    //бустер скорости разлета вбок у пола/потолка (растекание)
    //todo @test подобрать
    private static final float hs = 15f;

    public static int pos2index(int x, int y) { return x * world.sizeY + y; }

    public static void init() {
        temps = new float[world.sizeX * world.sizeY];
        Arrays.fill(temps, 20f);

        chunkActiveOnFrame = new int[((world.sizeX + 15) >> 4) * ((world.sizeY + 15) >> 4)];
        Arrays.fill(chunkActiveOnFrame, -1);
    }

    public static void generate() {
        int sizeX = world.sizeX;
        int sizeY = world.sizeY;
        int spawnX = sizeX / 2 - 100;
        int spawnY = sizeY / 2 + sizeY / 8;

        boolean truе = true;
        if (truе) {
            return;
        }

        int halfSize = 20;
        int startX = clamp(spawnX - halfSize * 3, 0, sizeY - 1);
        int endX = clamp(spawnX + halfSize * 3, 0, sizeX - 1);
        int startY = clamp(spawnY - halfSize, 0, sizeY - 1);
        int endY = clamp(spawnY + halfSize, 0, sizeX - 1);

        generateCave(startX + halfSize/2, startY + halfSize/2,
                3, 2, 5,
                CAVE_UPPER_MIN_ANGLE, CAVE_UPPER_MAX_ANGLE, 120, 200);

        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                setTemp(x, y, -250);
            }
        }

        var scope = new BatchScope(world.genPool);
        for (int i = 0; i < 1000; i++) {
            update(scope);
        }
        try (scope) {
            Debug.saveTemp("Temp", scope);
            Debug.savePressures("Pressure", scope);
            Debug.saveWindForce("Power", scope);
            Debug.saveWindDirection("Dir", scope);
        }
        for (int i = 0; i < 4000; i++) {
            update(scope);
        }
        Debug.saveTemp("Temp1", null);
        Debug.savePressures("Pressure1", null);
        Debug.saveWindForce("Power1", scope);
        Debug.saveWindDirection("Dir1", scope);
    }

    //казалось бы кандидат для класса физики
    //но добавление и работа с температурой могут сломаться
    //если будут разные фпс работы потоков
    private static void updateLivingEntity() {
        entityPool.forEachType(LivingEntity.class, emitter -> {
            if (!emitter.isEmitting()) {
                return;
            }

            int x = emitter.blockX();
            int y = emitter.blockY();
            int radius = emitter.heatRadius();

            //мне лень выносить в финал поля
            //загадочное п
            float P = 1.0f;
            //теплообмен
            float k = emitter.heatTransfer();
            //температура отказа грелки (метаболизма)
            float L = 29.0f;
            //бонус грелки
            float S_teep = 1.2f;
            //крутизна при отказе грелки
            float F_steep = 4.0f;
            //таргет
            float T_target = 36.6f;

            float currentHeat = emitter.getCurrentTemp();
            float overHeatFactor = 1.0f - (currentHeat - 33.0f) / 9.0f;
            //от неловкого нуля
            float P_dyn = Math.max(0.02f, P * overHeatFactor);
            //0.05 это метаболизм вообще всегда
            float baseMetabolism = 0.05f * P_dyn;
            //0.95 это бонусный метаболизм только когда холодно
            float maxBonusPower = 0.95f * P_dyn;
            float delta = T_target - currentHeat;
            //можно лениво поджать хвосты и не использовать ехп, но без тестов не поймешь)
            float curveFactor = (float) (1.0 / (1.0 + Math.exp(-S_teep * delta)));
            float metabolism = baseMetabolism + (maxBonusPower * curveFactor);
            float exhaustionFactor = (float) (1.0 / (1.0 + Math.exp(-(currentHeat - L) * F_steep)));

            metabolism *= exhaustionFactor;

            float environmentTemp = getTempCell(x, y, radius);
            float exchange = (environmentTemp - currentHeat) * k;
            float totalTemperatureDelta = metabolism + exchange;

            //тепло внутри ентити (нагрев от метаболизма и зависимость от среды)
            emitter.addTemp(totalTemperatureDelta);
            int countedCells = 0;
            for (int i = 0; i < radius; i++) {
                for (int j = 0; j < radius; j++) {
                    if (world.inBounds(x + i, y + j)) {
                        countedCells++;
                    }
                }
            }

            if (countedCells > 0) {
                float energyPerCell = -exchange / countedCells;
                for (int i = 0; i < radius; i++) {
                    for (int j = 0; j < radius; j++) {
                        int targetX = x + i;
                        int targetY = y + j;
                        if (world.inBounds(targetX, targetY)) {
                            float capacity = getHeatCapacity(targetX, targetY);
                            float deltaTemp = energyPerCell / capacity;

                            //нагрев окружающей среды сущностью
                            addTemp(targetX, targetY, deltaTemp);
                        }
                    }
                }
            }
        });
    }

    //todo было бы неплохо иметь итератор по миру, принимающий в параметр какие блоки выплевывать
    // один раз прошел -> закешировал нужные,
    // если мир изменился (ивенты помогут) меняет конкретный в кеше

    //todo очень хотелось бы ивентов на блокдейстрой и креат, чтоб изолироваться от world
    // и сделать нормально и красиво

    //я не буду это делать пока нету любого из верхних условий мне пофек
    private static void updateBlocks() {

    }

    //чтоб не утекало в одну сторону, это нормальная практика
    private static boolean flip = false;

    public static void update(BatchScope scope) {
        updateLivingEntity();

        //todo такт не надо, ведь точно можем узнать что где и когда но пока лень
        int sizeY = world.sizeY;
        int targetOffsetLeft = (world.sizeX - COPY_SIZE) * sizeY;

        for (int x = 0; x < COPY_SIZE; x++) {
            int srcOffset = x * sizeY;
            int targetOffsetRight = targetOffsetLeft + srcOffset;
            System.arraycopy(temps, srcOffset, temps, targetOffsetRight, sizeY);
        }

        flip = !flip;
        currentFrame++;

        //честно я хз как оно работает, по идее не должно было
        //todo чуть сломан спавн ветра, на температуру не влияет
        for (int c = 0; c < activeChunks.size(); c++) {
            int packedChunk = activeChunks.getInt(c);
            int x = packedChunk >> 16;
            int y = packedChunk & 0xFFFF;

            int startI = x << 4;
            int startJ = y << 4;

            int endI = Math.min(startI + 16, world.sizeX);
            int endJ = Math.min(startJ + 16, world.sizeY);

            if (endI < world.sizeX) {
                for (int j = startJ; j < endJ; j++) {
                    float current = temps[pos2index(endI - 1, j)];
                    float neighbor = temps[pos2index(endI, j)];

                    if (Math.abs(current - neighbor) > 0.01f) {
                        activateCell(endI, j);
                        break;
                    }
                }
            }

            if (startI > 0) {
                for (int j = startJ; j < endJ; j++) {
                    float current = temps[pos2index(startI, j)];
                    float neighbor = temps[pos2index(startI - 1, j)];

                    if (Math.abs(current - neighbor) > 0.01f) {
                        activateCell(startI - 1, j);
                        break;
                    }
                }
            }

            if (endJ < world.sizeY) {
                for (int i = startI; i < endI; i++) {
                    float current = temps[pos2index(i, endJ - 1)];
                    float neighbor = temps[pos2index(i, endJ)];

                    if (Math.abs(current - neighbor) > 0.01f) {
                        activateCell(i, endJ);
                        break;
                    }
                }
            }

            if (startJ > 0) {
                for (int i = startI; i < endI; i++) {
                    float current = temps[pos2index(i, startJ)];
                    float neighbor = temps[pos2index(i, startJ - 1)];

                    if (Math.abs(current - neighbor) > 0.01f) {
                        activateCell(i, startJ - 1);
                        break;
                    }
                }
            }
        }

        int numActive = activeChunks.size();
        int activeWidth = world.sizeX - COPY_SIZE;

        try (scope) {
            scope.submit(0, numActive, (startChunkIdx, endChunkIdx) -> {
                if (flip) {
                    for (int c = startChunkIdx; c < endChunkIdx; c++) {
//                        int ah = c + 4;
//                        if (ah < endChunkIdx) {
//                            int futurePacked = activeChunks.getInt(ah);
//                            int fX = futurePacked >> 16;
//                            int fY = futurePacked & 0xFFFF;
//                            int futureIdx = pos2index(fX << 4, fY << 4);
//
//                            if (futureIdx < temps.length) {
//                                //TS_ARRAY.getOpaque(temps, futureIdx);
//                            }
//                        }
                        processSingleChunk(c, activeWidth);
                    }
                } else {
                    for (int c = endChunkIdx - 1; c >= startChunkIdx; c--) {
                        processSingleChunk(c, activeWidth);
                    }
                }
            });
        }
        cleanupSleepingChunks();
    }

    public static void activateCell(int x, int y) {
        if (x < 0 || y < 0 || x >= world.sizeX || y >= world.sizeY) return;

        int chunkX = x >> 4;
        int chunkY = y >> 4;
        int idx = chunkY * ((world.sizeX + 15) >> 4) + chunkX;

        if (chunkActiveOnFrame[idx] != currentFrame) {
            chunkActiveOnFrame[idx] = currentFrame;

            int packedChunk = (chunkX << 16) | (chunkY & 0xFFFF);
            activeChunks.add(packedChunk);
        }
    }

    private static void cleanupSleepingChunks() {
        for (int c = 0; c < activeChunks.size(); c++) {
            int packedChunk = activeChunks.getInt(c);
            int x = packedChunk >> 16;
            int y = packedChunk & 0xFFFF;

            if (chunkActiveOnFrame[y * ((world.sizeX + 15) >> 4) + x] == -1) {
                int lastChunk = activeChunks.removeInt(activeChunks.size() - 1);
                if (c < activeChunks.size()) {
                    activeChunks.set(c, lastChunk);
                    c--;
                }
            }
        }
    }

    private static void processSingleChunk(int c, int activeWidth) {
        int packedChunk = activeChunks.getInt(c);
        int x = packedChunk >> 16;
        int y = packedChunk & 0xFFFF;

        int fromX = x << 4;
        int toX = Math.min(fromX + 16, world.sizeX);
        int fromY = y << 4;
        int toY = Math.min(fromY + 16, world.sizeY);
        boolean hasActivity = false;

        if (flip) {
            for (int i = fromX; i < toX; i++) {
                for (int j = fromY; j < toY; j++) {
                    if (deltaSmall(i, j)) {
                        continue;
                    }

                    hasActivity |= processHorizontalFlow(i, j, activeWidth) | processVerticalFlow(i, j);
                }
            }
        } else {
            for (int i = toX - 1; i >= fromX; i--) {
                for (int j = toY - 1; j >= fromY; j--) {
                    if (deltaSmall(i, j)) {
                        continue;
                    }

                    //'|=' заменяет if (!hasActivity && ..) {.. hasActivity = true; }
                    hasActivity |= processHorizontalFlow(i, j, activeWidth) | processVerticalFlow(i, j);
                }
            }
        }

        int idx = y * ((world.sizeX + 15) >> 4) + x;
        if (hasActivity) {
            chunkActiveOnFrame[idx] = currentFrame + 1;
        } else {
            chunkActiveOnFrame[idx] = -1;
        }
    }

    private static boolean deltaSmall(int i, int j) {
        int idx = pos2index(i, j);
        float tIdx = temps[idx];
        int nextI = i + 1;
        int next = j + 1;

        return (Math.abs(tIdx - temps[nextI * world.sizeY + j]) <= 0.01f) & ((next >= world.sizeY) | (Math.abs(tIdx - temps[(next < world.sizeY) ? (idx + 1) : idx]) <= 0.01f));
    }

    private static boolean processHorizontalFlow(int i, int j, int activeWidth) {
        if (activeWidth <= 0) {
            return false;
        }

        //todo метод возвращающий сразу трушные координаты
        int nextI = ((i + 1) % activeWidth);
        int idx = pos2index(i, j);
        int nextIdx = nextI * world.sizeY + j;

        float tIdx = temps[idx];
        float tNextIdx = temps[nextIdx];
        float deltaT = tIdx - tNextIdx;
        float absDeltaT = Math.abs(deltaT);

        boolean isSolidCurrent = world.isBlockType(i, j, Block.Type.SOLID);
        boolean isSolidNext = world.isBlockType(nextI, j, Block.Type.SOLID);

        float conduct = getConductivity(i, j, nextI, j);
        float capIdx = getHeatCapacity(i, j);
        float capNextIdx = getHeatCapacity(nextI, j);

        float factor = HEAT_DIFFUSION_K;
        boolean solidBelowCurrent = (j > 0) && world.isBlockType(i, j - 1, Block.Type.SOLID);
        boolean solidBelowNext = (j > 0) && world.isBlockType(nextI, j - 1, Block.Type.SOLID);
        boolean hasFloor = solidBelowCurrent || solidBelowNext;

        if (!isSolidCurrent && !isSolidNext) {
            if (absDeltaT > 0.5f) {
                //что то около 500 градусов разницы
                float saturation = Math.min(1f, absDeltaT * 0.002f);
                float dynamicHs = 1f + (hs - 1f) * saturation;
                factor = hasFloor ? (hb * dynamicHs * HEAT_DIFFUSION_K) : (hb * HEAT_DIFFUSION_K);
            }
        }

        float ht = deltaT * factor * conduct;
        if (hasFloor && !isSolidCurrent && !isSolidNext) {
            ht *= 0.1f;
        }

        float sumCap = capIdx + capNextIdx;
        float trns = (absDeltaT * 0.45f) * (capIdx * capNextIdx * 1.0f / sumCap);
        if (Math.abs(ht) > trns) {
            ht = (ht > 0) ? trns : -trns;
        }

        float cap = ht / sumCap;
        temps[idx] = tIdx - (cap * capNextIdx);
        temps[nextIdx] = tNextIdx + (cap * capIdx);

        return true;
    }

    private static final float VERT = (gs + cs) * 0.5f;
    private static boolean processVerticalFlow(int i, int j) {
        if (j >= world.sizeY - 1) {
            return false;
        }

        int idx = pos2index(i, j);
        int nextIdx = idx + 1;

        float tIdx = temps[idx];
        float tNextIdx = temps[nextIdx];
        float deltaT = tIdx - tNextIdx;
        float absDeltaT = Math.abs(deltaT);

        boolean isSolidCurrent = world.isBlockType(i, j, Block.Type.SOLID);
        boolean isSolidNext = world.isBlockType(i, j + 1, Block.Type.SOLID);

        float conduct = getConductivity(i, j, i, j + 1);
        float capIdx = getHeatCapacity(i, j);
        float capNextIdx = getHeatCapacity(i, j + 1);
        float factor = HEAT_DIFFUSION_K;

        if (!isSolidCurrent && !isSolidNext) {
            float verticalFactor = absDeltaT / (5f + absDeltaT);

            if (deltaT > 0) {
                factor = VERT * verticalFactor;
            }
        }

        float ht = deltaT * factor * conduct;
        float sum = capIdx + capNextIdx;
        float trns = (absDeltaT * 0.45f) * (capIdx * capNextIdx * 1.0f / sum);
        if (Math.abs(ht) > trns) {
            ht = (ht > 0) ? trns : -trns;
        }

        float cap = ht / sum;
        temps[idx] = tIdx - (cap * capNextIdx);
        temps[nextIdx] = tNextIdx + (cap * capIdx);

        return true;
    }

    private static float getHeatCapacity(int x, int y) {
        if (world.isBlockType(x, y, Block.Type.SOLID)) {
            return SOLID_HEAT_CAPACITY_MULT;
        }
        return BASE_DENSITY;
    }

    //todo очень много вызовов isBlockType() в коде
    private static float getConductivity(int x1, int y1, int x2, int y2) {
        float c1 = world.isBlockType(x1, y1, Block.Type.SOLID) ? SOLID_CONDUCTIVITY_MULT : 1f;
        float c2 = world.isBlockType(x2, y2, Block.Type.SOLID) ? SOLID_CONDUCTIVITY_MULT : 1f;
        return Math.min(c1, c2);
    }

    @Deprecated
    public static float getPressure(int x, int y) {
        int idx = pos2index(x, y);
        if (world.isBlockType(x, y, Block.Type.SOLID)) {
            return SOLID_BASE_PRESSURE * pow_2_5((temps[idx] + 273.15f) / 273.15f);
        }
        return BASE_DENSITY * (temps[idx] + 273.15f) * R;
    }

    private static float pow_2_5(float base) {
        return (float)((double)base * base * Math.sqrt(base));
    }

    public static float getTempCell(int x, int y) {
        return temps[pos2index(x, y)];
    }

    /// @return средняя по радиусу
    public static float getTempCell(int x, int y, int radius) {
        float total = 0;

        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                int targetX = x + i;
                int targetY = y + j;
                if (world.inBounds(targetX, targetY)) {
                    total += temps[pos2index(targetX, targetY)];
                }
            }
        }
        return total / (radius * radius);
    }

    public static void addTemp(int x, int y, float temp) {
        temps[pos2index(x, y)] += temp;
        activateCell(x, y);
    }

    public static void setTemp(int x, int y, float temp) {
        temps[pos2index(x, y)] = temp;
        activateCell(x, y);
    }

    //ничего лучше не было придумано, потому что обычно используется в паре
    public record Wind(float force, float angle) {
        public static final Wind CALM = new Wind(0.0f, 0.0f);
    }

    public static Wind getWind(int i, int j) {
        if ((j < 0 || j >= world.sizeY) || world.isBlockType(i, j, Block.Type.SOLID)) {
            return Wind.CALM;
        }

        int nextI = (i + 1) % world.sizeX;
        float windX = -calculateHorizontalFlowAmount(i, j, nextI);
        float windY = 0f;

        if (j < world.sizeY - 1) {
            windY = -calculateVerticalFlowAmount(i, j);
        }
        if (Math.abs(windX) < 0.001f && Math.abs(windY) < 0.001f) {
            return Wind.CALM;
        }

        float force = (float) Math.sqrt(windX * windX + windY * windY);
        float angle = (float) Math.toDegrees(Math.atan2(windY, windX));

        if (angle < 0) {
            angle += 360.0f;
        }

        return new Wind(force, angle);
    }

    //думать сложно, скопировать свой кусок проще, потом повыношу
    private static float calculateHorizontalFlowAmount(int i, int j, int nextI) {
        boolean isSolidCurrent = world.isBlockType(i, j, Block.Type.SOLID);
        boolean isSolidNext = world.isBlockType(nextI, j, Block.Type.SOLID);

        if (isSolidCurrent || isSolidNext) {
            return 0f;
        }

        int idx = pos2index(i, j);
        int nextIdx = pos2index(nextI, j);

        float deltaT = temps[idx] - temps[nextIdx];
        if (Math.abs(deltaT) <= 0.01f) {
            return 0f;
        }

        boolean hasFloor = j > 0 && (world.isBlockType(i, j - 1, Block.Type.SOLID) || world.isBlockType(nextI, j - 1, Block.Type.SOLID));

        float factor = HEAT_DIFFUSION_K;
        float absDeltaT = Math.abs(deltaT);
        if (absDeltaT > 0.5f) {
            float dynamicHs = 1f + (hs - 1f) * (absDeltaT / (5f + absDeltaT));
            factor = hasFloor ? (hb * dynamicHs * HEAT_DIFFUSION_K) : (hb * HEAT_DIFFUSION_K);
        }

        float ht = deltaT * factor * getConductivity(i, j, nextI, j);

        if (hasFloor) {
            ht *= 0.1f;
        }

        float capIdx = getHeatCapacity(i, j);
        float capNextIdx = getHeatCapacity(nextI, j);
        float trns = (Math.abs(deltaT) * 0.45f) * ((capIdx * capNextIdx) / (capIdx + capNextIdx));

        if (Math.abs(ht) > trns) {
            ht = Math.signum(ht) * trns;
        }
        return ht;
    }

    private static float calculateVerticalFlowAmount(int i, int j) {
        boolean isSolidCurrent = world.isBlockType(i, j, Block.Type.SOLID);
        boolean isSolidNext = world.isBlockType(i, j + 1, Block.Type.SOLID);

        if (isSolidCurrent || isSolidNext) {
            return 0f;
        }

        int idx = pos2index(i, j);
        float deltaT = temps[idx] - temps[idx + 1];

        if (Math.abs(deltaT) <= 0.01f) {
            return 0f;
        }

        float factor = HEAT_DIFFUSION_K;
        float absDelta = Math.abs(deltaT);
        if (deltaT > 0) {
            factor = ((gs + cs) / 2f) * absDelta / (5f + absDelta);
        }

        float heatTransfer = deltaT * factor * getConductivity(i, j, i, j + 1);
        float capIdx = getHeatCapacity(i, j);
        float capNextIdx = getHeatCapacity(i, j + 1);
        float trns = (Math.abs(deltaT) * 0.45f) * ((capIdx * capNextIdx) / (capIdx + capNextIdx));

        if (Math.abs(heatTransfer) > trns) {
            heatTransfer = Math.signum(heatTransfer) * trns;
        }
        return heatTransfer;
    }
}