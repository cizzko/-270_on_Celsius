package core.ui.GUI.Menu;

import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Json;
import core.Time;
import core.UI;
import core.Utils.SimpleColor;
import core.math.Point2i;
import core.ui.Button;
import core.ui.Dialog;
import core.ui.Group;
import core.ui.ImageElement;

import java.util.HashMap;

import static core.EventHandling.Logging.Config.*;
import static core.EventHandling.Logging.Json.getName;
import static core.Global.atlas;
import static core.Global.scheduler;
import static core.Window.start;

public class Settings extends Dialog {
    private String newLang = Json.lang;
    private final Button save;
    private final Dialog basicSettings, otherSettings, graphicsSettings;

    public Settings() {
        var mainPanel = addPanel(20, 20, 1880, 1040);
        var categories = mainPanel.addPanel(40, 40, 240, 1000)
                .setSimple(false);
        categories.addButton(this::exitBtn)
                .set(40, 900, 240, 65)
                .setName(getName("Return"))
                .setSimple(true)
                .setColor(SimpleColor.DEFAULT_ORANGE);
        categories.addButton(this::basicBtn)
                .set(40, 200, 240, 65)
                .setName(getName("SettingsBasic"))
                .setSimple(true)
                .setColor(SimpleColor.DIRTY_BLACK);
        categories.addButton(this::otherBtn)
                .set(40, 100, 240, 65)
                .setName(getName("SettingsOther"))
                .setSimple(true)
                .setColor(SimpleColor.DIRTY_BLACK);
        save = categories.addButton(this::saveBtn)
                .set(40, 800, 240, 65)
                .setName(getName("SettingsSave"))
                .setSimple(true)
                .setColor(SimpleColor.DEFAULT_ORANGE);
        categories.addButton(this::graphicsBtn)
                .set(40, 300, 240, 65)
                .setName(getName("SettingsGraphics"))
                .setSimple(true)
                .setColor(SimpleColor.DIRTY_BLACK);
        graphicsSettings = mainPanel.add(new Dialog() {{
            visible = true;
            addToggleButton(() -> {
                boolean newState = !Boolean.parseBoolean(getFromConfig(INTERPOLATE_SUNSET_KEY));
                updateConfig(INTERPOLATE_SUNSET_KEY, Boolean.toString(newState));
            })
                    .set(310, 980, 44, 44)
                    .setName(getName(INTERPOLATE_SUNSET_KEY))
                    .setPrompt(getName("InterpolateSunsetPrompt"))
                    .setColor(SimpleColor.DIRTY_WHITE)
                    .setClicked(Boolean.parseBoolean(getFromConfig(INTERPOLATE_SUNSET_KEY)));
            addToggleButton(() -> {
                boolean newState = !Boolean.parseBoolean(getFromConfig(PRELOAD_RESOURCES_KEY));
                updateConfig(PRELOAD_RESOURCES_KEY, Boolean.toString(newState));
            })
                    .set(310, 910, 44, 44)
                    .setName(getName(PRELOAD_RESOURCES_KEY))
                    .setPrompt(getName("PreloadResourcesPrompt"))
                    .setColor(SimpleColor.DIRTY_WHITE)
                    .setClicked(Boolean.parseBoolean(getFromConfig(PRELOAD_RESOURCES_KEY)));
            addToggleButton(() -> {
                boolean newState = !Boolean.parseBoolean(getFromConfig(VERTICAL_SYNC_KEY));
                updateConfig(VERTICAL_SYNC_KEY, Boolean.toString(newState));
            })
                    .set(310, 840, 44, 44)
                    .setName(getName(VERTICAL_SYNC_KEY))
                    .setPrompt(getName("VerticalSyncPrompt"))
                    .setColor(SimpleColor.DIRTY_WHITE)
                    .setClicked(Boolean.parseBoolean(getFromConfig(VERTICAL_SYNC_KEY)));
        }});
        otherSettings = mainPanel.add(new Dialog() {{
            visible = false;
            addToggleButton(() -> {
            })
                    .set(310, 980, 44, 44)
                    .setName(getName(SEND_ANONYMOUS_STATISTIC_KEY))
                    .setPrompt(getName("SendAnonymousStatisticsPrompt"))
                    .setColor(SimpleColor.DIRTY_WHITE)
                    .setClicked(Boolean.parseBoolean(getFromConfig(SEND_ANONYMOUS_STATISTIC_KEY)));
        }});
        basicSettings = mainPanel.add(new Dialog() {{
            visible = false;
            var dropDownMenu = add(new Dialog() {{
                visible = false;
                var dropDown = this;
                String[] langs = Json.getAllLanguagesArray();
                int ox = 780;
                int oy = 950;
                int w = 240;
                int h = 65;
                for (int i = 0; i < langs.length; i++) {
                    String lang = langs[i];
                    addButton(() -> {
                        newLang = lang;
                        dropDown.toggleVisibility();
                    })
                            .set(ox, oy - (h * (i + 1)) + (i * 6) + 6, w, h)
                            .setColor(SimpleColor.DEFAULT_ORANGE)
                            .setName(lang);
                }
            }});
            addButton(dropDownMenu::toggleVisibility)
                    .set(780, 950, 240, 65)
                    .setName(Json.getName("Language"))
                    .setColor(SimpleColor.DEFAULT_ORANGE);
            addToggleButton(() -> {
            })
                    .set(310, 980, 44, 44)
                    .setName(getName(SHOW_PROMPTS_KEY))
                    .setPrompt(getName("ShowPromptsPrompt"))
                    .setColor(SimpleColor.DIRTY_WHITE)
                    .setClicked(Boolean.parseBoolean(getFromConfig(SHOW_PROMPTS_KEY)));
            addToggleButton(() -> {
            })
                    .set(310, 910, 44, 44)
                    .setName(getName(DETECT_LANGUAGE_KEY))
                    .setPrompt(getName("DetectLanguagePrompt"))
                    .setColor(SimpleColor.DIRTY_WHITE)
                    .setClicked(Boolean.parseBoolean(getFromConfig(DETECT_LANGUAGE_KEY)));
            addImage(745, 965, atlas.byPath("UI/GUI/languageIcon.png"));
        }});
        mainPanel.add(new OtterBox(this));
    }

