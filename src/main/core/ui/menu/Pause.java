package core.ui.menu;

import core.*;
import core.ui.Align;
import core.ui.Styles;
import core.ui.widget.Dialog;

import static core.Global.*;
import static core.ui.widget.Widgets.buttonLocalized;
import static core.ui.widget.Widgets.panel;

public class Pause extends Dialog {

    public Pause() {
        setTouchable(true);
        setFillParent(true);

        var background = panel("Background", Styles.SIMPLE_PANEL);
        background.setTouchable(false);

        cell(background, cell -> {
            cell.grow();
            cell.widget.align(Align.CENTER);

            cell.widget.defaultCell()
                    .fixed(240, 65)
                    .padTop(20)
                    .align(Align.CENTER);

            cell.widget.cell(buttonLocalized("Exit", Styles.TEXT_BUTTON, app::quit), b -> {
                b.widget.color(Styles.DIRTY_WHITE);
            });

            cell.widget.row();
            cell.widget.cell(buttonLocalized("Menu", Styles.TEXT_BUTTON, this::menuBtm), b -> {
                b.widget.color(Styles.DIRTY_WHITE);
            });
            cell.widget.row();
            cell.widget.cell(buttonLocalized("Settings", Styles.TEXT_BUTTON, this::settingsBtn), b -> {
                b.widget.color(Styles.DIRTY_WHITE);
            });
            cell.widget.row();
            cell.widget.cell(buttonLocalized("Save world", Styles.TEXT_BUTTON, AutoSaveController::autosave), b -> {
                b.widget.color(Styles.DEFAULT_ORANGE);
            });
            cell.widget.row();
            cell.widget.cell(buttonLocalized("Continue", Styles.TEXT_BUTTON, this::continueBtn));
        });
    }

    private void menuBtm() {
        hide();
        setGameScene(new MenuScene());
        gameState = GameState.MENU;
        UIMenus.mainMenu().show();
    }

    private void continueBtn() {
        hide();
        ((PlayGameScene) gameScene).setPaused(false);
    }

    private void settingsBtn() {
        UIMenus.settings().show(this);
        if (gameState == GameState.PLAYING) {
            hide();
        } else {
            UIMenus.mainMenu().hide();
        }
    }
}
