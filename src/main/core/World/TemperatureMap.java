package core.World;

import core.content.blocks.Block;
import core.util.BatchScope;
import core.util.Debug;

import java.util.Arrays;

import static core.Global.world;
import static core.World.WorldGenerator.WorldGeneratorConstants.COPY_SIZE;
import static java.lang.Math.clamp;

//счас по упрощенной модели - не считает диагонали, большие клампы
//возможно, в будущем появится галочка обычной или упрощенной модели
public final class TemperatureMap {
    //column-major массивы
    //Внешний цикл по x, внутренний по y
    private static float[] temps;
    private static float[] density;

    //ем выше, тем 'взрывоопаснее' газы (температура больше влияет на расширение)
    public static final float R = 250.05f;
    //todo почти все тут нужно делать динамически
    //Чем выше , тем быстрее выравнивается давление
    public static final float SIM_K = 0.2f;
    //Не дает улететь больше 20% газа из ячейки за апдейт
    public static final float MAX_FLOW_RESTR = 0.2f;
    //Если разница давлений меньше движение газа игнориртся ради экономии
    public static final float MIN_PRESSURE_DELTA = 0.001f;
    //Если газа останется меньше, то пустота
    public static final float MIN_DENSITY_THRESHOLD = 0.01f;
    //тепло сосед-сосед
    public static final float HEAT_DIFFUSION_K = 0.2f;
    //для статического напряжения (ну типа давление но в блоках)
    private static final float SOLID_BASE_PRESSURE = 10000.0f;
    //зависимость напряжения от температуры
    private static final float SOLID_SHRINK = 2.5f;

    //todo
    //Множитель теплоемкости блоков
    public static final float SOLID_HEAT_CAPACITY_MULT = 3f;
    //Множитель теплопроводности блоков
    public static final float SOLID_CONDUCTIVITY_MULT = 0.4f;

    public static int pos2index(int x, int y) { return x * world.sizeY + y; }

    public static void init() {
        temps = new float[world.sizeX * world.sizeY];
        density = new float[world.sizeX * world.sizeY];

        //todo
        Arrays.fill(temps, 50.0f);
        Arrays.fill(density, 1.2f);
    }

    public static void generate() {
        if (true)
            return;

        int sizeX = world.sizeX;
        int sizeY = world.sizeY;
        int spawnX = sizeX / 2;
        int spawnY = sizeY / 2;

        {
            final int halfSize = 40;
            int startX = clamp(spawnX - halfSize, 0, sizeY - 1);
            int endX   = clamp(spawnX + halfSize, 0, sizeX - 1);
            int startY = clamp(spawnY - halfSize, 0, sizeY - 1);
            int endY   = clamp(spawnY + halfSize, 0, sizeX - 1);

            for (int x = startX; x < endX; x++) {
                int baseIdx = x * sizeY + startY;
                for (int y = startY; y < endY; y++) {
                    temps[baseIdx] = 1000.0f;
                    density[baseIdx] = 7f;
                    baseIdx++;
                }
            }
        }

        var scope = new BatchScope(world.genPool);
        for (int i = 0; i < 2000; i++) {
            update(scope);
        }
        try (scope) {
            Debug.saveTemp("Temp", scope);
            Debug.savePressures("Pressure", scope);
            Debug.saveDens("Density", scope);
        }
        for (int i = 0; i < 10000; i++) {
            update(scope);
        }
        Debug.saveTemp("Temp1", null);
        Debug.savePressures("Pressure1", null);
        Debug.saveDens("Density1", null);
    }

    //чтоб не утекало в одну сторону, это нормальная практика
    private static boolean flip = false;

