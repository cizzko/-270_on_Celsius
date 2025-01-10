package core;

import core.EventHandling.Logging.Logger;
import core.assets.AssetsManager;

import static core.Global.assets;

public class Main {
    public static void main(String[] args) {
        boolean exploded = true;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--packaged")) {
                exploded = false;
                break;
            }
        }
        assets = new AssetsManager(exploded);
        Logger.log("-------- Log started -------- \nStarting...");

        new Window().run();
    }
}
