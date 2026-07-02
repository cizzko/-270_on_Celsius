package core.World;

import core.content.blocks.Block;
import core.content.entity.LivingEntity;
import core.util.BatchScope;
import core.util.Debug;

import java.util.ArrayList;
import java.util.Arrays;

import static core.Global.entityPool;
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
    //чем выше, тем 'взрывоопаснее' газы (температура больше влияет на расширение)
    private static final float R = 250.05f;
    //тепло сосед-сосед
    //тестовое значение
    private static final float HEAT_DIFFUSION_K = 0.01f;
    private static final float SOLID_BASE_PRESSURE = 10000.0f;
    //Множитель теплоемкости блоков
    private static final float SOLID_HEAT_CAPACITY_MULT = 3f;
    //Множитель теплопроводности блоков
    //тестовое значение
    private static final float SOLID_CONDUCTIVITY_MULT = 0.01f;
    //верт тяга
    private static final float cs = 14f;
    private static final float gs = 14f;
    //как быстро газ разлетается вбок
    private static final float hb = 2.3f;
    //бустер скорости разлета вбок у пола/потолка (растекание)
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
            Debug.saveWindForce("Power", scope);
            Debug.saveWindDirection("Dir", scope);
        }
        for (int i = 0; i < 10000; i++) {
            update(scope);
        }
        Debug.saveTemp("Temp1", null);
        Debug.savePressures("Pressure1", null);
        Debug.saveWindForce("Power1", scope);
        Debug.saveWindDirection("Dir1", scope);
    }
    
    private static void updateLivingEntity() {
        entityPool.forEachType(LivingEntity.class, emitter -> {
            if (!emitter.isEmitting()) {
                return;
            }

            int x = emitter.blockX();
            int y = emitter.blockY();
            int radius =  emitter.heatRadius();
            float power = emitter.heatPower();

            //todo радиус квадратно-симметричный, хотелось бы более гибко
            for (int i = 0; i < radius; i++) {
                for (int j = 0; j < radius; j++) {
                    addTemp(x + i, y + j, power);
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
        int idx = pos2index(i, j);
        int nextIdx = nextI * world.sizeY + j;

        float tIdx = temps[idx];
        float tNextIdx = temps[nextIdx];
        float deltaT = tIdx - tNextIdx;
        float absDeltaT = Math.abs(deltaT);

        if (absDeltaT <= 0.01f) {
            return;
        }

        boolean isSolidCurrent = world.isBlockType(i, j, Block.Type.SOLID);
        boolean isSolidNext = world.isBlockType(nextI, j, Block.Type.SOLID);

        float conduct = getConductivity(i, j, nextI, j);
        float capIdx = getHeatCapacity(i, j);
        float capNextIdx = getHeatCapacity(nextI, j);

        float factor = HEAT_DIFFUSION_K;
        boolean solidBelowCurrent = (j > 0) && world.isBlockType(i, j - 1, Block.Type.SOLID);
        boolean solidBelowNext = (j > 0) && world.isBlockType(nextI, j - 1, Block.Type.SOLID);

        if (!isSolidCurrent && !isSolidNext) {
            if (absDeltaT > 0.5f) {
                if (deltaT < 0) {
                    factor = (solidBelowCurrent || solidBelowNext) ? (hb * 4.5f) : (hb * 0.4f);
                } else {
                    factor = hb * hs;
                }
            }
        }

        float ht = deltaT * factor * conduct;
        if (solidBelowCurrent && deltaT > 0) {
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
    }

    private static final float VERT = (gs + cs) * 0.5f;
    private static void processVerticalFlow(int i, int j) {
        int idx = pos2index(i, j);
        int nextIdx = idx + 1;

        float tIdx = temps[idx];
        float tNextIdx = temps[nextIdx];
        float deltaT = tIdx - tNextIdx;
        float absDeltaT = Math.abs(deltaT);

        if (absDeltaT <= 0.01f) {
            return;
        }

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
        float trns = (absDeltaT * 0.45f) * (capIdx * capNextIdx * 1.0f / (capIdx + capNextIdx));
        if (Math.abs(ht) > trns) {
            ht = (ht > 0) ? trns : -trns;
        }

        temps[idx] = tIdx - (ht * (1.0f / capIdx));
        temps[nextIdx] = tNextIdx + (ht * (1.0f / capNextIdx));
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

    public static void addTemp(int x, int y, float temp) {
        temps[pos2index(x, y)] += temp;
    }

    public static void setTemp(int x, int y, float temp) {
        temps[pos2index(x, y)] = temp;
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

        float factor = HEAT_DIFFUSION_K;
        if (Math.abs(deltaT) > 0.5f) {
            if (deltaT < 0) {
                boolean sc = (j > 0) && world.isBlockType(i, j - 1, Block.Type.SOLID);
                boolean sn = (j > 0) && world.isBlockType(nextI, j - 1, Block.Type.SOLID);
                factor = (sc || sn) ? (hb * 4.5f) : (hb * 0.4f);
            } else {
                factor = hb * hs;
            }
        }

        float ht = deltaT * factor * getConductivity(i, j, nextI, j);

        if (j > 0 && world.isBlockType(i, j - 1, Block.Type.SOLID) && deltaT > 0) {
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