package core.World;

import core.content.blocks.Block;
import core.util.BatchScope;
import core.util.Debug;

import java.util.Arrays;

import static core.Global.world;
import static core.World.WorldGenerator.WorldGeneratorConstants.COPY_SIZE;
import static java.lang.Math.clamp;

//это намеренно искаженная физика,
//она должна работать красиво, а не правильно

//счас по упрощенной модели
//возможно, в будущем появится галочка обычной или упрощенной
public final class TemperatureMap {
    // Column-major массив температур
    private static float[] temps;

    //todo
    private static final float BASE_DENSITY = 1.2f;
    private static final float SOLID_DENSITY = 5f;
    //чем выше, тем 'взрывоопаснее' газы (температура больше влияет на расширение)
    private static final float R = 250.05f;
    //тепло сосед-сосед
    //todo тестовое значение
    private static final float HEAT_DIFFUSION_K = 0.01f;
    private static final float SOLID_BASE_PRESSURE = 10000.0f;
    //Множитель теплоемкости блоков
    private static final float SOLID_HEAT_CAPACITY_MULT = 3f;
    //Множитель теплопроводности блоков
    //todo тестовое значение
    private static final float SOLID_CONDUCTIVITY_MULT = 0.01f;
    //не хватило бюджета, см. по контексту
    private static final float cs = 14f;
    private static final float gs = 14f;
    private static final float hb = 2.3f;
    private static final float hs = 4f;

    public static int pos2index(int x, int y) { return x * world.sizeY + y; }

    public static void init() {
        temps = new float[world.sizeX * world.sizeY];
        Arrays.fill(temps, 20f);
    }

    public static void generate() {
        int sizeX = world.sizeX;
        int sizeY = world.sizeY;
        int spawnX = sizeX / 2 - 100;
        int spawnY = sizeY / 2 - sizeY / 8;

        boolean truе = true;
        if (truе) {
            return;
        }

        int halfSize = 40;
        int startX = clamp(spawnX - halfSize, 0, sizeY - 1);
        int endX = clamp(spawnX + halfSize, 0, sizeX - 1);
        int startY = clamp(spawnY - halfSize, 0, sizeY - 1);
        int endY = clamp(spawnY + halfSize, 0, sizeX - 1);

        for (int x = startX; x < endX; x++) {
            int baseIdx = x * sizeY + startY;
            for (int y = startY; y < endY; y++) {
                temps[baseIdx] = 350f;
                baseIdx++;
            }
        }

        var scope = new BatchScope(world.genPool);
        for (int i = 0; i < 2000; i++) {
            update(scope);
        }
        try (scope) {
            Debug.saveTemp("Temp", scope);
            Debug.savePressures("Pressure", scope);
        }
        for (int i = 0; i < 10000; i++) {
            update(scope);
        }
        Debug.saveTemp("Temp1", null);
        Debug.savePressures("Pressure1", null);
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
            System.arraycopy(temps, srcOffset, temps, targetOffsetRight, sizeY);
        }

        for (int x = world.sizeX - COPY_SIZE + 1; x < world.sizeX; x++) {
            int srcOffset = x * sizeY;
            int targetOffsetLeftBound = (x - (world.sizeX - COPY_SIZE)) * sizeY;
            System.arraycopy(temps, srcOffset, temps, targetOffsetLeftBound, sizeY);
        }

        flip = !flip;

        int leftBorder = 0;
        int rightBorder = world.sizeX - COPY_SIZE;
        int activeWidth = rightBorder - leftBorder;

