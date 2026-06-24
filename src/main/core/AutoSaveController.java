package core;

import core.World.WorldUtils;
import core.util.Config;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static core.Global.assets;
import static core.Global.world;

public class AutoSaveController {
    private static long lastSaveTimestamp = System.currentTimeMillis();

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH:mm:ss")
            .localizedBy(Locale.getDefault());

    public static void update() {
        int worldSaveDelay = Config.getInt("AutosaveWorldFrequency", 60*60);
        if ((System.currentTimeMillis() - lastSaveTimestamp)/1000 >= worldSaveDelay) {
            lastSaveTimestamp = System.currentTimeMillis();

            autosave();
        }
    }

    public static void autosave() {
        var now = LocalDateTime.now()
                .truncatedTo(ChronoUnit.MILLIS);

        Path saveFile = assets.workingDir().resolve("open_worl_" + now.format(formatter) + ".json");
        WorldUtils.saveWorld(world, saveFile);
    }
}
