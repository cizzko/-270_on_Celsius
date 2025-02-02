package core;

import core.EventHandling.Logging.Config;

public class AutoSaveListener implements ApplicationListener {
    private long lastSaveTimestamp = System.currentTimeMillis();

    @Override
    public void update() {
        int worldSaveDelay = Integer.parseInt(Config.getFromConfig("AutosaveWorldFrequency"));
        if (System.currentTimeMillis() - lastSaveTimestamp >= worldSaveDelay) {
            // TODO реализовать

            lastSaveTimestamp = System.currentTimeMillis();
        }
    }
}
