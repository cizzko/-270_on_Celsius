package core.UI;

import core.pool.Poolable;

public abstract class Action implements Poolable {
    protected Element actor;

    public abstract boolean act(float delta);

    public void restart() {}

    public Element getActor() {
        return actor;
    }

    public void setActor(Element actor) {
        this.actor = actor;
    }

    @Override
    public void reset() {
        actor = null;
        restart();
    }
}
