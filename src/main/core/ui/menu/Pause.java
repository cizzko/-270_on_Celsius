package core.ui.menu;

import core.GameState;
import core.Global;
import core.UI;
import core.ui.Dialog;
import core.ui.Styles;

import static core.Global.input;

public class Pause extends Dialog {
    public Pause() {
        addPanel(Styles.SIMPLE_PANEL, 0, 0, input.getWidth(), input.getHeight());
        addButton(Styles.TEXT_BUTTON, this::continueBtn)
                .set(840, 650, 240, 65)
                .setName(Global.lang.get("Continue"))
                .setColor(Styles.DEFAULT_ORANGE);
        addButton(Styles.TEXT_BUTTON, this::saveButton)
                .set(840, 550, 240, 65)
                .setName(Global.lang.get("SaveWorld"))
                .setColor(Styles.DEFAULT_ORANGE);
        addButton(Styles.TEXT_BUTTON, this::exitBtn)
                .set(840, 300, 240, 65)
                .setName(Global.lang.get("Exit"))
                .setColor(Styles.DIRTY_WHITE);
        addButton(Styles.TEXT_BUTTON, this::settingsBtn)
                .set(840, 400, 240, 65)
                .setName(Global.lang.get("Settings"))
                .setColor(Styles.DIRTY_WHITE);
    }

    private void continueBtn() {
        hide();
        UI.settings().hide();
    }

    private void exitBtn() {
        Global.app.quit();
    }

    private void settingsBtn() {
        UI.settings().show();
        if (Global.gameState == GameState.PLAYING) {
            hide();
        } else {
            UI.mainMenu().hide();
        }
    }

    private void saveButton() {
        // TODO реализовать
    }
}