    public static void update(BatchScope scope) {
        //todo такт не надо, ведь точно можем узнать что где и когда но пока лень
        int sizeY = world.sizeY;
        int targetOffsetLeft = (world.sizeX - COPY_SIZE) * sizeY;

        for (int x = 0; x < COPY_SIZE; x++) {
            int srcOffset = x * sizeY;
            int targetOffsetRight = targetOffsetLeft + srcOffset;

            // Копируем левый край на правый
            System.arraycopy(temps, srcOffset, temps, targetOffsetRight, sizeY);
            System.arraycopy(density, srcOffset, density, targetOffsetRight, sizeY);
        }

        for (int x = world.sizeX - COPY_SIZE + 1; x < world.sizeX; x++) {
            int srcOffset = x * sizeY;
            int targetOffsetLeftBound = (x - (world.sizeX - COPY_SIZE)) * sizeY;

            // Копируем правый край на левый
            System.arraycopy(temps, srcOffset, temps, targetOffsetLeftBound, sizeY);
            System.arraycopy(density, srcOffset, density, targetOffsetLeftBound, sizeY);
        }


        flip = !flip;

        final int leftBorder = 0;
        final int rightBorder = world.sizeX - COPY_SIZE;
        final int activeWidth = rightBorder - leftBorder;

        final boolean currentFlip = flip;

        //оно должно ломаться, ведь итерации созависимы, но охренеть не ломается

        try (scope) {

            if (currentFlip) {
                scope.submit(leftBorder, rightBorder, (startX, endX) -> {
                    for (int i = startX; i < endX; i++) {
                        int end = world.sizeY - 1;
                        for (int j = 0; j < end; j++) {
                            processHorizontalFlow(i, j, leftBorder, activeWidth);
                            processVerticalFlow(i, j);
                        }
                        processHorizontalFlow(i, end, leftBorder, activeWidth);
                    }
                });
            } else {
                scope.submit(leftBorder, rightBorder, (startX, endX) -> {
                    for (int i = endX - 1; i >= startX; i--) {
                        int start = world.sizeY - 1;
                        processHorizontalFlow(i, start, leftBorder, activeWidth);
                        for (int j = start - 1; j >= 0; j--) {
                            processHorizontalFlow(i, j, leftBorder, activeWidth);
                            processVerticalFlow(i, j);
                        }
                    }
                });
            }
        }
    }

    private static void processHorizontalFlow(int i, int j, int leftBorder, int activeWidth) {
        int nextI = leftBorder + ((i - leftBorder + 1) % activeWidth);

        int idx = i * world.sizeY + j;
        int nextIdx = nextI * world.sizeY + j;

        float tIdx = temps[idx];
        float tNextIdx = temps[nextIdx];

        float deltaT = tIdx - tNextIdx;
        if (Math.abs(deltaT) > 0.01f) {
            float conduct = getConductivity(i, j, nextI, j);
            float ht = deltaT * HEAT_DIFFUSION_K * conduct;

            tIdx -= ht / getHeatCapacity(i, j);
            tNextIdx += ht / getHeatCapacity(nextI, j);
        }

        if (world.isBlockType(i, j, Block.Type.SOLID) || world.isBlockType(nextI, j, Block.Type.SOLID)) {
            temps[idx] = tIdx;
            temps[nextIdx] = tNextIdx;
            return;
        }

        float pLeft = getPressure(i, j);
        float pRight = getPressure(nextI, j);
        float deltaP = pLeft - pRight;

        if (Math.abs(deltaP) > MIN_PRESSURE_DELTA) {
            float flow = deltaP * SIM_K;

            boolean isLeftToRight = flow > 0;
            int srcIdx = isLeftToRight ? idx : nextIdx;
            int dstIdx = isLeftToRight ? nextIdx : idx;

            float tSrc = isLeftToRight ? tIdx : tNextIdx;
            float tDst = isLeftToRight ? tNextIdx : tIdx;

            float dSrc = density[srcIdx];
            float dDst = density[dstIdx];

            float absFlow = Math.abs(flow);

            float maxFlow = dSrc * MAX_FLOW_RESTR;
            if (absFlow > maxFlow) {
                absFlow = maxFlow;
            }

            float energy    = absFlow * tSrc;
            float energySrc = (dSrc * tSrc) - energy;
            float energyDst = (dDst * tDst) + energy;

            dSrc -= absFlow;
            dDst += absFlow;

            if (dSrc > MIN_DENSITY_THRESHOLD) {
                tSrc = energySrc / dSrc;
            }
            if (dDst > MIN_DENSITY_THRESHOLD) {
                tDst = energyDst / dDst;
            }

            density[srcIdx] = dSrc;
            density[dstIdx] = dDst;

            if (isLeftToRight) {
                tIdx = tSrc;
                tNextIdx = tDst;
            } else {
                tNextIdx = tSrc;
                tIdx = tDst;
            }
        }

        temps[idx] = tIdx;
        temps[nextIdx] = tNextIdx;
    }


