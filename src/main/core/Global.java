package core;

import core.assets.AssetsManager;
import core.g2d.Atlas;
import core.g2d.*;

public final class Global {
    private Global() {}

    public static InputHandler input;
    public static Atlas atlas;
    public static SortingBatch batch;
    public static AssetsManager assets;
    public static Camera2 camera;

    public static final TaskScheduler scheduler = new TaskScheduler();
    public static Application app;
}
