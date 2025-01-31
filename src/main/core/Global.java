package core;

import core.World.World;
import core.assets.AssetsManager;
import core.g2d.Atlas;
import core.g2d.*;
import core.input.InputHandler;

import static core.Utils.DebugTools.rethrow;

public final class Global {
    private Global() {}

    public static InputHandler input;
    public static Atlas atlas;
    public static SortingBatch batch;
    public static AssetsManager assets;
    public static Scene scene;
    public static final Camera2 camera = new Camera2();

    public static World world;
    public static GameState gameState = GameState.MENU;
    public static GameScene gameScene;

    public static void setGameScene(GameScene newGameScene) {
        var oldGameScene = gameScene;
        if (oldGameScene != null) {
            newGameScene.onTransition(oldGameScene);
            oldGameScene.unload();
        }
        try {
            newGameScene.init();
        } catch (Exception e) {
            newGameScene.unload();
            rethrow(e);
        }
        gameScene = newGameScene;
    }

    public static final TaskScheduler scheduler = new TaskScheduler();
    public static Application app;
}
