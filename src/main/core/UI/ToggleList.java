package core.UI;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class ToggleList
        extends BaseGroup<ToggleList> {

    protected static final int FLAG_ONLY_ONE = GROUP_LAST_FLAG << 1;

    public ToggleList() {
        this(null);
    }

    class ToggleListener<E extends Element> extends ClickListener {

        final E element;
        final BooleanConsumer listener;
        boolean clicked;

        ToggleListener(E element, BooleanConsumer listener) {
            super(GLFW.GLFW_MOUSE_BUTTON_1, ClickType.PRESS);
            this.element = element;
            this.listener = listener;
        }

        @Override
        protected void onPress(float x, float y) {
            if (isFlag(FLAG_ONLY_ONE)) {
                for (var child : children) {
                    for (var listener : child.listeners()) {
                        if (listener instanceof ToggleListener<?> t && t.clicked && t.element != element) {
                            t.disable();
                        }
                    }
                }
            }

            clicked = true;
            listener.accept(true);
        }

        public void disable() {
            clicked = false;
            listener.accept(false);
        }
    }

    public ToggleList(Group parent) {
        super(parent);
    }

    public ToggleList setOnlyOne(boolean state) {
        setFlag(FLAG_ONLY_ONE, state);
        return as();
    }

    public <E extends Element> E add(E element,
                                     BooleanConsumer click) {
        element.addListener(new ToggleListener<>(element, click));
        add(element);
        return element;
    }

    public ToggleButton add(String id, boolean defaultValue, Consumer<ToggleButton> callback) {
        var button = new ToggleButton(null, Styles.DEFAULT_TOGGLE_BUTTON);
        button.setId(id);
        button.setClicked(defaultValue);

        button.onClick(b -> {
            if (isFlag(FLAG_ONLY_ONE)) {
                b.setClickable(false);
                for (Element other : children) {
                    if (other != button && other instanceof ToggleButton toggle) {
                        toggle.setClicked(false);
                        toggle.setClickable(true);
                    }
                }
            }

            callback.accept(b);
        });

        return add(button);
    }

    public ToggleButton add(String id, Consumer<ToggleButton> callback) {
        return add(id, false, callback);
    }
}
