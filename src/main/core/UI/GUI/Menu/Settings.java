package core.UI.GUI.Menu;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Json;
import core.EventHandling.Logging.Logger;
import core.UI.GUI.CreateElement;
import core.Utils.SimpleColor;
import core.World.Creatures.Physics;
import core.math.Point2i;

import static core.EventHandling.Logging.Config.getFromConfig;
import static core.EventHandling.Logging.Json.getName;
import static core.Global.atlas;
import static core.UI.GUI.CreateElement.*;
import static core.Window.start;

public class Settings {
    public static boolean createdSettings = false, needUpdateCount = true;
    public static int pressedCount = 0;
    private static String newLang = Json.lang;

    public static void create() {
        createPanel(20, 20, 1880, 1040, "defaultPanSettings", false, "Settings");
        createPanel(40, 40, 240, 1000, "leftPanSettings", true, "Settings");

        createButton(40, 900, 240, 65, getName("Return"), null, true, SimpleColor.DEFAULT_ORANGE, "Settings", Settings::exitBtn);
        createButton(40, 800, 240, 65, getName("SettingsSave"), null, true, SimpleColor.DEFAULT_ORANGE, "Settings", Settings::saveBtn);
        createButton(40, 300, 240, 65, getName("SettingsGraphics"), null, true, SimpleColor.DIRTY_BLACK, "Settings", Settings::graphicsBtn);
        createButton(40, 200, 240, 65, getName("SettingsBasic"), null, true, SimpleColor.DIRTY_BLACK, "Settings", Settings::basicBtn);
        createButton(40, 100, 240, 65, getName("SettingsOther"), null, true, SimpleColor.DIRTY_BLACK, "Settings", Settings::otherBtn);

        buttons.get(Json.getName("SettingsSave")).setClickable(false);
        buttons.get(getName("SettingsGraphics")).setClickable(false);

        createdSettings = true;
        createGraphicsSet();
        otter();
    }

    public static void createGraphicsSet() {
        createSwapButton(310, 980, 32, 32, getName("InterpolateSunset"), getName("InterpolateSunsetPrompt"), false, true, SimpleColor.DIRTY_WHITE, Boolean.parseBoolean(getFromConfig("InterpolateSunset")), "SettingsGraphicsSwap");
        createSwapButton(310, 910, 32, 32, getName("PreloadResources"), getName("PreloadResourcesPrompt"), false, true, SimpleColor.DIRTY_WHITE, Boolean.parseBoolean(getFromConfig("PreloadResources")), "SettingsGraphicsSwap");
        createSwapButton(310, 840, 32, 32, getName("VerticalSync"), getName("VerticalSyncPrompt"), false, true, SimpleColor.DIRTY_WHITE, Boolean.parseBoolean(getFromConfig("VerticalSync")), "SettingsGraphicsSwap");
    }

    public static void createBasicSet() {
        String[] langs = Json.getAllLanguagesArray();
        createDropButton(780, 950, 240, 65, langs, Json.getName("Language"), SimpleColor.DEFAULT_ORANGE, "SettingsBasicDrop", langButton(langs));
        createSwapButton(310, 980, 32, 32, getName("ShowPrompts"), getName("ShowPromptsPrompt"), false, true, SimpleColor.DIRTY_WHITE, Boolean.parseBoolean(getFromConfig("ShowPrompts")), "SettingsBasicSwap");
        createSwapButton(310, 910, 32, 32, getName("DetectLanguage"), getName("DetectLanguagePrompt"), false, true, SimpleColor.DIRTY_WHITE, Boolean.parseBoolean(getFromConfig("DetectLanguage")), "SettingsBasicSwap");
        createPicture(745, 965, 1, "languageIcon", atlas.byPath("UI/GUI/languageIcon.png"), "SettingsBasic");
    }

    public static void createOtherSet() {
        createSwapButton(310, 980, 32, 32, getName("Autopause"), getName("AutopausePrompt"), false, true, SimpleColor.DIRTY_WHITE, Boolean.parseBoolean(getFromConfig("Autopause")), "SettingsOtherSwap");
    }

