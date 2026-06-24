package core.ui.menu;

import core.GameSettings;
import core.GameState;
import core.Global;
import core.UIMenus;
import core.ui.Align;
import core.ui.Cell;
import core.ui.Styles;
import core.ui.widget.Button;
import core.ui.widget.Dialog;
import core.ui.widget.DropDownMenu;

import java.util.Locale;
import java.util.function.Consumer;

import static core.Global.gameSettings;
import static core.Global.uiScene;
import static core.ui.widget.Widgets.*;

public class Settings extends Dialog {
    private final GameSettings current;
    private final Button saveButton;
    private final DropDownMenu langList;

    public Settings() {
        setFillParent(true);

        pad(20);

        current = new GameSettings();
        current.set(gameSettings);

        saveButton = buttonLocalized("Save", Styles.TEXT_BUTTON, this::saveBtn);
        saveButton.isClickable = false;
        saveButton.isClicked = true;

        langList = dropDownMenu("DropDownMenu", Styles.DUMMY_PANEL);

        cell(panel("MainPanel"), cell -> {
            cell.grow();

            var settingsBasic = table("SettingsBasic");
            {
                settingsBasic.align(Align.TOP_LEFT);
                settingsBasic.pad(20);

                langList.defaultCell()
                        .fixed(240, 65);

                for (String lang : Global.lang.supportedLanguages().toList()) {
                    Locale loc = Locale.of(lang);
                    String displayName = loc.getDisplayName(loc);
                    int cp = displayName.codePointAt(0);
                    if (Character.isLowerCase(cp)) {
                        var sb = new StringBuilder();
                        sb.appendCodePoint(Character.toUpperCase(cp));
                        sb.append(displayName, Character.charCount(cp), displayName.length());
                        displayName = sb.toString();
                    }

                    langList.cell(button(lang, Styles.TEXT_BUTTON, displayName, b -> {
                        toggleLangList(b);

                        modifySettings(s -> s.language = lang);
                    }), c -> updateDefaultLang(c.widget, lang));
                    langList.row();
                }

                settingsBasic.cell(atlasImage("UI/GUI/languageIcon"));
                var languageButton = buttonLocalized("Language", Styles.TEXT_BUTTON, () -> {
                    uiScene.toggle(langList);
                });
                langList.link(languageButton);

                settingsBasic.cell(languageButton);
            }
            var settingsOther = table("SettingsOther");
            var settingsGraphics = table("SettingsGraphics");
            {
                settingsGraphics.align(Align.TOP_LEFT);
                settingsGraphics.pad(20);
                settingsGraphics.cell(toggleButtonLocalized("Vertical sync", Styles.DEFAULT_TOGGLE_BUTTON, gameSettings.verticalSync,
                        () -> modifySettings(s -> s.verticalSync = !s.verticalSync)),
                        c -> c.widget.isClicked = gameSettings.verticalSync);
            }


            var categories = dockPanel("Categories", Styles.DEFAULT_PANEL);
            // TODO другой стиль
            categories.pad(20, 0, 0, 0);
            categories.align(Align.BOTTOM_LEFT);
            categories.defaultCell()
                    .padBottom(40)
                    .fixed(240, 65)
                    .align(Align.BOTTOM_LEFT);

            categories.add((Button) buttonLocalized("Basic", Styles.SIMPLE_TEXT_BUTTON)
                    .color(Styles.DIRTY_BLACK), settingsBasic);
            categories.row();
            categories.add((Button) buttonLocalized("Other", Styles.SIMPLE_TEXT_BUTTON)
                    .color(Styles.DIRTY_BLACK), settingsOther);
            categories.row();
            categories.add((Button) buttonLocalized("Graphics", Styles.SIMPLE_TEXT_BUTTON)
                    .color(Styles.DIRTY_BLACK), settingsGraphics);

            categories.defaultCell()
                    .align(Align.TOP_LEFT);
            categories.defaultCell()
                    .reset();

            var upperGroup = table("UpperGroup");
            upperGroup.defaultCell()
                    .padBottom(40)
                    .fixed(240, 65)
                    .align(Align.TOP_LEFT);

            upperGroup.cell(saveButton);
            upperGroup.row();
            upperGroup.cell(buttonLocalized("Return", Styles.SIMPLE_TEXT_BUTTON, this::exitBtn));
            upperGroup.row();

            categories.row();
            categories.cell(upperGroup, c -> c
                    .padTop(20)
                    .align(Align.TOP)
                    .expandY());

            categories.showDefault(settingsGraphics);

            cell.widget.cell(categories, c -> {
                c.fixedX(240);
                c.growY();
            });

            var settings = stack("Settings")
                    .add(settingsBasic)
                    .add(settingsOther)
                    .add(settingsGraphics);
            settings.setTouchable(true);
            cell.widget.cell(settings, Cell::grow);
        });
    }

    private void toggleLangList(Button button) {
        uiScene.toggle(langList);
        for (var child : langList.children()) {
            if (child != button && child instanceof Button b) {
                b.clicked(false);
            }
        }
        button.clicked(true);
    }

    private static void updateDefaultLang(Button button, String lang) {
        button.clicked(lang.equals(Global.lang.currentLanguage()));
    }

    private void exitBtn() {
        hide();
        // TODO возможно предупреждать о сбросе изменений
        //  хотя сейчас изменения не сбрасываются
        if (Global.gameState != GameState.PLAYING) {
            UIMenus.mainMenu().show();
        }
    }

    private void saveBtn(Button button) {
        button.isClicked = true;
        button.isClickable = false;

        Global.lang.setLanguage(current.language);

        gameSettings.set(current);
        gameSettings.save();
    }

    void modifySettings(Consumer<GameSettings> modifier) {
        modifier.accept(current);
        saveButton.isClicked = false;
        saveButton.isClickable = true;
    }
}
