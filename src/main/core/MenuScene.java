package core;

import core.UI.Styles;
import core.assets.AssetsManager;
import core.g2d.*;
import core.g2d.Render;
import core.util.Commandline;
import core.util.Debug;

import static core.Global.*;

public final class MenuScene extends GameScene {

    @Load(value = "sprites", owned = false)
    private Atlas sprites;
    @Load(value = "arial.ttf", owned = false, load = AssetsManager.LoadType.SYNC)
    private Font font;
    @Load(value = "World/Other/background.png", load = AssetsManager.LoadType.SYNC)
    private Texture backgroundTex;

    @Override
    public void onInit() {
        Debug.initMenu();

        camera.setToOrthographic(input.getWidth(), input.getHeight());
        StackfulRender.camera(camera);
    }

    @Override
    public void onLoaded() {
        Global.atlas = sprites;
        Window.defaultFont = font;

        Styles.loadAll();
        content.loadAll();

        UIMenus.mainMenu().show();
    }

    @Override
    protected void inputUpdate() {
        Debug.menuHotKeys();
        Commandline.inputUpdate();
    }

    @Override
    protected void update() {

    }

    @Override
    protected void draw() {
        drawLoading();
        uiScene.draw();
        Debug.drawTextValues();
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        UIMenus.createPlanet().reset();
    }

    @Override
    protected void drawLoading() {
        StackfulRender.z(Render.LAYER_BACKGROUND);
        StackfulRender.draw(backgroundTex, 0, 0, input.getWidth(), input.getHeight());
    }
}
