package core.UI.GUI.Menu;

import core.EventHandling.Logging.Json;
import java.awt.*;
import static core.EventHandling.Logging.Config.getFromConfig;
import static core.EventHandling.Logging.Config.updateConfig;
import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;

public class Settings {
    public static boolean createdSettings = false, needUpdateCount = true;
    private static boolean otherVisible = false, graphicsVisible = true, basicVisible = false;
    public static int pressedCount = 0;

    public static void create() {
        createPanel(20, 20, 1880, 1040, "defaultPanSettings", false, "Settings");
        createPanel(40, 40, 240, 1000, "leftPanSettings", true, "Settings");

        createButton(40, 900, 240, 65, getName("SettingsExit"), null, true, new Color(255, 80, 0, 55), "Settings");
        createButton(40, 800, 240, 65, getName("SettingsSave"), null, true, new Color(255, 80, 0, 55), "Settings");

        createButton(40, 300, 240, 65, getName("SettingsGraphics"), null, true, new Color(0, 0, 0, 50), "Settings");
        createButton(40, 200, 240, 65, getName("SettingsBasic"), null, true, new Color(0, 0, 0, 50), "Settings");
        createButton(40, 100, 240, 65, getName("SettingsOther"), null, true, new Color(0, 0, 0, 50), "Settings");
        buttons.get(Json.getName("SettingsSave")).isClickable = false;

        createdSettings = true;
        createGraphicsSett();
    }

    public static void createGraphicsSett() {
        graphicsVisible = true;
        createSwapButton(310, 980, 32, 32, getName("InterpolateSunset"), getName("InterpolateSunsetPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("InterpolateSunset")), "SettingsGraphics");
        createSwapButton(310, 910, 32, 32, getName("PreloadTextures"), getName("PreloadTexturesPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("PreloadTextures")), "SettingsGraphics");
        createSwapButton(310, 840, 32, 32, getName("VerticalSync"), getName("VerticalSyncPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("VerticalSync")), "SettingsGraphics");
    }


    public static void createBasicSett() {
        basicVisible = true;
        createSwapButton(310, 980, 32, 32, getName("ShowPrompts"), getName("ShowPromptsPrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("ShowPrompts")), "SettingsBasic");
    }

    public static void createOtherSett() {
        otherVisible = true;
        createSwapButton(310, 980, 32, 32, getName("AnonState"), getName("AnonStatePrompt"), false, new Color(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("SendAnonymousStatistics")), "SettingsOther");
    }

    public static void deleteGraphicsSett() {
        graphicsVisible = false;
        buttons.values().stream().filter(button -> button.group.equals("SettingsGraphics")).forEach(button -> button.visible = false);
    }

    public static void deleteBasicSett() {
        basicVisible = false;
        buttons.values().stream().filter(button -> button.group.equals("SettingsBasic")).forEach(button -> button.visible = false);
    }

    public static void deleteOtherSett() {
        otherVisible = false;
        buttons.values().stream().filter(button -> button.group.equals("SettingsOther")).forEach(button -> button.visible = false);
    }

    public static void delete() {
        panels.values().stream().filter(button -> button.group.equals("Settings")).forEach(button -> button.visible = false);
        buttons.values().stream().filter(button -> button.group.equals("Settings")).forEach(button -> button.visible = false);

        deleteGraphicsSett();
        deleteBasicSett();
        deleteOtherSett();
        createdSettings = false;
    }

    public static void updateConfigAll() {
        if (graphicsVisible) {
            updateConfig("InterpolateSunset", String.valueOf(buttons.get(getName("InterpolateSunset")).isClicked));
            updateConfig("PreloadTextures", String.valueOf(buttons.get(getName("PreloadTextures")).isClicked));
            updateConfig("VerticalSync", String.valueOf(buttons.get(getName("VerticalSync")).isClicked));
        } else if (basicVisible) {
            updateConfig("ShowPrompts", String.valueOf(buttons.get(getName("ShowPrompts")).isClicked));
        } else if (otherVisible) {
            updateConfig("SendAnonymousStatistics", String.valueOf(buttons.get(getName("AnonState")).isClicked));
        }
    }
}
