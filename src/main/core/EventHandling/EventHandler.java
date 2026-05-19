package core.EventHandling;

import core.PlayGameScene;
import core.UIMenus;
import core.math.Point2i;

import static core.Global.*;
import static core.Window.windowFocused;
import static org.lwjgl.glfw.GLFW.*;

public class EventHandler {

    public static boolean isMouseClickedIn(float minX, float minY, float maxX, float maxY) {
        Point2i mousePos = input.mousePos();

        return mousePos.x >= minX && mousePos.x <= maxX &&
                mousePos.y >= minY && mousePos.y <= maxY &&
                input.justClicked(GLFW_MOUSE_BUTTON_LEFT);
    }

    public static void updateHotkeys(PlayGameScene scene) {
        if (input.justPressed(GLFW_KEY_ESCAPE)) {
            UIMenus.pause().toggle();
        }

        if (!windowFocused && Config.getBoolean("Autopause")) {
            UIMenus.pause().show();
        }

        scene.setPaused(UIMenus.pause().isShown());
    }

}
