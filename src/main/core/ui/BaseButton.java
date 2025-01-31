package core.ui;

import core.EventHandling.EventHandler;
import core.Utils.Color;
import core.Window;
import core.World.Textures.TextureDrawing;
import core.g2d.Font;

import java.util.function.Consumer;

import static core.EventHandling.Logging.Config.getFromConfig;
import static core.Global.input;

public abstract class BaseButton<B extends BaseButton<B>> extends BaseElement<B> {
    public boolean isClickable = true, isClicked, oneShot; // TODO перевести на битовые флаги.
    public Color color;
    public String name, prompt;
    public Consumer<? super B> clickAction;

    protected BaseButton(Group panel) {
        super(panel);
    }

    public B setClicked(boolean clicked) {
        this.isClicked = clicked;
        return as();
    }

    public B setPrompt(String prompt) {
        this.prompt = prompt;
        return as();
    }

    public B setOneShot(boolean oneShot) {
        this.oneShot = oneShot;
        return as();
    }

    public B setName(String name) {
        this.name = name;
        return as();
    }

    public B onClick(Consumer<? super B> clickAction) {
        this.clickAction = clickAction;
        return as();
    }

    public B onClick(Runnable clickAction) {
        this.clickAction = b -> clickAction.run();
        return as();
    }

    public B setColor(Color color) {
        this.color = color;
        return as();
    }

    public B setClickable(boolean isClickable) {
        this.isClickable = isClickable;
        return as();
    }

    public B toggleClickable() {
        this.isClickable = !isClickable;
        return as();
    }

    protected void drawPrompt(BaseButton<?> button, Font font) {
        if (getFromConfig("ShowPrompts").equals("true")) {
            if (EventHandler.isMousePressed(button) && System.currentTimeMillis() - input.getLastMouseMoveTimestamp() >= 1000 && button.prompt != null) {
                TextureDrawing.drawRectangleText(input.mousePos().x, input.mousePos().y, 0, button.prompt,
                        false, Styles.DEFAULT_PANEL_COLOR, font);
            }
        }
    }
}
