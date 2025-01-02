package core.ui.GUI.Menu;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Logger;
import core.UI;
import core.Utils.SimpleColor;
import core.ui.Dialog;

import java.awt.*;
import java.net.URI;

import static core.EventHandling.Logging.Json.getName;
import static core.EventHandling.Logging.Logger.printException;
import static core.Global.atlas;
import static core.Window.start;

public class MainMenu extends Dialog {
    public MainMenu() {
        addPanel()
                .set(0, 965, EventHandler.width, 115)
                .setSimple(true);
        addImageButton(this::discordBtn)
                .setPosition(1830, 990)
                .setImage(atlas.byPath("UI/discordIcon.png"));
        addButton(this::exitBtn)
                .set(822, 990, 240, 65)
                .setName(getName("Exit"))
                .setSimple(false)
                .setColor(SimpleColor.DIRTY_WHITE);
        addButton(this::settingsBtn)
                .set(548, 990, 240, 65)
                .setName(getName("Settings"))
                .setSimple(false)
                .setColor(SimpleColor.DIRTY_WHITE);
        addButton(this::playButton)
                .set(46, 990, 240, 65)
                .setName(getName("Play"))
                .setSimple(false)
                .setColor(SimpleColor.DEFAULT_ORANGE);
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
