package core.UI.menu;

import core.Application;
import core.GameState;
import core.Global;
import core.UI.Dialog;
import core.UI.Styles;
import core.UIMenus;

import static core.Constants.discordLink;
import static core.Global.*;

public class MainMenu extends Dialog {
    public MainMenu() {
        setMaximized(true);

        var panel = addPanel(Styles.SIMPLE_PANEL, 0, 965, input.getWidth(), 115);
        panel.addImageButton(this::discordBtn)
                .setPosition(1830, 990)
                .setImage(atlas.get("UI/discordIcon"));

        panel.addButton(Styles.TEXT_BUTTON, this::playButton)
                .set(40, 990, 240, 65)
                .setName(lang.get("NewGame"));
        panel.addButton(Styles.TEXT_BUTTON, this::loadSave)
                .set(300, 990, 240, 65)
                .setName(lang.get("LoadSave"));

        panel.addButton(Styles.TEXT_BUTTON, this::settingsBtn)
                .set(720, 990, 240, 65)
                .setName(lang.get("Settings"))
                .setColor(Styles.DIRTY_WHITE);
        panel.addButton(Styles.TEXT_BUTTON, this::exitBtn)
                .set(980, 990, 240, 65)
                .setName(lang.get("Exit"))
                .setColor(Styles.DIRTY_WHITE);
    }

    private void loadSave() {
        hide();
        UIMenus.loadSave().show();
    }

    private void discordBtn() {
        Application.open(discordLink);
    }

    private void exitBtn() {
        Global.app.quit();
    }

    private void settingsBtn() {
        UIMenus.settings().show();
        if (gameState == GameState.PLAYING) {
            UIMenus.pause().hide();
        } else {
            hide();
        }
    }

    private void playButton() {
        hide();
        UIMenus.createPlanet().show();
    }
}
