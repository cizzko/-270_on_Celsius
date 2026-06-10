package core;

import core.EventHandling.Config;
import core.World.Creatures.Physics;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;
import core.g2d.*;
import core.graphic.GuiDrawing;
import core.World.Weather.Sun;
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
        sun.update();
        updateInventoryInteraction();
        world.update();
        Inventory.updateStaticBlocksPreview();
    }

    @Override
    protected void draw() {

        StackfulRender.z(LAYER_BACKGROUND);
        sun.draw();
        StackfulRender.z(LAYER_BLOCKS);
        StackfulRender.camera(camera);
        WorldDrawing.drawBlocks();
        Debug.drawPlayerBorders();

        StackfulRender.z(LAYER_ENTITIES);
        try (var state = StackfulRender.pushState()) {
            var worldShader = Shaders.world;
            state.shader = worldShader;
            var uniformBuffer = queue().uniformBuffer();
            var ublock = uniformBuffer.allocate(worldShader);
            ublock.push(UniformBuffer.Uniform.of("u_logical_ratio", Global.camera.projectionScale));
            uniformBuffer.push(ublock);

            state.uniformBlock(ublock);
            WorldDrawing.drawEntities();
        }

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
