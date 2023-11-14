package core.UI.GUI.Menu;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Json;
import core.EventHandling.Logging.Logger;
import core.UI.GUI.CreateElement;
import core.Utils.SimpleColor;
import java.awt.Point;
import static core.EventHandling.Logging.Config.getFromConfig;
import static core.EventHandling.Logging.Json.getName;
import static core.UI.GUI.CreateElement.*;
import static core.Window.*;

public class Settings {
    public static boolean createdSettings = false, needUpdateCount = true;
    public static int pressedCount = 0;
    private static String newLang = Json.lang;

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
        otter();
    }

    public static void createGraphicsSett() {
        createSwapButton(310, 980, 32, 32, getName("InterpolateSunset"), getName("InterpolateSunsetPrompt"), false, new SimpleColor(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("InterpolateSunset")), "SettingsGraphicsSwap");
        createSwapButton(310, 910, 32, 32, getName("PreloadResources"), getName("PreloadResourcesPrompt"), false, new SimpleColor(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("PreloadResources")), "SettingsGraphicsSwap");
        createSwapButton(310, 840, 32, 32, getName("VerticalSync"), getName("VerticalSyncPrompt"), false, new SimpleColor(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("VerticalSync")), "SettingsGraphicsSwap");
    }

    public static void createBasicSett() {
        String[] langs = Json.getAllLanguagesArray();
        createDropButton(780, 950, 240, 65, langs, Json.getName("Language"), new SimpleColor(255, 80, 0, 55), "SettingsBasicDrop", langButton(langs));
        createSwapButton(310, 980, 32, 32, getName("ShowPrompts"), getName("ShowPromptsPrompt"), false, new SimpleColor(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("ShowPrompts")), "SettingsBasicSwap");
        createSwapButton(310, 910, 32, 32, getName("DetectLanguage"), getName("DetectLanguagePrompt"), false, new SimpleColor(236, 236, 236, 55), Boolean.parseBoolean(getFromConfig("DetectLanguage")), "SettingsBasicSwap");
        createPicture(745, 965, 1, "languageIcon", assetsDir("UI/GUI/languageIcon.png"), "SettingsBasic");
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
        Config.updateConfig("Language", newLang);
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

    private static Runnable[] langButton(String[] buttonNames) {
        Runnable[] tasks = new Runnable[buttonNames.length];

        for (int i = 0; i < tasks.length; i++) {
            int finalI = i;
            tasks[i] = () -> {
              Settings.newLang = buttonNames[finalI];
              buttons.get(getName("Language")).taskOnClick.run();
            };
        }
        return tasks;
    }

    private static void otter() {
        new Thread(() -> {
            boolean crawlingOut = false;
            boolean out = false;
            int pressedCount = 0;
            long lastPress = System.currentTimeMillis();
            long lastPosSwap = System.currentTimeMillis();
            Point pos = new Point(2160, -480);

            while (createdSettings) {
                if (EventHandler.getRectanglePress(1800, 0, 1920, 120) && !crawlingOut && System.currentTimeMillis() - lastPress >= 100) {
                    pressedCount++;
                    lastPress = System.currentTimeMillis();
                }
                if (pressedCount >= 5) {
                    crawlingOut = true;
                    pressedCount = 0;
                }
                if (crawlingOut && System.currentTimeMillis() - lastPosSwap >= 7) {
                    lastPosSwap = System.currentTimeMillis();
                    CreateElement.createPicture(pos.x, pos.y, 0, "otter", assetsDir("UI/comeOutOtter.png"), "Settings");

                    if (!out && pos.x > 1770 && pos.y < -90) {
                        pos.x -= 1;
                        pos.y += 1;
                    } else if (pos.x <= 1770 && pos.y >= -90) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            Logger.printException("Error when stop otter", e);
                        }
                        out = true;
                    }
                    if (out) {
                        pos.x += 1;
                        pos.y -= 1;
                    }
                    if (pos.x >= 2160 && pos.y <= -480) {
                        crawlingOut = false;
                        out = false;
                    }
                }
            }
        }).start();
    }
}
