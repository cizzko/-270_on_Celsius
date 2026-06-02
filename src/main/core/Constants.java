package core;

import static core.World.WorldGenerator.WorldGeneratorConstants.COPY_SIZE;

public final class Constants {
    private Constants() {}

    public static final String versionStamp = "0.32";
    public static final String version = "beta " + versionStamp + " (non stable)";
    public static final String appName = "Celsius";

    public static final String link = "https://discord.gg/gUS9X6exAQ";

    //нельзя брать вообще все, иначе звуки и гуи самой ос начинают лагать
    public static final int availableProcessors = Runtime.getRuntime().availableProcessors() - 1;

    public static final class World {
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
