package core;

import core.EventHandling.EventHandler;
import core.assets.AssetsManager;
import core.g2d.Atlas;
import core.g2d.Font;
import core.g2d.Texture;
import core.ui.Styles;

import static core.Global.batch;
import static core.Global.camera;
import static core.Global.input;
import static core.Global.uiScene;

public final class MenuScene extends GameScene {

    @Load(value = "arial.ttf", owned = false)
    private Font font;
    @Load(value = "sprites", owned = false)
    private Atlas sprites;
    @Load(value = "World/Other/background.png", load = AssetsManager.LoadType.SYNC)
    private Texture backgroundTex;

    @Override
    public void onInit() {
        camera.setToOrthographic(input.getWidth(), input.getHeight());
        batch.matrix(camera.projection);
    }

    @Override
    public void onLoaded() {
        Global.atlas = sprites;
        Window.defaultFont = font;

        Styles.loadAll();
        EventHandler.init();
        UI.mainMenu().show();
    }

    @Override
    protected void inputUpdate() {

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
    protected void drawLoading() {
        batch.draw(backgroundTex, 0, 0, input.getWidth(), input.getHeight());
    }
}
