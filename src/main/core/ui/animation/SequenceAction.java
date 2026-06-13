package core.ui.animation;

//действие с кадром
public class SequenceAction<A> extends ParallelAction<A> {
    private int index;

    @Override
    public boolean act(float delta) {
        if (index >= actions.size()) {
            return true;
        }

        if (actions.get(index).act(delta)) {
            if (actor == null) {
                return true;
            }
            index++;
            return index >= actions.size();
        }
        return false;
    }

    @Override
    public void restart() {
        super.restart();
        index = 0;
    }
}
