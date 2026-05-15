package core.UI;

import core.math.Interpolation;

public interface UiEffect {

    static AlphaAction fadeIn(float duration) {
        return alpha(1, duration, null);
    }

    static AlphaAction fadeOut(float duration) {
        return alpha(0, duration, null);
    }

    static AlphaAction alpha(float a){
        return alpha(a, 0, null);
    }

    static AlphaAction alpha(float a, float duration, Interpolation interpolation) {
        AlphaAction action = new AlphaAction();
        action.setAlpha(a);
        action.setDuration(duration);
        action.setInterpolation(interpolation);
        return action;
    }

    static SequenceAction sequence(Action action1, Action action2){
        SequenceAction action = new SequenceAction();
        action.addAction(action1);
        action.addAction(action2);
        return action;
    }

    static RemoveActorAction remove(){
        return new RemoveActorAction();
    }
}