        //оно должно ломаться, ведь итерации созависимы, но охренеть не ломается
        try (scope) {
            if (flip) {
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

        if (Math.abs(deltaT) <= 0.01f)  {
            return;
        }

        boolean isSolidCurrent = world.isBlockType(i, j, Block.Type.SOLID);
        boolean isSolidNext = world.isBlockType(nextI, j, Block.Type.SOLID);

        float conduct = getConductivity(i, j, nextI, j);
        float capIdx = getHeatCapacity(i, j);
        float capNextIdx = getHeatCapacity(nextI, j);

        float factor = HEAT_DIFFUSION_K;

        if (!isSolidCurrent && !isSolidNext) {
            if (tIdx < -1f || tNextIdx < -1f) {
                boolean solidBelowCurrent = (j > 0) && world.isBlockType(i, j - 1, Block.Type.SOLID);
                boolean solidBelowNext = (j > 0) && world.isBlockType(nextI, j - 1, Block.Type.SOLID);
                factor = (solidBelowCurrent || solidBelowNext) ? (hb * 4.5f) : (hb * 0.4f);
            } else if (tIdx > 1f || tNextIdx > 1f) {
                factor = hb * hs;
            }
        }

        float ht = deltaT * factor * conduct;
        if (j > 0 && world.isBlockType(i, j - 1, Block.Type.SOLID) && (tIdx < 0 || tNextIdx < 0)) {
            ht *= 0.1f;
        }

        float maxSafeTransfer = (Math.abs(deltaT) * 0.45f) * ((capIdx * capNextIdx) / (capIdx + capNextIdx));
        if (Math.abs(ht) > maxSafeTransfer) {
            ht = Math.signum(ht) * maxSafeTransfer;
        }

        temps[idx] = tIdx - (ht / capIdx);
        temps[nextIdx] = tNextIdx + (ht / capNextIdx);
    }

    private static void processVerticalFlow(int i, int j) {
        int idx = i * world.sizeY + j;
        int nextIdx = idx + 1;

        float tIdx = temps[idx];
        float tNextIdx = temps[nextIdx];
        float deltaT = tIdx - tNextIdx;

        if (Math.abs(deltaT) <= 0.01f) {
            return;
        }

        boolean isSolidCurrent = world.isBlockType(i, j, Block.Type.SOLID);
        boolean isSolidNext = world.isBlockType(i, j + 1, Block.Type.SOLID);

        float conduct = getConductivity(i, j, i, j + 1);
        float capIdx = getHeatCapacity(i, j);
        float capNextIdx = getHeatCapacity(i, j + 1);
        float factor = HEAT_DIFFUSION_K;

        if (!isSolidCurrent && !isSolidNext) {
            float avgTemp = (tIdx + tNextIdx) / 2f;
            float verticalFactor = Math.abs(avgTemp) / (5f + Math.abs(avgTemp));

            if (tNextIdx < -1f && deltaT > 0) {
                factor = gs * verticalFactor;
            }
            else if (tIdx > 1f && deltaT > 0) {
                factor = cs * verticalFactor;
            }
        }

        float heatTransfer = deltaT * factor * conduct;
        float maxSafeTransfer = (Math.abs(deltaT) * 0.45f) * ((capIdx * capNextIdx) / (capIdx + capNextIdx));
        if (Math.abs(heatTransfer) > maxSafeTransfer) {
            heatTransfer = Math.signum(heatTransfer) * maxSafeTransfer;
        }

        temps[idx] = tIdx - (heatTransfer / capIdx);
        temps[nextIdx] = tNextIdx + (heatTransfer / capNextIdx);
    }

    private static float getHeatCapacity(int x, int y) {
        if (world.isBlockType(x, y, Block.Type.SOLID)) {
            return SOLID_HEAT_CAPACITY_MULT;
        }
        return BASE_DENSITY;
    }

    private static float getConductivity(int x1, int y1, int x2, int y2) {
        float c1 = world.isBlockType(x1, y1, Block.Type.SOLID) ? SOLID_CONDUCTIVITY_MULT : 1f;
        float c2 = world.isBlockType(x2, y2, Block.Type.SOLID) ? SOLID_CONDUCTIVITY_MULT : 1f;
        return Math.min(c1, c2);
    }

    //todo еще где то тут должен считаться/читаться предел прочности, чтоб
    //материал мог реагировать на слишком большое внутреннее напряжение (давление)
    public static float getDensity(int x, int y) {
        if (world.isBlockType(x, y, Block.Type.SOLID)) {
            return SOLID_DENSITY;
        }
        return BASE_DENSITY;
    }

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
}