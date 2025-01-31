package core.ui.menu;

import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Json;
import core.GameState;
import core.Global;
import core.Time;
import core.UI;
import core.math.Vector2f;
import core.ui.*;

import static core.EventHandling.Logging.Config.*;
import static core.EventHandling.Logging.Json.getName;
import static core.Global.atlas;
import static core.Global.scheduler;

public class Settings extends Dialog {
    private String newLang = Json.lang;
    private final Button save;
    private final Dialog basicSettings, otherSettings, graphicsSettings;

    public Settings() {
        var mainPanel = addPanel(Styles.DEFAULT_PANEL, 20, 20, 1880, 1040);
        var categories = mainPanel.addPanel(Styles.DEFAULT_PANEL, 40, 40, 240, 1000);
        categories.addButton(Styles.SIMPLE_TEXT_BUTTON, this::exitBtn)
                .set(40, 900, 240, 65)
                .setName(getName("Return"));

        categories.oneOf(
                categories.addButton(Styles.SIMPLE_TEXT_BUTTON, this::basicBtn)
                        .set(40, 200, 240, 65)
                        .setName(getName("SettingsBasic"))
                        .setColor(Styles.DIRTY_BLACK),
                categories.addButton(Styles.SIMPLE_TEXT_BUTTON, this::otherBtn)
                        .set(40, 100, 240, 65)
                        .setName(getName("SettingsOther"))
                        .setColor(Styles.DIRTY_BLACK),
                categories.addButton(Styles.SIMPLE_TEXT_BUTTON, this::graphicsBtn)
                        .set(40, 300, 240, 65)
                        .setName(getName("SettingsGraphics"))
                        .setColor(Styles.DIRTY_BLACK)
        );

        save = categories.addButton(Styles.TEXT_BUTTON, this::saveBtn)
                .set(40, 800, 240, 65)
                .setName(getName("SettingsSave"));
        graphicsSettings = mainPanel.add(new Dialog() {{
            setVisible(true);
            addToggleButton(Styles.DEFAULT_TOGGLE_BUTTON, () -> {
                boolean newState = !Boolean.parseBoolean(getFromConfig(INTERPOLATE_SUNSET_KEY));
                updateConfig(INTERPOLATE_SUNSET_KEY, Boolean.toString(newState));
            })
                    .setPosition(310, 980)
                    .setName(getName(INTERPOLATE_SUNSET_KEY))
                    .setPrompt(getName("InterpolateSunsetPrompt"))
                    .setClicked(Boolean.parseBoolean(getFromConfig(INTERPOLATE_SUNSET_KEY)));
            addToggleButton(Styles.DEFAULT_TOGGLE_BUTTON, () -> {
                boolean newState = !Boolean.parseBoolean(getFromConfig(PRELOAD_RESOURCES_KEY));
                updateConfig(PRELOAD_RESOURCES_KEY, Boolean.toString(newState));
            })
                    .setPosition(310, 910)
                    .setName(getName(PRELOAD_RESOURCES_KEY))
                    .setPrompt(getName("PreloadResourcesPrompt"))
                    .setClicked(Boolean.parseBoolean(getFromConfig(PRELOAD_RESOURCES_KEY)));
            addToggleButton(Styles.DEFAULT_TOGGLE_BUTTON, () -> {
                boolean newState = !Boolean.parseBoolean(getFromConfig(VERTICAL_SYNC_KEY));
                updateConfig(VERTICAL_SYNC_KEY, Boolean.toString(newState));
            })
                    .setPosition(310, 840)
                    .setName(getName(VERTICAL_SYNC_KEY))
                    .setPrompt(getName("VerticalSyncPrompt"))
                    .setClicked(Boolean.parseBoolean(getFromConfig(VERTICAL_SYNC_KEY)));
        }});
        otherSettings = mainPanel.add(new Dialog() {{
            setVisible(false);
            addToggleButton(Styles.DEFAULT_TOGGLE_BUTTON, () -> {
            })
                    .setPosition(310, 980)
                    .setName(getName(SEND_ANONYMOUS_STATISTIC_KEY))
                    .setPrompt(getName("SendAnonymousStatisticsPrompt"))
                    .setClicked(Boolean.parseBoolean(getFromConfig(SEND_ANONYMOUS_STATISTIC_KEY)));
        }});
        basicSettings = mainPanel.add(new Dialog() {{
            setVisible(false);
            var dropDownMenu = add(new Dialog() {{
                setVisible(false);
                var dropDown = this;
                String[] langs = Json.getAllLanguagesArray();
                int ox = 780;
                int oy = 950;
                int w = 240;
                int h = 65;
                for (int i = 0; i < langs.length; i++) {
                    String lang = langs[i];
                    addButton(Styles.TEXT_BUTTON, () -> {
                        newLang = lang;
                        dropDown.toggleVisibility();
                    })
                            .set(ox, oy - (h * (i + 1)) + (i * 6) + 6, w, h)
                            .setColor(Styles.DEFAULT_ORANGE)
                            .setName(lang);
                }
            }});
            addButton(Styles.TEXT_BUTTON, dropDownMenu::toggleVisibility)
                    .set(780, 950, 240, 65)
                    .setName(Json.getName("Language"));
            addToggleButton(Styles.DEFAULT_TOGGLE_BUTTON, () -> {
            })
                    .setPosition(310, 980)
                    .setName(getName(SHOW_PROMPTS_KEY))
                    .setPrompt(getName("ShowPromptsPrompt"))
                    .setClicked(Boolean.parseBoolean(getFromConfig(SHOW_PROMPTS_KEY)));
            addToggleButton(Styles.DEFAULT_TOGGLE_BUTTON, () -> {
            })
                    .setPosition(310, 910)
                    .setName(getName(DETECT_LANGUAGE_KEY))
                    .setPrompt(getName("DetectLanguagePrompt"))
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
        if (Global.gameState != GameState.PLAYING) {
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
            add(new Button(this, Styles.TEXT_BUTTON) {
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
        public void updateThis() {
            if (otterClicks >= 5) {
                otterClicks = 0;
                otterImage.setVisible(true);
            }

            if (otterImage.visible()) {
                runOtter();
            }
        }

        final Vector2f speed = new Vector2f(5f, 5f);

        private void runOtter() {
            float x = otterImage.x();
            float y = otterImage.y();

            if (!out && x > 1770 && y < -90) {
                x -= speed.x * Time.delta;
                y += speed.y * Time.delta;
            } else if (x <= 1770 && y >= -90) {
                scheduler.post(() -> out = true, Time.ONE_SECOND);
                out = true;
            }
            if (out) {
                x += speed.x * Time.delta;
                y -= speed.y * Time.delta;
            }

            otterImage.setPosition(x, y);

            if (x >= 2160 && y <= -480) {
                otterImage.setVisible(false);
                out = false;
            }
        }
    }
}
