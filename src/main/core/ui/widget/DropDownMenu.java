package core.ui.widget;

import core.Global;
import core.ui.Style;
import core.ui.Element;
import org.jetbrains.annotations.Nullable;

public final class DropDownMenu extends Panel {
    private Element link;

    public DropDownMenu(@Nullable String id, Style.Panel style) {
        super(id, style);
    }

    public DropDownMenu link(Element link) {
        this.link = link;
        return this;
    }

    @Override
    protected void updateThis(float dt) {
        if (link != null && !inScene(link)) {
            remove();
        }
    }

    private boolean inScene(Element link) {
        return link.visible() &&
               inSceneRecursive(link);
    }

    private static boolean inSceneRecursive(Element link) {
        var parent = link.parent();
        while (parent != null) {
            if (parent == Global.uiScene.root()) {
                return true;
            }

            parent = parent.parent();
        }
        return false;
    }

    protected void onSizeComplete() {
        if (link != null) {
            x = link.x();
            y = link.y() - actualHeight;
        }
    }
}
