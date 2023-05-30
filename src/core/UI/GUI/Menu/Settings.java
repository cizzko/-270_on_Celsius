package core.UI.GUI.Menu;

import java.awt.*;
import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;

public class Settings {

    public static void create() {
        createPanel(20, 20, 1880, 1040, "defaultPanSettings", false);
        createPanel(40, 40, 240, 1000, "leftPanSettings", true);

        createButton(40, 915, 240, 65, getName("SettingsExit"), true, new Color(255, 80, 0, 55));
        createButton(40, 300, 240, 65, getName("SettingsGraphics"), true, new Color(0, 0, 0, 50));
        createButton(40, 203, 240, 65, getName("SettingsSound"), true, new Color(0, 0, 0, 50));
        createButton(40, 105, 240, 65, getName("SettingsOther"), true, new Color(0, 0, 0, 50));
    }

    public static void delete() {
        panels.get("defaultPanSettings").visible = false;
        panels.get("leftPanSettings").visible = false;

        buttons.get(getName("SettingsExit")).visible = false;
        buttons.get(getName("SettingsGraphics")).visible = false;
        buttons.get(getName("SettingsSound")).visible = false;
        buttons.get(getName("SettingsOther")).visible = false;
    }
}
