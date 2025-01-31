package core;

import core.EventHandling.Logging.Logger;
import core.assets.AssetsManager;

import java.nio.file.Files;

import static core.Global.assets;

public class Main {
    public static void main(String[] args) throws Throwable {
        boolean exploded = true;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--packaged")) {
                exploded = false;
                break;
            }
        }
        assets = new AssetsManager(exploded, Constants.appName);
        Files.writeString(assets.dataDir().resolve("config.properties"), "Debug=2");
        Logger.log("-------- Log started -------- \nStarting...");

        new Window().run();
    }
}
