package core.UI;

import java.util.ArrayList;

public class ParallelAction extends Action {
    protected final ArrayList<Action> actions = new ArrayList<>(4);

    private boolean complete;

    @Override
    public boolean act(float delta) {
        if (complete) return true;
        complete = true;

        ArrayList<Action> actions = this.actions;
        for (int i = 0, n = actions.size(); i < n && actor != null; i++) {
            Action currentAction = actions.get(i);
            if (currentAction.getActor() != null && !currentAction.act(delta)) {
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
        for (Action action : this.actions) {
            action.restart();
        }
    }

    @Override
    public void reset() {
        super.reset();
        actions.clear();
    }

    public void addAction(Action action) {
        actions.add(action);
        if (actor != null) action.setActor(actor);
    }

    @Override
    public void setActor(Element actor) {
        for (Action action : this.actions) {
            action.setActor(actor);
        }
        super.setActor(actor);
    }

    public ArrayList<Action> getActions() {
        return actions;
    }
}
