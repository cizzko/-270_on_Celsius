package core.UI.animation;

import core.pool.Poolable;

public abstract class Action<A> implements Poolable {
    protected A actor;

    public abstract boolean act(float delta);

    public void restart() {}

    public A actor() {
        return actor;
    }

    public void setActor(A actor) {
        this.actor = actor;
    }

    @Override
    public void reset() {
        actor = null;
        restart();
    }
}
