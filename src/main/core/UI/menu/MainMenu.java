package core.UI.menu;

import core.Application;
import core.GameState;
import core.Global;
import core.UI.Dialog;
import core.UI.Styles;
import core.UIMenus;

import java.awt.*;
import java.net.URI;

import static core.Global.*;

public class MainMenu extends Dialog {
    public MainMenu() {
        setMaximized(true);

        var panel = addPanel(Styles.SIMPLE_PANEL, 0, 965, input.getWidth(), 115);
        panel.addImageButton(this::discordBtn)
                .setPosition(1830, 990)
                .setImage(atlas.get("UI/discordIcon"));
        panel.addButton(Styles.TEXT_BUTTON, this::exitBtn)
                .set(822, 990, 240, 65)
                .setName(lang.get("Exit"))
                .setColor(Styles.DIRTY_WHITE);
        panel.addButton(Styles.TEXT_BUTTON, this::settingsBtn)
                .set(548, 990, 240, 65)
                .setName(lang.get("Settings"))
                .setColor(Styles.DIRTY_WHITE);
        panel.addButton(Styles.TEXT_BUTTON, this::playButton)
                .set(46, 990, 240, 65)
                .setName(lang.get("Play"));
        panel.addButton(Styles.TEXT_BUTTON, this::loadSave)
                .set(46 + 240 + 20, 990, 240, 65)
                .setName(lang.get("LoadSave"));
    }

    private void loadSave() {
        hide();
        UIMenus.loadSave().show();
    }

    private void discordBtn() {
        try { // TODO НЕ ИСПОЛЬЗОВАТЬ АПИ ЖАВЫ
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
