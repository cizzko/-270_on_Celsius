package core.UI;

import core.Global;
import core.g2d.StackfulRender;

import java.util.Objects;

public class TextArea extends BaseElement<TextArea> {
    private static final int FLAG_TRANSLATION = ELEMENT_LAST_FLAG << 1;

    private final GlyphCache cache = new GlyphCache();

    public String text;
    public Style.Text style;

    public TextArea(Group parent, Style.Text style) {
        super(parent);
        this.style = style;
        setTouchable(false);
    }

    public TextArea setTranslation(String id) { // TODO доработать обновление в зависимости от языка
        if (id == null) {
            this.text = null;
            this.cache.reset();
            return this;
        }
        setFlag(FLAG_TRANSLATION, true);
        this.text = id;
        resolveTranslation(id);
        return this;
    }

    public TextArea setText(String newText) {
        setFlag(FLAG_TRANSLATION, false);
        if (newText == null) {
            this.text = null;
            this.cache.reset();
            return this;
        }

        if (Objects.equals(this.text, newText)) {
            return this;
        }
        this.text = newText;
        this.cache.setText(style.font, newText, 0, newText.length(), style.color, x, y);
        return this;
    }

    @Override
    protected void resize() {
        if ((flags & FLAG_TRANSLATION) != 0 && Global.lang.languageHasChanged()) {
            resolveTranslation(text);
        } else {
            if ((flags & (FLAG_X_CHANGED | FLAG_Y_CHANGED)) != 0) {
                this.cache.recomputePosition(x, y);
            }
        }
    }

    private void resolveTranslation(String id) {
        String newText = Global.lang.get(id);
        this.cache.setText(style.font, newText, 0, newText.length(), style.color, x, y);
    }

    @Override
    public void draw() {
        if (!visible()) {
            return;
        }
        var glyphs = cache.getGlyphs();
        int count = cache.getCount();
        for (int i = 0; i < count; i++) {
            GlyphCache.GlyphData pos = glyphs.get(i);
            StackfulRender.draw(pos.glyph, pos.rgba8888, pos.x, pos.y);
        }
    }
}
