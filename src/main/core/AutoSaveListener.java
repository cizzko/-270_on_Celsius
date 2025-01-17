package core;

import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Logger;
import core.World.Saves;

public class AutoSaveListener implements ApplicationListener {
    private long lastSaveTimestamp = System.currentTimeMillis();

    @Override
    public void update() {
        int worldSaveDelay = Integer.parseInt(Config.getFromConfig("AutosaveWorldFrequency"));
        if (System.currentTimeMillis() - lastSaveTimestamp >= worldSaveDelay) {
            Logger.log("Creating world backup..");
            // TODO реализовать

            lastSaveTimestamp = System.currentTimeMillis();
        }
    }
}
