package core.UI.GUI.Menu;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Logger;
import core.Global;
import core.Utils.SimpleColor;
import java.awt.Desktop;
import java.net.URI;
import static core.EventHandling.Logging.Json.getName;
import static core.EventHandling.Logging.Logger.printException;
import static core.UI.GUI.CreateElement.*;
import static core.Window.*;

public class Main {
    public static void create() {
        // default coordinate system - full hd
        createPanel(0, 965, EventHandler.width, 115, "defPan", true, "MainMenu");

        createPictureButton(1830, 990, Global.atlas.byPath("UI/discordIcon.png"), "DiscordButton", "MainMenu", Main::discordBtn);
        createButton(822, 990, 240, 65, getName("Exit"), null, false, SimpleColor.DIRTY_WHITE, "MainMenu", Main::exitBtn);
        createButton(548, 990, 240, 65, getName("Settings"), null, false, SimpleColor.DIRTY_WHITE, "MainMenu", Main::settingsBtn);
        createButton(46, 990, 240, 65, getName("Play"), null, false, SimpleColor.DEFAULT_ORANGE, "MainMenu", Main::playBtn);
    }

    public static void delete() {
        buttons.values().stream().filter(button -> button.getGroup().equals("MainMenu")).forEach(button -> button.setVisible(false));
        panels.get("defPan").setVisible(false);
    }

    private static void discordBtn() {
        try {
            Desktop desktop = Desktop.getDesktop();
            desktop.browse(new URI("https://discord.gg/gUS9X6exAQ"));
        } catch (Exception e) {
            printException("Error when open discord server", e);
        }
    }

    private static void exitBtn() {
        Logger.logExit(0);
    }

    private static void settingsBtn() {
        Settings.create();
        if (!start) {
            Main.delete();
        } else {
            Pause.delete();
        }
    }

    private static void playBtn() {
        Main.delete();
        CreatePlanet.create();
    }
}
