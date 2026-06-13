package core;

import core.util.Config;
import core.ui.menu.Pause;
import org.lwjgl.glfw.GLFW;

import static core.Global.input;
import static core.Global.uiScene;
import static core.Window.windowFocused;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public final class Hotkeys {
    public static void inputUpdate() {
        if (Global.input.justPressed(GLFW.GLFW_KEY_F12)) {
            Window.toggleFullscreen();
        }
    }

    public static boolean isMouseClickedIn(float minX, float minY, float maxX, float maxY) {
        var mousePos = input.mousePos();

        return mousePos.x >= minX && mousePos.x <= maxX &&
               mousePos.y >= minY && mousePos.y <= maxY &&
               input.justClicked(GLFW_MOUSE_BUTTON_LEFT);
    }

    public static void updateHotkeys(PlayGameScene scene) {
        Pause pauseMenu = UIMenus.pause();
        if (Global.input.justPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)) {
            boolean toggle = pauseMenu.toggle();
            if (toggle) {
                uiScene.setFocus(pauseMenu);
            }
            scene.setPaused(toggle);
        } else {
            if (scene.isPaused()) {
                return;
            }

            if (!windowFocused && Config.getBoolean("Autopause")) {
                UIMenus.pause().show();
                scene.setPaused(true);
            }
        }

    }
}
