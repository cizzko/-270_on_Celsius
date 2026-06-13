package core.ui.widget;

import core.ui.animation.Action;
import core.ui.LayoutElement;
import core.ui.Table;
import core.util.SnapshotArrayList;
import org.jetbrains.annotations.Nullable;

import static core.Global.uiScene;

public class Dialog extends Table {

    // private static final Supplier<Action<?>> DEFAULT_OPEN_ACTION =
    //         () -> sequence();
                    /*sequence(Actions.alpha(0), Actions.alpha(1, Time.ONE_SECOND / 2f, smoothstep))*/;

    public final SnapshotArrayList<Action<?>> actions = new SnapshotArrayList<>();

    private Dialog prev;

    protected Dialog() {
        this(null);
        id(getClass().getSimpleName());
    }

    public Dialog(@Nullable String id) {
        super(id);
        setTouchable(false);
    }

    @Override
    protected void updateThis(float dt) {

        Object[] elements = actions.begin();
        int size = actions.size();
        for (int i = 0; i < size; i++) {
            if (elements[i] instanceof Action<?> a) {
                @SuppressWarnings("unchecked")
                var action = (Action<LayoutElement<?>>) a;

                action.setActor(this);
                if (action.act(dt)) {
                    action.reset();
                    actions.removeAt(i);
                }
            }
        }
        actions.end();
    }

    public boolean toggle() {
        if (uiScene.contains(this)) {
            hide();
            return false;
        } else {
            show();
            return true;
        }
    }

    public void open(Dialog other) {
        hide();
        other.show();
    }

    public void hide() {
        // actions.add(sequence(
        //         fadeOut(Time.ONE_SECOND / 2f),
        //         (Action<MainMenu>) Actions.hide()));

        if (prev != null) {
            prev.show();
        }

        reset();
        remove();
    }

    public void show(Dialog prev) {
        this.prev = prev;
        uiScene.add(this);
        // actions.add(sequence(
        //         Actions.alpha(0),
        //         Actions.alpha(color.af(), Time.ONE_SECOND/2f, smoothstep)));
    }

    public void show() { show(null); }

    public void reset() {

    }
}
