package core.World;

import core.Constants;
import core.content.blocks.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import static core.Global.world;
import static core.World.WorldGenerator.WorldGeneratorConstants.COPY_SIZE;

//счас по упрощенной модели - не считает диагонали, большие клампы
//возможно, в будущем появится галочка обычной или упрощенной модели
public class TemperatureMap {
    private static float[][] temps;
    private static float[][] density;

    //ем выше, тем 'взрывоопаснее' газы (температура больше влияет на расширение)
    public static final float R = 500.05f;
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

    //todo
    //Множитель теплоемкости блоков
    public static final float SOLID_HEAT_CAPACITY_MULT = 3f;
    //Множитель теплопроводности блоков
    public static final float SOLID_CONDUCTIVITY_MULT = 0.4f;

    public static void generate() {
        temps = new float[world.sizeX][world.sizeY];
        density = new float[world.sizeX][world.sizeY];

        //todo
        for (int x = 0; x < world.sizeX; x++) {
            java.util.Arrays.fill(temps[x], 50.0f);
            java.util.Arrays.fill(density[x], 1.2f);
        }

//        int halfSize = 40;
//        int spawnX = world.sizeY / 2;
//        int spawnY = world.sizeY / 2;
//
//        for (int x = spawnX - halfSize; x < spawnX + halfSize; x++) {
//            for (int y = spawnY - halfSize; y < spawnY + halfSize; y++) {
//                if (y >= 0 && y < world.sizeY && x > 0) {
//                    temps[x][y] = 1000.0f;
//                    density[x][y] = 7f;
//                }
//            }
//        }
//        for (int i = 0; i < 2000; i++) {
//            update();
//        }
//        WorldGeneratorTMP.saveTemp("Temp");
//        WorldGeneratorTMP.savePressures("Pressure");
//        WorldGeneratorTMP.saveDens("Density");
//        for (int i = 0; i < 10000; i++) {
//            update();
//        }
//        WorldGeneratorTMP.saveTemp("Temp1");
//        WorldGeneratorTMP.savePressures("Pressure1");
//        WorldGeneratorTMP.saveDens("Density1");
    }

    //чтоб не утекало в одну сторону, это нормальная практика
    private static boolean flip = false;

