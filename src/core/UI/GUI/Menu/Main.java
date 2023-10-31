package core.UI.GUI.Menu;

import core.EventHandling.Logging.Logger;
import core.UI.GUI.CreateElement;
import core.Utils.SimpleColor;
import java.awt.*;
import java.net.URI;
import static core.EventHandling.Logging.Json.getName;
import static core.EventHandling.Logging.Logger.printException;
import static core.UI.GUI.CreateElement.*;
import static core.Window.defPath;
import static core.Window.start;

public class Main {
    public static void create() {
        //default coordinate system - full hd
        createPanel(0, 965, 1920, 115, "defPan", true, "MainMenu");

        CreateElement.createPictureButton(1830, 990, defPath + "\\src\\assets\\UI\\discordIcon.png", "DiscordButton", "MainMenu", Main::discordBtn);
        createButton(822, 990, 240, 65, getName("Exit"), null, false, new SimpleColor(236, 236, 236, 55), "MainMenu", Main::exitBtn);
        createButton(548, 990, 240, 65, getName("Settings"), null, false, new SimpleColor(236, 236, 236, 55), "MainMenu", Main::settingsBtn);
        createButton(46, 990, 240, 65, getName("Play"), null, false, new SimpleColor(255, 80, 0, 55), "MainMenu", Main::playBtn);
    }

    public static void delete() {
        buttons.values().stream().filter(button -> button.group.equals("MainMenu")).forEach(button -> button.visible = false);
        panels.get("defPan").visible = false;
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
