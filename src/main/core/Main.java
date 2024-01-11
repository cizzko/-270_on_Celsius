package core;

import core.EventHandling.Logging.Logger;
import core.assets.AssetsManager;

import static core.Global.assets;

public class Main {
    public static void main(String[] args) {
        assets = new AssetsManager();
        Logger.log("-------- Log started -------- \nStarting...");

        new Window().run();
    }
}
