package core;

import core.ui.hud.HeadUpDisplay;
import core.ui.menu.*;
import core.ui.LayoutGroup;
import org.jetbrains.annotations.Nullable;

public final class UIMenus {
    private static MainMenu mainMenu;
    private static CreatePlanet createPlanet;
    private static LoadSave loadSave;
    private static Pause pause;
    private static Settings settings;

    public static final class WidgetGroup extends LayoutGroup<WidgetGroup> {
        WidgetGroup(@Nullable String id) { super(id); }
    }

    public static final WidgetGroup hudGroup = new WidgetGroup("Hud")
            .setTouchable(false)
            .setFillParent(true);

    static {
        UIMenus.hudGroup.add(new HeadUpDisplay());
    }

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
}
