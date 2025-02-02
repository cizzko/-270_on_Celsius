package core.ui.menu;

import core.Application;
import core.GameState;
import core.Global;
import core.UI;
import core.ui.Dialog;
import core.ui.Styles;

import java.awt.*;
import java.net.URI;

import static core.Global.*;

public class MainMenu extends Dialog {
    public MainMenu() {
        addPanel(Styles.SIMPLE_PANEL, 0, 965, input.getWidth(), 115);
        addImageButton(this::discordBtn)
                .setPosition(1830, 990)
                .setImage(atlas.byPath("UI/discordIcon.png"));
        addButton(Styles.TEXT_BUTTON, this::exitBtn)
                .set(822, 990, 240, 65)
                .setName(lang.get("Exit"))
                .setColor(Styles.DIRTY_WHITE);
        addButton(Styles.TEXT_BUTTON, this::settingsBtn)
                .set(548, 990, 240, 65)
                .setName(lang.get("Settings"))
                .setColor(Styles.DIRTY_WHITE);
        addButton(Styles.TEXT_BUTTON, this::playButton)
                .set(46, 990, 240, 65)
                .setName(lang.get("Play"));
    }

    private void discordBtn() {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(new URI("https://discord.gg/gUS9X6exAQ"));
        } catch (Exception e) {
            Application.log.error("Error when open discord server", e);
        }
    }

    private void exitBtn() {
        Global.app.quit();
    }

    private void settingsBtn() {
        UI.settings().show();
        if (gameState == GameState.PLAYING) {
            UI.pause().hide();
        } else {
            hide();
        }
    }

    private void playButton() {
        hide();
        UI.createPlanet().show();
    }
}
