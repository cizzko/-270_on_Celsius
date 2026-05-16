package core;

import core.EventHandling.EventHandler;
import core.UI.Styles;
import core.UI.menu.CreatePlanet;
import core.assets.AssetsManager;
import core.g2d.Atlas;
import core.g2d.Font;
import core.g2d.Texture;
import core.util.Commandline;
import core.util.DebugTools;

import static core.Global.*;
import static org.lwjgl.glfw.GLFW.*;

public final class MenuScene extends GameScene {

    @Load(value = "arial.ttf", owned = false)
    private Font font;
    @Load(value = "sprites", owned = false)
    private Atlas sprites;
    @Load(value = "World/Other/background.png", load = AssetsManager.LoadType.SYNC)
    private Texture backgroundTex;

    @Override
    public void onInit() {
        DebugTools.initMenu();

        camera.setToOrthographic(input.getWidth(), input.getHeight());
        batch.matrix(camera.projection);
    }

    @Override
    public void onLoaded() {
        Global.atlas = sprites;
        Window.defaultFont = font;

        Styles.loadAll();
        content.loadAll();

        EventHandler.init();
        UIMenus.mainMenu().show();
    }

    @Override
    protected void inputUpdate() {
        DebugTools.menuHotKeys();
        Commandline.inputUpdate();
    }

    @Override
    protected void update() {

    }

    @Override
    protected void draw() {
        drawLoading();
        uiScene.draw();
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        UIMenus.createPlanet().reset();
    }

    @Override
    protected void drawLoading() {
        batch.draw(backgroundTex, 0, 0, input.getWidth(), input.getHeight());
    }
}
