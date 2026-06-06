package core;

import core.EventHandling.Config;
import core.World.Creatures.Physics;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Bullets;
import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;
import core.graphic.GuiDrawing;
import core.World.Weather.Sun;
import core.g2d.StackfulRender;
import core.graphic.WorldDrawing;
import core.util.Commandline;
import core.util.Debug;

import static core.EventHandling.EventHandler.updateHotkeys;
import static core.Global.*;
import static core.World.Creatures.Player.Player.*;
import static core.WorldCoordinates.toWorld;
import static core.g2d.Render.*;

public final class PlayGameScene extends GameScene {
    public final Sun sun = new Sun();
    public final PostEffect postEffect = new PostEffect();
    //надо что то думать с круглым миром
    public static boolean smoothedCamera;

    private boolean paused;

    public void togglePaused() {
        paused = !paused;
    }

    public void setPaused(boolean state) {
        paused = state;
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    public void onInit() {
        Debug.initPlaying();

        // Global.postEffect.use(true);

        updateCamera();
        smoothedCamera = Config.getBoolean("SmoothedCamera");

        UIMenus.headUpDisplay().show();
    }

    @Override
    protected void inputUpdate() {
        AutoSaveController.update();

        updateHotkeys(this);
        WorkbenchLogic.updateInput();
        Commandline.inputUpdate();
        updateToolInteraction();
        Inventory.inputUpdate();
    }

    @Override
    protected void update() {
        Physics.updatePhysics(this);
        updateCamera();
        postEffect.update();
        sun.update();
        updateInventoryInteraction();
        Bullets.updateBullets();
        world.update();
        Inventory.updateStaticBlocksPreview();
    }

    @Override
    protected void draw() {

        StackfulRender.z(LAYER_BACKGROUND);
        sun.draw();
        postEffect.draw();
        StackfulRender.z(LAYER_BLOCKS);
        StackfulRender.camera(camera); // Центрируем камеру на позицию игрока
        WorldDrawing.drawBlocks();
        StackfulRender.z(LAYER_ENTITIES);
        WorldDrawing.drawEntities();

        Debug.drawPlayerBorders();
        GuiDrawing.drawBlocksGui();
        Debug.drawDebugBorders();

        uiScene.draw();
        WorkbenchLogic.draw();
        Inventory.draw();
        drawCurrentHP();
        Debug.drawTextValues();
    }

    @Override
    protected void drawLoading() {

    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        player = null;
        world = null;
        entityPool.clear();
        WorldDrawing.resetState();
    }

    public static final float CAMERA_OFFSET_X = toWorld(32f);
    public static final float CAMERA_OFFSET_Y = toWorld(200f);

    public static void updateCamera() {
        if (player.isDead()) {
            return;
        }

        camera.updateLastPosition();

        if (smoothedCamera) {
            float base = 0.08f * Math.max(1, player.velocity().len() / 4f);
            base = Math.min(1f, base);
            float alpha = 1 - (float)Math.pow(1 - base, Time.delta);
            camera.position.lerp(player.x() + CAMERA_OFFSET_X, player.y() + CAMERA_OFFSET_Y, alpha);
        } else {
            camera.position.set(player.x() + CAMERA_OFFSET_X, player.y() + CAMERA_OFFSET_Y);
        }

        camera.update();
    }
}
