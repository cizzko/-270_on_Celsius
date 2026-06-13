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

        int halfSize = 40;
        int spawnX = world.sizeX / 2;
        int spawnY = world.sizeY / 2;

        {
            int startX = clamp(spawnX - halfSize, 0, world.sizeY - 1);
            int endX   = clamp(spawnX + halfSize, 0, world.sizeY - 1);
            int startY = clamp(spawnY - halfSize, 0, world.sizeX - 1);
            int endY   = clamp(spawnY + halfSize, 0, world.sizeX - 1);

            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    int idx = pos2index(x, y);
                    temps[idx] = 1000.0f;
                    density[idx] = 7f;
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
        for (int x = 0; x < world.sizeX; x++) {
            for (int y = 0; y < world.sizeY; y++) {
                if (x < COPY_SIZE) {
                    int targetX = world.sizeX - COPY_SIZE + x;
                    temps[pos2index(targetX, y)] = temps[pos2index(x, y)];
                    density[pos2index(targetX, y)] = density[pos2index(x, y)];
                } else if (x > world.sizeX - COPY_SIZE) {
                    int targetX = x - (world.sizeX - COPY_SIZE);
                    temps[pos2index(targetX, y)] = temps[pos2index(x, y)];
                    density[pos2index(targetX, y)] = density[pos2index(x, y)];
                }
            }
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
        //todo хихи хаха
        int nextI = leftBorder + ((i - leftBorder + 1) % activeWidth);

        int idx = pos2index(i, j);
        int nextIdx = pos2index(nextI, j);

        float deltaT = temps[idx] - temps[nextIdx];
        if (Math.abs(deltaT) > 0.01f) {
            float conduct = getConductivity(i, j, nextI, j);
            float ht = deltaT * HEAT_DIFFUSION_K * conduct;

            temps[idx] -= ht / getHeatCapacity(i, j);
            temps[nextIdx] += ht / getHeatCapacity(nextI, j);
        }

        boolean isLeftSolid = world.isBlockType(i, j, Block.Type.SOLID);
        if (isLeftSolid) {
            return;
        }
        boolean isRightSolid = world.isBlockType(nextI, j, Block.Type.SOLID);
        if (isRightSolid) {
            return;
        }

        float pLeft = getPressure(i, j);
        float pRight = getPressure(nextI, j);
        float deltaP = pLeft - pRight;

        if (Math.abs(deltaP) > MIN_PRESSURE_DELTA) {
            float flow = deltaP * SIM_K;

            //флов показывает влево или вправо
            if (flow > 0) {
                float maxFlow = density[idx] * MAX_FLOW_RESTR;
                if (flow > maxFlow) {
                    flow = maxFlow;
                }

                float energy = flow * temps[idx];
                float energyLeft = (density[idx] * temps[idx]) - energy;
                float energyRight = (density[nextIdx] * temps[nextIdx]) + energy;

                density[idx] -= flow;
                density[nextIdx] += flow;

                if (density[idx] > MIN_DENSITY_THRESHOLD) {
                    temps[idx] = energyLeft / density[idx];
                }
                if (density[nextIdx] > MIN_DENSITY_THRESHOLD) {
                    temps[nextIdx] = energyRight / density[nextIdx];
                }

            } else {
                flow = -flow;
                float maxFlow = density[nextIdx] * MAX_FLOW_RESTR;
                if (flow > maxFlow) {
                    flow = maxFlow;
                }

                float energy = flow * temps[nextIdx];
                float energyRight = (density[nextIdx] * temps[nextIdx]) - energy;
                float energyLeft = (density[idx] * temps[idx]) + energy;

                density[nextIdx] -= flow;
                density[idx] += flow;

                if (density[nextIdx] > MIN_DENSITY_THRESHOLD) {
                    temps[nextIdx] = energyRight / density[nextIdx];
                }
                if (density[idx] > MIN_DENSITY_THRESHOLD) {
                    temps[idx] = energyLeft / density[idx];
                }
            }
        }
    }

    private static void processVerticalFlow(int i, int j) {
        int idx = pos2index(i, j);
        int nextIdx = pos2index(i, j + 1);

        float deltaT = temps[idx] - temps[nextIdx];
        if (Math.abs(deltaT) > 0.01f) {
            float conduct = getConductivity(i, j, i, j + 1);
            float heatTransfer = deltaT * HEAT_DIFFUSION_K * conduct;

            temps[idx] -= heatTransfer / getHeatCapacity(i, j);
            temps[nextIdx] += heatTransfer / getHeatCapacity(i, j + 1);
        }

        boolean isUpSolid = world.isBlockType(i, j, Block.Type.SOLID);
        if (isUpSolid) {
            return;
        }
        boolean isDownSolid = world.isBlockType(i, j + 1, Block.Type.SOLID);
        if (isDownSolid) {
            return;
        }

        float pUp = getPressure(i, j);
        float pDown = getPressure(i, j + 1);
        float deltaP = pUp - pDown;

        if (Math.abs(deltaP) > MIN_PRESSURE_DELTA) {
            float flow = deltaP * SIM_K;

            if (flow > 0) {
                float maxFlow = density[idx] * MAX_FLOW_RESTR;
                if (flow > maxFlow) {
                    flow = maxFlow;
                }

                float energy = flow * temps[idx];
                float energyUp = (density[idx] * temps[idx]) - energy;
                float energyDown = (density[nextIdx] * temps[nextIdx]) + energy;

                density[idx] -= flow;
                density[nextIdx] += flow;

                if (density[idx] > MIN_DENSITY_THRESHOLD) {
                    temps[idx] = energyUp / density[idx];
                }
                if (density[nextIdx] > MIN_DENSITY_THRESHOLD) {
                    temps[nextIdx] = energyDown / density[nextIdx];
                }

            } else {
                flow = -flow;
                float maxFlow = density[nextIdx] * MAX_FLOW_RESTR;
                if (flow > maxFlow) {
                    flow = maxFlow;
                }

                float energy = flow * temps[nextIdx];
                float energyDown = (density[nextIdx] * temps[nextIdx]) - energy;
                float energyUp = (density[idx] * temps[idx]) + energy;

                density[nextIdx] -= flow;
                density[idx] += flow;

                if (density[nextIdx] > MIN_DENSITY_THRESHOLD) {
                    temps[nextIdx] = energyDown / density[nextIdx];
                }
                if (density[idx] > MIN_DENSITY_THRESHOLD) {
                    temps[idx] = energyUp / density[idx];
                }
            }
        }
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
