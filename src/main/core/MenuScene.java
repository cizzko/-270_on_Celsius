package core;

import core.assets.AssetsManager;
import core.g2d.*;
import core.ui.Styles;
import core.util.Commandline;
import core.util.Debug;

import static core.Global.*;
import static core.Window.glfwHandle;
import static org.lwjgl.glfw.GLFW.*;

public final class MenuScene extends GameScene {

    @Load(value = "sprites", owned = false)
    private Atlas sprites;
    @Load(value = "arial.ttf", owned = false)
    private Font font;
    @Load(value = "World/Other/background.png", load = AssetsManager.LoadType.SYNC)
    private Texture backgroundTex;

    @Override
    public void onInit() {
        Debug.initMenu();
    }

    @Override
    public void onLoaded() {
        Global.atlas = sprites;
        Window.defaultFont = font;

        Styles.loadAll();
        Shaders.loadAll();
        content.loadAll();

        UIMenus.mainMenu().show();

        if (glfwGetWindowAttrib(glfwHandle, GLFW_VISIBLE) == GLFW_FALSE)
            glfwShowWindow(glfwHandle);
    }

    @Override
    protected void inputUpdate() {
        Debug.menuHotKeys();
        Commandline.inputUpdate();
        Hotkeys.inputUpdate();
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
        StackfulRender.draw(backgroundTex, 0, 0, input.viewportWidth(), input.viewportHeight());
    }
}
