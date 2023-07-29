package core.UI.GUI.Menu;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Json;
import java.awt.*;
import static core.EventHandling.Logging.Logger.log;
import static core.UI.GUI.CreateElement.*;
import static core.Window.glfwWindow;
import static org.lwjgl.glfw.GLFW.*;

public class MouseCalibration {
    public static float xMultiplier = Float.parseFloat(Config.getFromConfig("MouseMultiplierX")), yMultiplier = Float.parseFloat(Config.getFromConfig("MouseMultiplierY"));

    public static void create() {
        glfwSetInputMode(glfwWindow, GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        createPanel(400, 400, 1120, 400, "CalibratePan", true, "MouseCalibration");
        createButton(860, 410, 240, 65, Json.getName("CalibrateMouseContinue"), null, true, new Color(255, 80, 0, 55), "MouseCalibration", null);
        createText(410, 770, "CalibratePanText", Json.getName("CalibrateMouseText"), new Color(210, 210, 210, 255), "MouseCalibration");

        try {
            new Robot().mouseMove(960, 440);
        } catch (Exception e) {
            log(e.toString());
        }

        update();
    }

    public static void delete() {
        glfwSetInputMode(glfwWindow, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

        buttons.values().stream().filter(button -> button.group.equals("MouseCalibration")).forEach(button -> button.visible = false);
        panels.values().stream().filter(button -> button.group.equals("MouseCalibration")).forEach(button -> button.visible = false);
        texts.values().stream().filter(button -> button.group.equals("MouseCalibration")).forEach(button -> button.visible = false);
    }

    public static void update() {
        new Thread(() -> {
            while (!buttons.get(Json.getName("CalibrateMouseContinue")).isClicked) {
                if (EventHandler.getKeyClick(GLFW_KEY_A) || EventHandler.getKeyClick(GLFW_KEY_LEFT)) {
                    xMultiplier -= 0.01f;

                } else if (EventHandler.getKeyClick(GLFW_KEY_D) || EventHandler.getKeyClick(GLFW_KEY_RIGHT)) {
                    xMultiplier += 0.01f;

                } else if (EventHandler.getKeyClick(GLFW_KEY_S) || EventHandler.getKeyClick(GLFW_KEY_DOWN)) {
                    yMultiplier -= 0.01f;

                } else if (EventHandler.getKeyClick(GLFW_KEY_W) || EventHandler.getKeyClick(GLFW_KEY_UP)) {
                    yMultiplier += 0.01f;

                }
            }
            Config.updateConfig("MouseMultiplierX", String.valueOf(xMultiplier));
            Config.updateConfig("MouseMultiplierY", String.valueOf(yMultiplier));

            delete();
        }).start();
    }
}
