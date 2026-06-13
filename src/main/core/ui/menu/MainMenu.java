package core.ui.menu;

import core.GameState;
import core.ui.Styles;
import core.ui.widget.Dialog;
import core.UIMenus;
import core.ui.Align;

import static core.Global.*;
import static core.ui.Styles.DIRTY_WHITE;
import static core.ui.widget.Widgets.*;

public class MainMenu extends Dialog {

    public MainMenu() {
        super("MainMenu");
        setFillParent(true);
        cell(panel("UpperPanel", Styles.SIMPLE_PANEL), pan -> {
            pan.align(Align.TOP);
            pan.fixedY(115);
            pan.expand();
            pan.growX();

            int padding = 20;
            pan.widget.defaultCell()
                    .align(Align.CENTER)
                    .expand()
                    .padRight(padding)
                    // .minWidth(100)
                    .prefWidth(200)
                    .colspan(2)
            ;

            pan.widget.cell(buttonLocalized("New game", Styles.TEXT_BUTTON, () -> {
                open(UIMenus.createPlanet());
            }), cell -> {
                cell.padLeft(padding);
            });
            pan.widget.cell(buttonLocalized("Load save", Styles.TEXT_BUTTON, () -> {
                hide();
                UIMenus.loadSave().show();
            }));

            pan.widget.cell(buttonLocalized("Settings", Styles.TEXT_BUTTON, () -> {
                UIMenus.settings().show();
                if (gameState == GameState.PLAYING) {
                    UIMenus.pause().hide();
                } else {
                    hide();
                }
            }), cell -> {
                cell.widget.color(DIRTY_WHITE);
                cell.padLeft(100);
            });
            pan.widget.cell(buttonLocalized("Exit", Styles.TEXT_BUTTON, app::quit), cell -> {
                cell.widget.color(DIRTY_WHITE);
            });
        });
    }
}
