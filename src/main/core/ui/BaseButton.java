package core.ui;

import core.Utils.SimpleColor;

import java.util.function.Consumer;

public abstract class BaseButton<B extends BaseButton<B>> extends BaseElement<B> {
    public boolean isClickable = true, isClicked, oneShot; // TODO перевести на битовые флаги.
    public SimpleColor color;
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

    public B setColor(SimpleColor color) {
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

    @Override
    public String toString() {
        return super.toString() + " {name=" + name + "}";
    }
}
