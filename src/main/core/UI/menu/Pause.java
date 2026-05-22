package core.UI.menu;

import core.*;
import core.UI.Dialog;
import core.UI.Styles;

import static core.Global.*;

public class Pause extends Dialog {
    public Pause() {

        var background = addPanel(Styles.SIMPLE_PANEL, 0, 0, input.getWidth(), input.getHeight());
        background.addButton(Styles.TEXT_BUTTON, this::continueBtn)
                .set(840, 650, 240, 65)
                .setName(Global.lang.get("Continue"))
                .setColor(Styles.DEFAULT_ORANGE);
        background.addButton(Styles.TEXT_BUTTON, this::saveButton)
                .set(840, 580, 240, 65)
                .setName(Global.lang.get("SaveWorld"))
                .setColor(Styles.DEFAULT_ORANGE);
        background.addButton(Styles.TEXT_BUTTON, this::settingsBtn)
                .set(840, 430, 240, 65)
                .setName(Global.lang.get("Settings"))
                .setColor(Styles.DIRTY_WHITE);
        background.addButton(Styles.TEXT_BUTTON, this::menuBtm)
                .set(840, 330, 240, 65)
                .setName(Global.lang.get("Menu"))
                .setColor(Styles.DIRTY_WHITE);
        background.addButton(Styles.TEXT_BUTTON, this::exitBtn)
                .set(840, 130, 240, 65)
                .setName(Global.lang.get("Exit"))
                .setColor(Styles.DIRTY_WHITE);
    }

    private void menuBtm() {
        hide();
        Global.setGameScene(new MenuScene());
        gameState = GameState.MENU;
        UIMenus.mainMenu().show();
    }

    private void continueBtn() {
        hide();
        UIMenus.settings().hide();
    }

    private void exitBtn() {
        // AutoSaveController.autosave();
        Global.app.quit();
    }

    private void settingsBtn() {
        UIMenus.settings().show();
        if (Global.gameState == GameState.PLAYING) {
            hide();
        } else {
            UIMenus.mainMenu().hide();
        }
    }

    private void saveButton() {
        AutoSaveController.autosave();
    }
}
