package core;

import core.util.Config;

import static core.World.WorldGenerator.WorldGeneratorConstants.COPY_SIZE;
import static core.WorldCoordinates.toWorld;

public final class Constants {
    private Constants() {}

    public static final String versionStamp = "0.42";
    public static final String version = "beta " + versionStamp + " (non stable)";
    public static final String appName = "Celsius";

    public static final String link = "https://discord.gg/gUS9X6exAQ";

    //нельзя брать вообще все, иначе звуки и гуи самой ос начинают лагать
    static int prcs = Config.getInt("Processors");
    public static int availableProcessors = prcs > 0 ? prcs : Runtime.getRuntime().availableProcessors() - 1;

    public static final class Camera {
        public static final float OFFSET_X = toWorld(32f);
        public static final float OFFSET_Y = toWorld(200f);
    }

    public static final class World {
        /* говорит само за себя */
        public static final float ABS_ZERO = -273.15f;
        /* Минимальный размер мира в блоках */
        public static final int MIN_WORLD_SIZE = 500;
        /* Максимальный размер мира в блоках */
        public static final int MAX_WORLD_SIZE = 30000;
        /* В какой области с концов мира нас телепортирует на другую сторону */
        public static final int SWAP_AREA = COPY_SIZE / 2;
    }

    public static final class Entity {
        /* Максимальное количество сущности в мире единовременно существующих */
        public static final int MAX_COUNT = Short.MAX_VALUE;
    }
}
