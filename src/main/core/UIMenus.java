package core;

import core.UI.menu.CreatePlanet;
import core.UI.menu.MainMenu;
import core.UI.menu.Pause;
import core.UI.menu.Settings;

public final class UIMenus {
    private static MainMenu mainMenu;
    private static CreatePlanet createPlanet;
    private static Pause pause;
    private static Settings settings;

    public static MainMenu mainMenu() {
        if (mainMenu == null) {
            mainMenu = new MainMenu();
        }
        return mainMenu;
    }

    public static CreatePlanet createPlanet() {
        if (createPlanet == null) {
            createPlanet = new CreatePlanet();
        }
        return createPlanet;
    }

    public static Pause pause() {
        if (pause == null) {
            pause = new Pause();
        }
        return pause;
    }

    public static Settings settings() {
        if (settings == null) {
            settings = new Settings();
        }
        return settings;
    }
}