    private static void processVerticalFlow(int i, int j) {
        int idx = i * world.sizeY + j;
        int nextIdx = idx + 1;

        float tIdx = temps[idx];
        float tNextIdx = temps[nextIdx];

        float deltaT = tIdx - tNextIdx;
        if (Math.abs(deltaT) > 0.01f) {
            float conduct = getConductivity(i, j, i, j + 1);
            float heatTransfer = deltaT * HEAT_DIFFUSION_K * conduct;

            tIdx -= heatTransfer / getHeatCapacity(i, j);
            tNextIdx += heatTransfer / getHeatCapacity(i, j + 1);
        }

        if (world.isBlockType(i, j, Block.Type.SOLID) || world.isBlockType(i, j + 1, Block.Type.SOLID)) {
            temps[idx] = tIdx;
            temps[nextIdx] = tNextIdx;
            return;
        }

        float pUp = getPressure(i, j);
        float pDown = getPressure(i, j + 1);
        float deltaP = pUp - pDown;

        if (Math.abs(deltaP) > MIN_PRESSURE_DELTA) {
            float flow = deltaP * SIM_K;

            boolean isDownwards = flow > 0;
            int srcIdx = isDownwards ? idx : nextIdx;
            int dstIdx = isDownwards ? nextIdx : idx;

            float tSrc = isDownwards ? tIdx : tNextIdx;
            float tDst = isDownwards ? tNextIdx : tIdx;

            float dSrc = density[srcIdx];
            float dDst = density[dstIdx];

            float absFlow = Math.abs(flow);

            float maxFlow = dSrc * MAX_FLOW_RESTR;
            if (absFlow > maxFlow) {
                absFlow = maxFlow;
            }

            float energy = absFlow * tSrc;
            float energySrc = (dSrc * tSrc) - energy;
            float energyDst = (dDst * tDst) + energy;

            dSrc -= absFlow;
            dDst += absFlow;

            if (dSrc > MIN_DENSITY_THRESHOLD) {
                tSrc = energySrc / dSrc;
            }
            if (dDst > MIN_DENSITY_THRESHOLD) {
                tDst = energyDst / dDst;
            }

            density[srcIdx] = dSrc;
            density[dstIdx] = dDst;

            if (isDownwards) {
                tIdx = tSrc;
                tNextIdx = tDst;
            } else {
                tNextIdx = tSrc;
                tIdx = tDst;
            }
        }

        temps[idx] = tIdx;
        temps[nextIdx] = tNextIdx;
    }


    private static float getHeatCapacity(int x, int y) {
        if (world.isBlockType(x, y, Block.Type.SOLID)) {
            return SOLID_HEAT_CAPACITY_MULT;
        }
        return Math.max(MIN_DENSITY_THRESHOLD, density[pos2index(x, y)]);
    }

    private static float getConductivity(int x1, int y1, int x2, int y2) {
        float c1 = world.isBlockType(x1, y1, Block.Type.SOLID) ? SOLID_CONDUCTIVITY_MULT : 1.0f;
        float c2 = world.isBlockType(x2, y2, Block.Type.SOLID) ? SOLID_CONDUCTIVITY_MULT : 1.0f;

        return Math.min(c1, c2);
    }

    //todo еще где то тут должен считаться/читаться предел прочности, чтоб
    //материал мог реагировать на слишком большое внутреннее напряжение (давление)

    //не семплируется!!!! если нужно узнать в конкретной точке
    //то надо брать несколько и сглаживать
    public static float getDensity(int x, int y) {
        if (world.isBlockType(x, y, Block.Type.SOLID)) {
            //todo
            return 5;
        }
        return density[pos2index(x, y)];
    }

    public static float getPressure(int x, int y) {
        int idx = pos2index(x, y);
        if (world.isBlockType(x, y, Block.Type.SOLID)) {
            return SOLID_BASE_PRESSURE * pow_2_5((temps[idx] + 273.15f) / 273.15f);
        }

        return density[idx] * (temps[idx] + 273.15f) * R;
    }

    /// {@link TemperatureMap#SOLID_SHRINK}
    private static float pow_2_5(float base) {
        return (float)((double)base * base * Math.sqrt(base));
    }

    public static float getTempCell(int x, int y) {
        return temps[pos2index(x, y)];
    }
}
