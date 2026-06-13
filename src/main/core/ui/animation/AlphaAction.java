package core.ui.animation;

import core.graphic.Color;
import core.graphic.Colorf;
import core.math.MathUtil;

///управление прозрачностью
public class AlphaAction<A extends AlphaAction.Colored> extends TemporalAction<A> {
    private float prev;
    private float start, end;
    private Colorf color;

    @Override
    protected void begin() {
        if (color == null) {
            color = actor.color().copyf();
        }
        prev = color.a();
        start = prev;
    }

    @Override
    protected void update(float percent) {
        color.a(MathUtil.lerp(start, end, percent));
    }

    @Override
    public void reset() {
        super.reset();
        color.a(prev);
        color = null;
    }

    public void setAlpha(float alpha) {
        this.end = alpha;
    }

    @Override
    public String toString() {
        return "AlphaAction{start=" + start + ", end=" + end + ", duration=" + duration() + ", time=" + time() + "}";
    }

    public interface Colored {
        Color color();
    }
}
