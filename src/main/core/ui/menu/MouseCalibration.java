package core.ui.menu;

import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Json;
import core.Global;
import core.Utils.SimpleColor;
import java.awt.Robot;
import static core.EventHandling.Logging.Logger.printException;
import static core.Window.glfwWindow;
import static org.lwjgl.glfw.GLFW.*;

public class MouseCalibration {
    public static float xMultiplier = Float.parseFloat(Config.getFromConfig("MouseMultiplierX")), yMultiplier = Float.parseFloat(Config.getFromConfig("MouseMultiplierY"));

    public static void create() {
        glfwSetInputMode(glfwWindow, GLFW_CURSOR, GLFW_CURSOR_NORMAL);

        // createPanel(400, 400, 1120, 400, "CalibratePan", true, "MouseCalibration");
        // createButton(860, 410, 240, 65, Json.getName("CalibrateMouseContinue"), null, true, SimpleColor.DEFAULT_ORANGE, "MouseCalibration", null);
        // createText(410, 770, "CalibratePanText", Json.getName("CalibrateMouseText"), SimpleColor.DIRTY_BRIGHT_WHITE, "MouseCalibration");

        try {
            new Robot().mouseMove(960, 440);
        } catch (Exception e) {
            printException("Error when moving mouse", e);
        }

        // update();
    }

    public static void delete() {
        glfwSetInputMode(glfwWindow, GLFW_CURSOR, GLFW_CURSOR_HIDDEN);

        // buttons.values().stream().filter(button -> button.group.equals("MouseCalibration")).forEach(button -> button.visible = false);
        // panels.values().stream().filter(button -> button.group.equals("MouseCalibration")).forEach(button -> button.visible = false);
        // texts.values().stream().filter(button -> button.group.equals("MouseCalibration")).forEach(button -> button.visible = false);
    }

    /*
    public static void update() {
        new Thread(() -> {
            while (!buttons.get(Json.getName("CalibrateMouseContinue")).isClicked) {
                if (Global.input.pressed(GLFW_KEY_A) || Global.input.pressed(GLFW_KEY_LEFT)) {
                    xMultiplier -= 0.01f;

                } else if (Global.input.pressed(GLFW_KEY_D) || Global.input.pressed(GLFW_KEY_RIGHT)) {
                    xMultiplier += 0.01f;

                } else {
                    if (Global.input.pressed(GLFW_KEY_S) || Global.input.pressed(GLFW_KEY_DOWN)) {
                        yMultiplier -= 0.01f;

                    } else {
                        if (Global.input.pressed(GLFW_KEY_W) || Global.input.pressed(GLFW_KEY_UP)) {
                            yMultiplier += 0.01f;

                        }
                    }
                }
            }
            Config.updateConfig("MouseMultiplierX", String.valueOf(xMultiplier));
            Config.updateConfig("MouseMultiplierY", String.valueOf(yMultiplier));

            delete();
        }).start();
    }
     */
}
