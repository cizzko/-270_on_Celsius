package core.ui.animation;

import core.ui.Element;
import core.math.Interpolation;

public final class Actions {
    private Actions() {}

    public static <A extends AlphaAction.Colored> AlphaAction<A> fadeIn(float duration) {
        return alpha(1, duration, Interpolation.identity);
    }

    public static <A extends AlphaAction.Colored> AlphaAction<A> fadeOut(float duration) {
        return alpha(0, duration, Interpolation.identity);
    }

    public static <A extends AlphaAction.Colored> AlphaAction<A> alpha(float a) {
        return alpha(a, 0, Interpolation.identity);
    }

    public static <A extends AlphaAction.Colored> AlphaAction<A> alpha(float a, float duration, Interpolation interpolation) {
        var action = new AlphaAction<A>();
        action.setAlpha(a);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    public static <A> SequenceAction<A> sequence(Action<A> action1, Action<A> action2) {
        var action = new SequenceAction<A>();
        action.addAction(action1);
        action.addAction(action2);
        return action;
    }

    public static <E extends Element> Action<E> hide(){
        class RemoveElementAction extends Action<E> {
            private boolean removed;

            @Override
            public boolean act(float delta) {
                if (!removed) {
                    removed = true;
                    actor.remove();
                }
                return true;
            }

            @Override
            public void restart() {
                removed = false;
            }
        }
        return new RemoveElementAction();
    }
}
