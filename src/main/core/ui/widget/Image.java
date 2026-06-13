package core.ui.widget;

import core.g2d.Drawable;
import core.g2d.StackfulRender;
import core.ui.LayoutElement;
import org.jetbrains.annotations.Nullable;

public final class Image extends LayoutElement<Image> {
    private Drawable drawable;

    public Image(@Nullable String id, Drawable texture) {
        super(id);
        drawable(texture);
    }

    public Image drawable(Drawable texture) {
        this.drawable = texture;
        fixedSize(texture.width(), texture.height());
        return this;
    }

    @Override
    public void draw() {
        StackfulRender.draw(drawable, color.rgba8888(), x, y, width, height);
    }
}
