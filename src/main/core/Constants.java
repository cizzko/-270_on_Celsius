package core;

public final class Constants {
    private Constants() {}

    public static final String versionStamp = "0.32";
    public static final String version = "beta " + versionStamp + " (non stable)";
    public static final String appName = "Celsius";

    public static final String discordLink = "https://discord.gg/gUS9X6exAQ";

    public static final class World {
        public static final int availableProcessors = Runtime.getRuntime().availableProcessors();

        public static final int COPY_SIZE = 50;
        /* Минимальный размер мира в блоках */
        public static final int MIN_WORLD_SIZE = 500;
        /* Максимальный размер мира в блоках */
        public static final int MAX_WORLD_SIZE = 15000;
    }
}
