package core;

import core.UI.hud.HeadUpDisplay;
import core.UI.menu.*;

public final class UIMenus {
    private static MainMenu mainMenu;
    private static CreatePlanet createPlanet;
    private static LoadSave loadSave;
    private static Pause pause;
    private static Settings settings;

    private static HeadUpDisplay headUpDisplay;

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

    public static LoadSave loadSave() {
        if (loadSave == null) {
            loadSave = new LoadSave();
        }
        return loadSave;
    }

    public static HeadUpDisplay headUpDisplay() {
        if (headUpDisplay == null) {
            headUpDisplay = new HeadUpDisplay();
        }
        return headUpDisplay;
    }
}
