package core.UI.GUI.Menu;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Json;
import core.World.Textures.SimpleColor;
import static core.EventHandling.Logging.Config.getFromConfig;
import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;
import static core.Window.defPath;
import static core.Window.start;

public class Settings {
    public static boolean createdSettings = false, needUpdateCount = true;
    public static int pressedCount = 0;

    public static void create() {
        createPanel(20, 20, 1880, 1040, "defaultPanSettings", false, "Settings");
        createPanel(40, 40, 240, 1000, "leftPanSettings", true, "Settings");

        createButton(40, 900, 240, 65, getName("SettingsExit"), null, true, new SimpleColor(255, 80, 0, 55), "Settings", Settings::exitBtn);
        createButton(40, 800, 240, 65, getName("SettingsSave"), null, true, new SimpleColor(255, 80, 0, 55), "Settings", Settings::saveBtn);
        createButton(40, 300, 240, 65, getName("SettingsGraphics"), null, true, new SimpleColor(0, 0, 0, 50), "Settings", Settings::graphicsBtn);
        createButton(40, 200, 240, 65, getName("SettingsBasic"), null, true, new SimpleColor(0, 0, 0, 50), "Settings", Settings::basicBtn);
        createButton(40, 100, 240, 65, getName("SettingsOther"), null, true, new SimpleColor(0, 0, 0, 50), "Settings", Settings::otherBtn);

        buttons.get(Json.getName("SettingsSave")).isClickable = false;
        buttons.get(getName("SettingsGraphics")).isClickable = false;

        createdSettings = true;
        createGraphicsSett();
    }

    public static void createGraphicsSett() {
        createSwapButton(310, 980, 32, 32, getName("InterpolateSunset"), getName("InterpolateSunsetPrompt"), false, new SimpleColor(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("InterpolateSunset")), "SettingsGraphicsSwap");
        createSwapButton(310, 910, 32, 32, getName("PreloadTextures"), getName("PreloadTexturesPrompt"), false, new SimpleColor(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("PreloadTextures")), "SettingsGraphicsSwap");
        createSwapButton(310, 840, 32, 32, getName("VerticalSync"), getName("VerticalSyncPrompt"), false, new SimpleColor(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("VerticalSync")), "SettingsGraphicsSwap");
    }


    public static void createBasicSett() {
        createDropMenu(780, 950, 240, 65, Json.getAllLanguagesArray(), Json.lang, Json.getName("Language"), new SimpleColor(255, 80, 0, 55), "SettingsBasicDrop");
        createSwapButton(310, 980, 32, 32, getName("ShowPrompts"), getName("ShowPromptsPrompt"), false, new SimpleColor(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("ShowPrompts")), "SettingsBasicSwap");
        createSwapButton(310, 910, 32, 32, getName("DetectLanguage"), getName("DetectLanguagePrompt"), false, new SimpleColor(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("DetectLanguage")), "SettingsBasicSwap");
        createPicture(745, 965, 1, "languageIcon", defPath + "\\src\\assets\\UI\\GUI\\languageIcon.png", "SettingsBasic");
    }

    public static void createOtherSett() {
        createSwapButton(310, 980, 32, 32, getName("SendAnonymousStatistics"), getName("SendAnonymousStatisticsPrompt"), false, new SimpleColor(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("SendAnonymousStatistics")), "SettingsOtherSwap");
    }

    public static void deleteGraphicsSett() {
        buttons.values().stream().filter(button -> button.group.contains("SettingsGraphics")).forEach(button -> button.visible = false);
    }

    public static void deleteBasicSett() {
        buttons.values().stream().filter(button -> button.group.contains("SettingsBasic")).forEach(button -> button.visible = false);
        panels.values().stream().filter(button -> button.group.contains("SettingsBasic")).forEach(button -> button.visible = false);
    }

    public static void deleteOtherSett() {
        buttons.values().stream().filter(button -> button.group.contains("SettingsOther")).forEach(button -> button.visible = false);
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
        buttons.values().stream().filter(button -> button.group.contains("Swap") && button.visible).forEach(button -> Config.updateConfig(Json.getKey(button.name), String.valueOf(button.isClicked)));
        buttons.values().stream().filter(button -> button.group.contains("Drop") && button.visible).forEach(button -> Config.updateConfig(Json.getKey(button.name), EventHandler.getDropMenuClicks(button.name)));
        buttons.values().stream().filter(button -> button.group.contains("Drop") && button.visible).forEach(button -> button.isClicked = false);
    }

    private static void exitBtn() {
        Settings.delete();
        if (!start) {
            Main.create();
        }
    }

    private static void saveBtn() {
        buttons.get(Json.getName("SettingsSave")).isClickable = false;
        Settings.updateConfigAll();
    }

    private static void graphicsBtn() {
        Settings.deleteBasicSett();
        Settings.deleteOtherSett();
        Settings.createGraphicsSett();

        buttons.get(Json.getName("SettingsGraphics")).isClickable = false;
        buttons.get(Json.getName("SettingsBasic")).isClickable = true;
        buttons.get(Json.getName("SettingsOther")).isClickable = true;
        buttons.get(Json.getName("SettingsSave")).isClickable = false;
        Settings.needUpdateCount = true;
    }

    private static void basicBtn() {
        Settings.createBasicSett();
        Settings.deleteOtherSett();
        Settings.deleteGraphicsSett();

        buttons.get(Json.getName("SettingsBasic")).isClickable = false;
        buttons.get(Json.getName("SettingsGraphics")).isClickable = true;
        buttons.get(Json.getName("SettingsOther")).isClickable = true;
        buttons.get(Json.getName("SettingsSave")).isClickable = false;
        Settings.needUpdateCount = true;
    }

    private static void otherBtn() {
        Settings.deleteBasicSett();
        Settings.createOtherSett();
        Settings.deleteGraphicsSett();

        buttons.get(Json.getName("SettingsOther")).isClickable = false;
        buttons.get(Json.getName("SettingsGraphics")).isClickable = true;
        buttons.get(Json.getName("SettingsBasic")).isClickable = true;
        buttons.get(Json.getName("SettingsSave")).isClickable = false;
        Settings.needUpdateCount = true;
    }
}
