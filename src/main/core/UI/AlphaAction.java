package core.UI;

import core.math.MathUtil;
import core.util.Color;

public class AlphaAction extends TemporalAction {
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
}
