package core.World.WorldGenerator;

public class WorldGeneratorConstants {
    //за вертикаль принято 0, горизонталь 90

    /* Сколько блоков копируем по краям */
    public static final int COPY_SIZE = 50;
    /* между миром и скопированным куском 90 блоков,
       для сглаживания перепад высот между ними   */
    public static final int INTERPOLATE_SIZE = 90;

    /** Стартовый угол генерации рельефа в градусах */
    public static final float RELIEF_START_ANGLE = 90f;
    /** Прижимает угол генерации к горизонтали (не позволяет миру быть как пила) */
    public static final float RELIEF_ITERS_MULTIPLIER = 150f;
    /** Максимальный базовый угол для рельефа */
    public static final float RELIEF_BASE_ANGLE = 90f;
    /** Минимальное количество блоков между сменой биомов */
    public static final int MIN_SWAP_BIOMES = 200;
    /** Ранд для смены биома */
    public static final float BIOME_SWAP_MULTIPLIER = 30f;
    /** Порог блоков для применения флага предыдущего биома */
    public static final int BIOME_SWAP_MAX_THRESHOLD = 20;
    /** Шанс сглаживания перехода между биомами */
    public static final float BIOME_SWAP_CHANCE = 5f;

    public static final float SMOOTH_HEIGHTS_DELTA = 90f;

    /** Начальная координата X для пещер */
    public static final int CAVES_INITIAL_X = INTERPOLATE_SIZE + COPY_SIZE;
    /** Случайный множитель расстояния между пещерами */
    public static final float CAVES_COUNT_RAND_MULT = 40f;
    /** Минимальное расстояние между пещерами */
    public static final float CAVES_COUNT_BASE = 50f;
    /** Минимальный радиус генерируемой пещеры */
    public static final int CAVES_MIN_RADIUS = 2;
    /** Максимальный радиус генерируемой пещеры */
    public static final int CAVES_MAX_RADIUS = 8;
    /** Шанс генерации пещеры сверху (после минимального количества) */
    public static final float CAVES_UPPER_CHANCE = 1.1f;
    /** Шанс генерации пещеры идущей влево или вправо */
    public static final float CAVES_LR_CHANCE = 0.9f;
    /** Шанс генерации пещеры идущей в случайное направление */
    public static final float CAVES_EVERY_CHANCE = 1.5f;
    /** Минимальное количество пещер сверху (от общего колва пещер) */
    public static final int CAVES_UPPER_DIVISOR = 16;
    /** Максимальная глубина (от world.sizeY / 2) пещер в земле */
    public static final float CAVES_DOWNED_Y_DIVISOR = 2.4f;
    /** ьььььььььььь */
    public static final float CAVES_X_DIVISOR_UPPER = 4f;
    /** ъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъъ */
    public static final float CAVES_X_DIVISOR_DOWNED = 2f;
    /** Макс размер удаляемого острова */
    public static final int ISLAND_MAXSIZE_CLEAR_SIZE = 150;

    /** Минимальный угол генерации для верхних пещер */
    public static final int CAVE_UPPER_MIN_ANGLE = 100;
    /** Максимальный угол генерации для верхних пещер */
    public static final int CAVE_UPPER_MAX_ANGLE = 260;
    /** Минимальный стартовый угол для верхних пещер */
    public static final int CAVE_UPPER_START_MIN = 40;
    /** Максимальный стартовый угол для верхних пещер */
    public static final int CAVE_UPPER_START_MAX = 170;
    /** Базовый шанс ответвления для верхних пещер (отростки отростков) */
    public static final int CAVE_UPPER_SHOT_CHANCE = 100;

    /** Минимальный угол генерации для нижних пещер */
    public static final int CAVE_DOWNED_MIN_ANGLE = 95;
    /** Максимальный угол генерации для нижних пещер */
    public static final int CAVE_DOWNED_MAX_ANGLE = 265;
    /** Случайный стартовый угол для нижних пещер */
    public static final int CAVE_DOWNED_START_ANGLE = 360;
    /** Базовый шанс ответвления для нижних пещер (отростки отростков) */
    public static final int CAVE_DOWNED_SHOT_CHANCE = 150;

