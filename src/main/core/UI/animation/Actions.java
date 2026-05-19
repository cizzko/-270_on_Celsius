package core.UI.animation;

import core.Global;
import core.UI.Element;
import core.math.Interpolation;

public final class Actions {
    private Actions() {}

    static <A extends AlphaAction.Colored> AlphaAction<A> fadeIn(float duration) {
        return alpha(1, duration, Interpolation.identity);
    }

    static <A extends AlphaAction.Colored> AlphaAction<A> fadeOut(float duration) {
        return alpha(0, duration, Interpolation.identity);
    }

    static <A extends AlphaAction.Colored> AlphaAction<A> alpha(float a) {
        return alpha(a, 0, Interpolation.identity);
    }

    static <A extends AlphaAction.Colored> AlphaAction<A> alpha(float a, float duration, Interpolation interpolation) {
        var action = new AlphaAction<A>();
        action.setAlpha(a);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    static <A> SequenceAction<A> sequence(Action<A> action1, Action<A> action2) {
        var action = new SequenceAction<A>();
        action.addAction(action1);
        action.addAction(action2);
        return action;
    }

    static Action<Element> remove(){
        class RemoveElementAction extends Action<Element> {
            private boolean removed;

            @Override
            public boolean act(float delta) {
                if (!removed) {
                    removed = true;
                    Global.uiScene.remove(actor);
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