    private void updateConfigAll() {
        Config.updateConfig("Language", newLang);
    }

    private void exitBtn() {
        hide();
        if (!start) {
            UI.mainMenu().show();
        }
    }

    private void saveBtn() {
        save.isClickable = false;
        updateConfigAll();
    }

    private void graphicsBtn() {
        basicSettings.setVisible(false);
        otherSettings.setVisible(false);
        graphicsSettings.setVisible(true);
    }

    private void basicBtn() {
        otherSettings.setVisible(false);
        graphicsSettings.setVisible(false);
        basicSettings.setVisible(true);
    }

    private void otherBtn() {
        basicSettings.setVisible(false);
        graphicsSettings.setVisible(false);
        otherSettings.setVisible(true);
    }

    static class OtterBox extends Dialog {
        private long lastPress;
        private int otterClicks;
        private boolean out;

        private final ImageElement otterImage;

        protected OtterBox(Group panel) {
            super(panel);
            add(new Button(this) {
                { onClick(OtterBox.this::countOtters); }
                @Override public void draw() {}
            })
            .set(1800, 0, 120, 120);
            otterImage = addImage(2160, -480, atlas.byPath("UI/comeOutOtter.png"));
            otterImage.setVisible(false);
        }

        private void countOtters() {
            if (!otterImage.visible() && (lastPress == 0 || System.currentTimeMillis() - lastPress >= 100)) {
                otterClicks++;
                lastPress = System.currentTimeMillis();
            }
        }

        @Override
        public void update() {
            super.update();

            if (otterClicks >= 5) {
                otterClicks = 0;
                otterImage.setVisible(true);
            }

            if (otterImage.visible()) {
                runOtter();
            }
        }

        private void runOtter() {
            int x = otterImage.x();
            int y = otterImage.y();

            if (!out && x > 1770 && y < -90) {
                x -= 1;
                y += 1;
            } else if (x <= 1770 && y >= -90) {
                scheduler.post(() -> out = true, Time.delta * Time.ONE_SECOND);
                out = true;
            }
            if (out) {
                x += 1;
                y -= 1;
            }

            otterImage.setPosition(x, y);

            if (x >= 2160 && y <= -480) {
                otterImage.setVisible(false);
                out = false;
            }
        }

    }
}