    /** Множитель изменения угла пещеры в зависимости от глубины */
    public static final float CAVE_MAX_ANGLE_MULT = 80f;
    /** Минимальное изменение угла пещеры */
    public static final int CAVE_MAX_ANGLE_MIN = 10;
    /** Максимальное изменение угла пещеры */
    public static final int CAVE_MAX_ANGLE_MAX = 50;
    /** Шанс изменения радиуса пещеры на итерации (больше число меньше шанс) */
    public static final int CAVE_RADIUS_CHANCE = 25;
    /** Радиус меняется на от CAVE_RADIUS_MIN до CAVE_RADIUS_MAX */
    public static final float CAVE_RADIUS_MIN = -1f;
    /** Радиус меняется на от CAVE_RADIUS_MIN до CAVE_RADIUS_MAX */
    public static final float CAVE_RADIUS_MAX = 1f;
    /** Рандомное количество итераций на каждый угол пещеры */
    public static final int CAVE_ITERS_BOUND = 5;
    /** Минимально итерсов от прошлого отростка для пещер в рандомном направлении */
    public static final int CAVE_EVERY_MIN_ITERS = 30;
    /** Минимально итерсов от прошлого отростка для пещер влево||вправо */
    public static final int CAVE_LR_MIN_ITERS = 20;
    /** Шанс появления отростка от отростка уменьшается на от CAVE_SHOT_MULT_MIN до CAVE_SHOT_MULT_MAX */
    public static final float CAVE_SHOT_MULT_MIN = 2.6f;
    /** Шанс появления отростка от отростка уменьшается на от CAVE_SHOT_MULT_MIN до CAVE_SHOT_MULT_MAX */
    public static final float CAVE_SHOT_MULT_MAX = 3.2f;
    /** Угол для пещер задается начальный + ранд от CAVE_SHOT_ANGLE_MIN до CAVE_SHOT_ANGLE_MAX */
    public static final float CAVE_SHOT_ANGLE_MIN = -45f;
    /** Угол для пещер задается начальный + ранд от CAVE_SHOT_ANGLE_MIN до CAVE_SHOT_ANGLE_MAX */
    public static final float CAVE_SHOT_ANGLE_MAX = 45f;
    /** Поскольку шанс отростка увеличивается нелинейно (*), нужен ограничитель чтоб он не был слишком малым */
    public static final int CAVE_EVERY_CHANCE_SHOT_MAX = 1200;
    /** Поскольку шанс отростка увеличивается нелинейно (*), нужен ограничитель чтоб он не был слишком малым */
    public static final int CAVE_LR_CHANCE_SHOT_MAX = 1500;
    /** Ограничение угла для пещер в случайном направлении */
    public static final int CAVE_EVERY_ANGLE_OFFSET = 50;
    /** тут я устал писать */
    //todo
    public static final int CAVE_BRANCH_LEFT_MIN_ANGLE = 265;
    public static final int CAVE_BRANCH_RIGHT_MIN_ANGLE = 20;
    public static final int CAVE_BRANCH_LEFT_MAX_ANGLE = 340;
    public static final int CAVE_BRANCH_RIGHT_MAX_ANGLE = 95;
    /** Ограничение максимальной глубины пещер (больше число - ниже может опуститься) */
    public static final float CAVE_Y_BOUND_DIVISOR = 15f;
    /** Максимальное общее количество итераций для одной пещеры */
    public static final int CAVE_TOTAL_ITERS_MAX = 5000;
    /** Какая то штука для зависимости от размера мира */
    public static final float CAVE_TOTAL_ITERS_DIVISOR = 200f;

    /** Начальный размер стака при удалении островов */
    public static final int LOCAL_STACK_CAPACITY = 512;

    /** Шанс спавна декоративных камней */
    public static final float DECOR_STONE_SPAWN_CHANCE = 40f;

    /** Шанс спавна травы */
    public static final int HERB_SPAWN_CHANCE = 10;
    /** Минимальный размер зарослей травы */
    public static final int HERB_MIN_FOREST_SIZE = 1;
    /** Максимальный размер зарослей травы */
    public static final int HERB_MAX_FOREST_SIZE = 20;
    /** Минимальная дистанция между спавном травы */
    public static final int HERB_MIN_SPAWN_DIST = 1;
    /** Максимальная дистанция между спавном травы */
    public static final int HERB_MAX_SPAWN_DIST = 0;

    /** Множитель размера области генерации леса */
    public static final int FOREST_SIZE_MULT = 8;
    /** Какой то оффсет смещения */
    public static final int FOREST_SIZE_OFFSET = 4;

    /** Сюда не подглядываем, доделаю позже */
    public static final float RESOURCES_ITER_DIVISOR = 100f;
    /** Сюда не подглядываем, доделаю позже */
    public static final int RESOURCES_NOISE_BOUND = 30;
    /** Сюда не подглядываем, доделаю позже */
    public static final float RESOURCES_NOISE_SCALE = 1.3f;
}
