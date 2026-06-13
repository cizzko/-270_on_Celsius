package core.ui.animation;

import java.util.ArrayList;

//одновременно
public class ParallelAction<A> extends Action<A> {
    // TODO: Может это COW-список?
    protected final ArrayList<Action<A>> actions = new ArrayList<>(4);

    private boolean complete;

    @Override
    public boolean act(float delta) {
        if (complete) {
            return true;
        }
        complete = true;

        var actions = this.actions;
        for (int i = 0, n = actions.size(); i < n && actor != null; i++) {
            var currentAction = actions.get(i);
            if (currentAction.actor() != null && !currentAction.act(delta)) {
                complete = false;
            }
            if (actor == null) {
                return true;
            }
        }
        return complete;
    }

    @Override
    public void restart() {
        complete = false;
        for (var action : this.actions) {
            action.restart();
        }
    }

    @Override
    public void reset() {
        super.reset();
        actions.clear();
    }

    public void addAction(Action<A> action) {
        actions.add(action);
        if (actor != null) {
            action.setActor(actor);
        }
    }

    @Override
    public void setActor(A actor) {
        for (var action : this.actions) {
            action.setActor(actor);
        }
        super.setActor(actor);
    }

    public ArrayList<Action<A>> actions() {
        return actions;
    }
}
