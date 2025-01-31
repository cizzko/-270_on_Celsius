package core;

public final class Constants {
    private Constants() {}

    public static final String versionStamp = "0.0.7";
    public static final String version = "alpha " + versionStamp + " (non stable)";
    public static final String appName = "Celsius";

    public static final class World {
        /* Минимальный размер мира в блоках */
        public static final int MIN_WORLD_SIZE = 200;
        /* Максимальный размер мира в блоках */
        public static final int MAX_WORLD_SIZE = 2500;
    }
}
