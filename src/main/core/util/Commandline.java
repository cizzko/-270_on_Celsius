package core.util;

import core.g2d.Fill;
import core.ui.Styles;
import core.ui.widget.TextField;
import core.ui.widget.Widgets;
import core.ui.widget.Console;
import core.ui.Table;

import static core.Global.*;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F5;

public class Commandline {

    static class DebugConsole extends Table {

        final Console outputConsole;
        final TextField textField;

        public DebugConsole() {
            super("Console");

            setTouchable(false);

            outputConsole = Widgets.console("Console");

            cell(textField = new TextField("Console", Styles.DEFAULT_TEXT_FIELD), c -> {
                c.widget.enterCallback = snippet -> JavaInterpreter.execute(snippet, outputConsole);
                c.fixedX(650);
                c.expandX();

                c.widget.style.background = Fill.cachedRect;
                c.widget.color.set(Styles.DIRTY_BLACK);
            });

            row();

            cell(outputConsole, c -> {
                c.padBottom(20);
                c.fixedX(650);
                c.fixedY(650);
                c.growY();
                c.expand();
            });
        }

        @Override
        protected void onSizeComplete() {
            x = parent.x;
            y = parent.y + parent.height - actualHeight;
        }

        void toggle() {
            uiScene.toggle(this);
            if (uiScene.contains(this)) {
                uiScene.setScrollFocus(textField);
                uiScene.setKeyboardFocus(textField);
            }
        }
    }

    static final DebugConsole console = new DebugConsole();

    public static void inputUpdate() {
        if (Debug.debugLevel < 3) {
            return;
        }

        if (input.justPressed(GLFW_KEY_F5)) {
            console.toggle();
        }
    }
}