    public static void update() {
        //todo такт не надо, ведь точно можем узнать что где и когда но пока лень
        for (int x = 0; x < world.sizeX; x++) {
            for (int j = 0; j < world.sizeY; j++) {
                if (x < COPY_SIZE) {
                    int targetX = world.sizeX - COPY_SIZE + x;
                    temps[targetX][j] = temps[x][j];
                    density[targetX][j] = density[x][j];
                } else if (x > world.sizeX - COPY_SIZE) {
                    int targetX = x - (world.sizeX - COPY_SIZE);
                    temps[targetX][j] = temps[x][j];
                    density[targetX][j] = density[x][j];
                }
            }
        }

        flip = !flip;
        final int leftBorder = 0;
        final int rightBorder = world.sizeX - COPY_SIZE;
        final int activeWidth = rightBorder - leftBorder;

        int processors = Constants.availableProcessors;
        int chunkSize = activeWidth / processors;
        if (chunkSize < 1) {
            chunkSize = 1;
        }

        List<Future<?>> futures = new ArrayList<>(processors);
        final boolean currentFlip = flip;

        //оно должно ломаться, ведь итерации созависимы, но охренеть не ломается
        for (int t = 0; t < processors; t++) {
            final int startX = leftBorder + t * chunkSize;
            final int endX = (t == processors - 1) ? rightBorder : (startX + chunkSize);

            if (startX >= rightBorder) {
                break;
            }

            futures.add(world.genPool.submit(() -> {
                if (currentFlip) {
                    for (int i = startX; i < endX; i++) {
                        for (int j = 0; j < world.sizeY; j++) {
                            processHorizontalFlow(i, j, leftBorder, activeWidth);

                            if (j < world.sizeY - 1) {
                                processVerticalFlow(i, j);
                            }
                        }
                    }
                } else {
                    for (int i = endX - 1; i >= startX; i--) {
                        for (int j = world.sizeY - 1; j >= 0; j--) {
                            processHorizontalFlow(i, j, leftBorder, activeWidth);

                            if (j < world.sizeY - 1) {
                                processVerticalFlow(i, j);
                            }
                        }
                    }
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void processHorizontalFlow(int i, int j, int leftBorder, int activeWidth) {
        //todo хихи хаха
        int nextI = leftBorder + ((i - leftBorder + 1) % activeWidth);

        boolean isLeftSolid = world.getBlockType(i, j) == Block.Type.SOLID;
        boolean isRightSolid = world.getBlockType(nextI, j) == Block.Type.SOLID;

        float deltaT = temps[i][j] - temps[nextI][j];
        if (Math.abs(deltaT) > 0.01f) {
            float conduct = getConductivity(i, j, nextI, j);
            float ht = deltaT * HEAT_DIFFUSION_K * conduct;

            temps[i][j] -= ht / getHeatCapacity(i, j);
            temps[nextI][j] += ht / getHeatCapacity(nextI, j);
        }

        if (isLeftSolid || isRightSolid) {
            return;
        }

        float pLeft = getPressure(i, j);
        float pRight = getPressure(nextI, j);
        float deltaP = pLeft - pRight;

        if (Math.abs(deltaP) > MIN_PRESSURE_DELTA) {
            float flow = deltaP * SIM_K;

            //флов показывает влево или вправо
            if (flow > 0) {
                float maxFlow = density[i][j] * MAX_FLOW_RESTR;
                if (flow > maxFlow){
                    flow = maxFlow;
                }

                float energy = flow * temps[i][j];
                float energyLeft = (density[i][j] * temps[i][j]) - energy;
                float energyRight = (density[nextI][j] * temps[nextI][j]) + energy;

                density[i][j] -= flow;
                density[nextI][j] += flow;

                if (density[i][j] > MIN_DENSITY_THRESHOLD) {
                    temps[i][j] = energyLeft / density[i][j];
                }
                if (density[nextI][j] > MIN_DENSITY_THRESHOLD) {
                    temps[nextI][j] = energyRight / density[nextI][j];
                }

            } else if (flow < 0) {
                flow = -flow;
                float maxFlow = density[nextI][j] * MAX_FLOW_RESTR;
                if (flow > maxFlow) {
                    flow = maxFlow;
                }

                float energy = flow * temps[nextI][j];
                float energyRight = (density[nextI][j] * temps[nextI][j]) - energy;
                float energyLeft = (density[i][j] * temps[i][j]) + energy;

                density[nextI][j] -= flow;
                density[i][j] += flow;

                if (density[nextI][j] > MIN_DENSITY_THRESHOLD) {
                    temps[nextI][j] = energyRight / density[nextI][j];
                }
                if (density[i][j] > MIN_DENSITY_THRESHOLD) {
                    temps[i][j] = energyLeft / density[i][j];
                }
            }
        }
    }

    private static void processVerticalFlow(int i, int j) {
        float deltaT = temps[i][j] - temps[i][j + 1];
        if (Math.abs(deltaT) > 0.01f) {
            float conduct = getConductivity(i, j, i, j + 1);
            float heatTransfer = deltaT * HEAT_DIFFUSION_K * conduct;

            temps[i][j] -= heatTransfer / getHeatCapacity(i, j);
            temps[i][j + 1] += heatTransfer / getHeatCapacity(i, j + 1);
        }

        boolean isUpSolid = world.getBlockType(i, j) == Block.Type.SOLID;
        boolean isDownSolid = world.getBlockType(i, j + 1) == Block.Type.SOLID;
        if (isUpSolid || isDownSolid) {
            return;
        }

        float pUp = getPressure(i, j);
        float pDown = getPressure(i, j + 1);
        float deltaP = pUp - pDown;

        if (Math.abs(deltaP) > MIN_PRESSURE_DELTA) {
            float flow = deltaP * SIM_K;

            if (flow > 0) {
                float maxFlow = density[i][j] * MAX_FLOW_RESTR;
                if (flow > maxFlow) {
                    flow = maxFlow;
                }

                float energy = flow * temps[i][j];
                float energyUp = (density[i][j] * temps[i][j]) - energy;
                float energyDown = (density[i][j + 1] * temps[i][j + 1]) + energy;

                density[i][j] -= flow;
                density[i][j + 1] += flow;

                if (density[i][j] > MIN_DENSITY_THRESHOLD) {
                    temps[i][j] = energyUp / density[i][j];
                }
                if (density[i][j + 1] > MIN_DENSITY_THRESHOLD) {
                    temps[i][j + 1] = energyDown / density[i][j + 1];
                }

            } else if (flow < 0) {
                flow = -flow;
                float maxFlow = density[i][j + 1] * MAX_FLOW_RESTR;
                if (flow > maxFlow) {
                    flow = maxFlow;
                }

                float energy = flow * temps[i][j + 1];
                float energyDown = (density[i][j + 1] * temps[i][j + 1]) - energy;
                float energyUp = (density[i][j] * temps[i][j]) + energy;

                density[i][j + 1] -= flow;
                density[i][j] += flow;

                if (density[i][j + 1] > MIN_DENSITY_THRESHOLD) {
                    temps[i][j + 1] = energyDown / density[i][j + 1];
                }
                if (density[i][j] > MIN_DENSITY_THRESHOLD) {
                    temps[i][j] = energyUp / density[i][j];
                }
            }
        }
    }

    private static float getHeatCapacity(int x, int y) {
        if (world.getBlockType(x, y) == Block.Type.SOLID) {
            return SOLID_HEAT_CAPACITY_MULT;
        }
        return Math.max(MIN_DENSITY_THRESHOLD, density[x][y]);
    }

    private static float getConductivity(int x1, int y1, int x2, int y2) {
        float c1 = (world.getBlockType(x1, y1) == Block.Type.SOLID) ? SOLID_CONDUCTIVITY_MULT : 1.0f;
        float c2 = (world.getBlockType(x2, y2) == Block.Type.SOLID) ? SOLID_CONDUCTIVITY_MULT : 1.0f;

        return Math.min(c1, c2);
    }

    //todo еще где то тут должен считаться/читаться предел прочности, чтоб
    //материал мог реагировать на слишком большое внутреннее напряжение (давление)

    //не семплируется!!!! если нужно узнать в конкретной точке
    //то надо брать несколько и сглаживать
    public static float getDensity(int x, int y) {
        if (world.getBlockType(x, y) == Block.Type.SOLID) {
            //todo
            return 5;
        }
        return density[x][y];
    }

    public static float getPressure(int x, int y) {
        return getDensity(x, y) * (temps[x][y] + 273.15f) * R;
    }

    public static float getTempCell(int x, int y) {
        return temps[x][y];
    }
}
