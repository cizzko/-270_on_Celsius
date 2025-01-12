package core.ui.GUI.Menu;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Logger;
import core.UI;
import core.Utils.SimpleColor;
import core.ui.Dialog;
import core.ui.Styles;

import java.awt.*;
import java.net.URI;

import static core.EventHandling.Logging.Json.getName;
import static core.EventHandling.Logging.Logger.printException;
import static core.Global.atlas;
import static core.Window.start;

public class MainMenu extends Dialog {
    public MainMenu() {
        addPanel(Styles.SIMPLE_PANEL, 0, 965, EventHandler.width, 115);
        addImageButton(this::discordBtn)
                .setPosition(1830, 990)
                .setImage(atlas.byPath("UI/discordIcon.png"));
        addButton(Styles.TEXT_BUTTON, this::exitBtn)
                .set(822, 990, 240, 65)
                .setName(getName("Exit"))
                .setColor(SimpleColor.DIRTY_WHITE);
        addButton(Styles.TEXT_BUTTON, this::settingsBtn)
                .set(548, 990, 240, 65)
                .setName(getName("Settings"))
                .setColor(SimpleColor.DIRTY_WHITE);
        addButton(Styles.TEXT_BUTTON, this::playButton)
                .set(46, 990, 240, 65)
                .setName(getName("Play"));
    }

    private void discordBtn() {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(new URI("https://discord.gg/gUS9X6exAQ"));
        } catch (Exception e) {
            printException("Error when open discord server", e);
        }
    }

    private void exitBtn() {
        Logger.logExit(0);
    }

    private void settingsBtn() {
        UI.settings().show();
        if (!start) {
            hide();
        } else {
            UI.pause().hide();
        }
    }

    private void playButton() {
        hide();
        UI.createPlanet().show();
    }
}
