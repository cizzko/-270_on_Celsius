package core;

import core.EventHandling.Config;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static core.Global.assets;
import static core.Global.world;

public class AutoSaveListener implements ApplicationListener {
    private long lastSaveTimestamp = System.currentTimeMillis();

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH:mm:ss")
            .localizedBy(Locale.getDefault());

    @Override
    public void update() {
        if (Global.gameState == GameState.PLAYING) {

            int worldSaveDelay = Config.getInt("AutosaveWorldFrequency", 60*60);
            if ((System.currentTimeMillis() - lastSaveTimestamp)/1000 >= worldSaveDelay) {

                var now = LocalDateTime.now()
                        .truncatedTo(ChronoUnit.MILLIS);

                Path saveFile = assets.workingDir().resolve("open_worl_" + now.format(formatter) + ".json");
                try {
                    Config.json.writeValue(saveFile.toFile(), world);
                } catch (IOException e) {
                    Application.log.error("Failed to auto-save world to file '{}'", saveFile, e);
                }

                lastSaveTimestamp = System.currentTimeMillis();
            }
        }

    }
}
