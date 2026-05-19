package core.UI.animation;

import core.math.MathUtil;
import core.util.Color;

///управление прозрачностью
public class AlphaAction<A extends AlphaAction.Colored> extends TemporalAction<A> {
    private float start, end;
    private Color color;

    @Override
    protected void begin() {
        if (color == null) {
            color = actor.color();
        }
        start = color.af();
    }

    @Override
    protected void update(float percent) {
        color.af(MathUtil.lerp(start, end, percent));
    }

    @Override
    public void reset() {
        super.reset();
        color = null;
    }

    public void setAlpha(float alpha) {
        this.end = alpha;
    }

    public interface Colored {
        Color color();
    }
}
