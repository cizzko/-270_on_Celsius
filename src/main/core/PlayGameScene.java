package core;

import core.EventHandling.Config;
import core.World.Creatures.Physics;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.WorkbenchMenu.WorkbenchLogic;
import core.World.Weather.Sun;
import core.World.WorldGenerator.Background;
import core.g2d.Shaders;
import core.g2d.StackfulRender;
import core.g2d.UniformBuffer;
import core.graphic.GuiDrawing;
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
        player.updateCamera();
        sun.update();
        Background.update();
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
}
