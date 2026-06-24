package core.ui.widget;

import core.g2d.Font;
import core.g2d.StackfulRender;
import core.lang.LangTranslation;
import core.ui.GlyphCache;
import core.ui.LayoutElement;
import core.ui.Style;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static core.Global.lang;

public class Label extends LayoutElement<Label> {
    protected static final int FLAG_TRANSLATION = ELEMENT_LAST_FLAG << 1;

    private final GlyphCache cache = new GlyphCache();

    public final Font font;

    private @Nullable String translation, cachedText;

    public Label(@Nullable String id, Style.Text style) {
        super(id);
        this.color.set(style.color);
        this.font = style.font;
    }

    public Label translation(@LangTranslation.Translation String text) {
        setFlag(FLAG_TRANSLATION, true);
        String newText = lang.get(text);
        translation = text;
        cachedText = newText;
        text0(newText, 0, newText.length());
        return this;
    }

    public Label text(String text) { return text(text, 0, text.length()); }

    public Label text(String newText, int offset, int length) {
        setFlag(FLAG_TRANSLATION, false);
        translation = null;
        cachedText = null;
        text0(newText, offset, length);
        return this;
    }

    private void text0(String newText, int offset, int length) {
        cache.setText(font, newText, offset, length, color);
        fixedSize(cache.width(), cache.height());
    }

    public Label reset() {
        setFlag(FLAG_TRANSLATION, false);
        translation = cachedText = null;
        cache.reset();
        return this;
    }

    @Override
    protected void updateThis(float dt) {
        if (isFlag(FLAG_TRANSLATION) && translation != null) {
            var text = lang.get(translation);
            if (!Objects.equals(text, cachedText)) {
                cachedText = text;
            }
            text0(text, 0, text.length());
        }
    }

    @Override
    public void draw() {
        int count = cache.count();
        if (count == 0) {
            return;
        }
        var glyphs = cache.glyphs();
        float textY = 0;/*(height - cache.height()) / 2*/;

        for (int i = 0; i < count; i++) {
            var gl = glyphs.get(i);
            StackfulRender.draw(gl.glyph, gl.rgba8888, x + gl.offsetX, y + textY + gl.offsetY);
        }
    }
}
