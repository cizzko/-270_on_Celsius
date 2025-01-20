package core;

import core.ui.menu.CreatePlanet;
import core.ui.menu.Pause;
import core.ui.menu.MainMenu;
import core.ui.menu.Settings;

public class UI {
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
