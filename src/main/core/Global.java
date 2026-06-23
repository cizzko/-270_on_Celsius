package core;

import core.World.World;
import core.assets.AssetsManager;
import core.content.ContentManager;
import core.content.EntityPool;
import core.content.creatures.PlayerEntity;
import core.g2d.Atlas;
import core.graphic.Camera;
import core.input.InputHandler;
import core.lang.LangTranslation;

import static core.util.Debug.rethrow;

public final class Global {
    private Global() {}

    public static InputHandler input;
    public static Atlas atlas;
    public static AssetsManager assets;
    public static UIScene uiScene;
    public static final LangTranslation lang = new LangTranslation();
    public static final EntityPool entityPool = new EntityPool(Constants.Entity.MAX_COUNT);
    public static PlayerEntity player;
    public static final Camera camera = new Camera(WorldCoordinates.BLOCK_SIZE);
    public static GameSettings gameSettings;

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
            gameScene = newGameScene;
        } catch (Exception e) {
            try {
                newGameScene.unload();
            } catch (Exception e1) {
                e.addSuppressed(e1);
            }
            gameScene = null;
            rethrow(e);
        }
    }

    public static final ContentManager content = new ContentManager();
    public static final TaskScheduler scheduler = new TaskScheduler();
    public static Application app;
}