    public static void deleteGraphicsSet() {
        buttons.values().stream().filter(button -> button.getGroup().contains("SettingsGraphics")).forEach(button -> button.setVisible(false));
    }

    public static void deleteBasicSet() {
        buttons.values().stream().filter(button -> button.getGroup().contains("SettingsBasic")).forEach(button -> button.setVisible(false));
        panels.values().stream().filter(button -> button.getGroup().contains("SettingsBasic")).forEach(button -> button.setVisible(false));
    }

    public static void deleteOtherSet() {
        buttons.values().stream().filter(button -> button.getGroup().contains("SettingsOther")).forEach(button -> button.setVisible(false));
    }

    public static void delete() {
        panels.values().stream().filter(button -> button.getGroup().equals("Settings")).forEach(button -> button.setVisible(false));
        buttons.values().stream().filter(button -> button.getGroup().equals("Settings")).forEach(button -> button.setVisible(false));

        deleteGraphicsSet();
        deleteBasicSet();
        deleteOtherSet();
        createdSettings = false;
    }

    public static void updateConfigAll() {
        buttons.values().stream().filter(button -> button.getGroup().contains("Swap") && button.isVisible()).forEach(button -> Config.updateConfig(Json.getKey(button.getName()), String.valueOf(button.isClicked())));
        Config.updateConfig("Language", newLang);
    }

    private static void exitBtn() {
        Physics.resumePhysics();
        delete();
        if (!start) {
            Main.create();
        }
    }

    private static void saveBtn() {
        buttons.get(Json.getName("SettingsSave")).setClickable(false);
        updateConfigAll();
    }

    private static void graphicsBtn() {
        deleteBasicSet();
        deleteOtherSet();
        createGraphicsSet();

        buttons.get(Json.getName("SettingsGraphics")).setClickable(false);
        buttons.get(Json.getName("SettingsBasic")).setClickable(true);
        buttons.get(Json.getName("SettingsOther")).setClickable(true);
        buttons.get(Json.getName("SettingsSave")).setClickable(false);
        needUpdateCount = true;
    }

    private static void basicBtn() {
        createBasicSet();
        deleteOtherSet();
        deleteGraphicsSet();

        buttons.get(Json.getName("SettingsBasic")).setClickable(false);
        buttons.get(Json.getName("SettingsGraphics")).setClickable(true);
        buttons.get(Json.getName("SettingsOther")).setClickable(true);
        buttons.get(Json.getName("SettingsSave")).setClickable(false);
        needUpdateCount = true;
    }

    private static void otherBtn() {
        deleteBasicSet();
        createOtherSet();
        deleteGraphicsSet();

        buttons.get(Json.getName("SettingsOther")).setClickable(false);
        buttons.get(Json.getName("SettingsGraphics")).setClickable(true);
        buttons.get(Json.getName("SettingsBasic")).setClickable(true);
        buttons.get(Json.getName("SettingsSave")).setClickable(false);
        needUpdateCount = true;
    }

    private static Runnable[] langButton(String[] buttonNames) {
        Runnable[] tasks = new Runnable[buttonNames.length];

        for (int i = 0; i < tasks.length; i++) {
            int finalI = i;
            tasks[i] = () -> {
              newLang = buttonNames[finalI];
              buttons.get(getName("Language")).getTaskOnClick().run();
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
            Point2i pos = new Point2i(2160, -480);

            while (createdSettings) {
                if (EventHandler.getRectangleClick(1800, 0, 1920, 120) && !crawlingOut && System.currentTimeMillis() - lastPress >= 100) {
                    pressedCount++;
                    lastPress = System.currentTimeMillis();
                }
                if (pressedCount >= 5) {
                    crawlingOut = true;
                    pressedCount = 0;
                }
                if (crawlingOut && System.currentTimeMillis() - lastPosSwap >= 7) {
                    lastPosSwap = System.currentTimeMillis();
                    CreateElement.createPicture(pos.x, pos.y, 0, "otter", atlas.byPath("UI/comeOutOtter.png"), "Settings");

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
