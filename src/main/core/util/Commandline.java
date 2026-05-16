package core.util;

import core.Global;
import core.UI.Styles;
import core.UI.TextField;

import static core.Global.input;
import static core.Global.uiScene;
import static org.lwjgl.glfw.GLFW.*;

public class Commandline {
    // private static final String prefix = Config.getFromFC("Prefix");

    private static final TextField consoleField = new TextField(null, Styles.DEFAULT_TEXT_FIELD)
            .set(20, 800, 650, 260);
    static {
        consoleField.enterCallback = JavaInterpreter::execute;
    }

    public static void inputUpdate() {
        if (input.justPressed(GLFW_KEY_F5)) {
            if (Global.uiScene.contains(consoleField)) {
                uiScene.remove(consoleField);
            } else {
                uiScene.add(consoleField);
            }
        }
    }
}
