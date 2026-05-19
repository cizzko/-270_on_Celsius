package core.UI.animation;

import core.math.Interpolation;

//от дт
abstract public class TemporalAction<A> extends Action<A> {
    private float duration, time;
    private Interpolation interpolation;
    private boolean began, complete;

    public boolean act(float delta) {
        if (complete) return true;
        if (!began) {
            began = true;
            begin();
        }
        time += delta;
        complete = time >= duration;

        float percent;
        if (complete) {
            percent = 1;
        } else {
            percent = time / duration;
            if (interpolation != null) percent = interpolation.apply(percent);
        }
        update(percent);
        return complete;
    }

    protected void begin() {
    }

    protected abstract void update(float percent);

    public void restart() {
        time = 0;
        began = false;
        complete = false;
    }

    public void reset() {
        super.reset();
        interpolation = null;
    }

    public float time() {
        return time;
    }

    public void setTime(float time) {
        this.time = time;
    }

    public float duration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public Interpolation interpolation() {
        return interpolation;
    }

    public void setInterpolation(Interpolation interpolation) {
        this.interpolation = interpolation;
    }
}
