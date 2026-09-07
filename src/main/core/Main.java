package core;

import core.assets.AssetsManager;
import org.lwjgl.system.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

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
        configureLwjglLibraryPath(exploded);

        assets = new AssetsManager(exploded, Constants.appName);

        var window = new Window();
        Global.app = window;
        window.run();
    }

    private static void configureLwjglLibraryPath(boolean exploded) {
        if (System.getProperty("org.lwjgl.librarypath") != null) {
            return;
        }
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path[] candidates = exploded ? new Path[]{userDir.resolve("build/natives"), userDir.resolve("natives"), userDir.resolve("src/main/native")} : new Path[]{userDir.resolve("app"), userDir};
        for (Path p : candidates) {
            if (Files.isDirectory(p)) {
                Configuration.LIBRARY_PATH.set(p.toString());
                System.setProperty("org.lwjgl.librarypath", p.toString());
                return;
            }
        }
    }
}
