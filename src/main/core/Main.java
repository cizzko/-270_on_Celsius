package core;

import core.assets.AssetsManager;

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

        new Window().run();
    }
}
