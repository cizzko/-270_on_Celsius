package core.UI;

import core.Global;

public class RemoveActorAction extends Action {
    private boolean removed;

    @Override
    public boolean act(float delta) {
        if (!removed) {
            removed = true;
            Global.uiScene.remove(actor);
        }
        return true;
    }

    @Override
    public void restart() {
        removed = false;
    }
}
