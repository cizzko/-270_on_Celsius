package core.ui;

import core.Global;

import java.util.ArrayList;
import java.util.Objects;

public class TextArea extends BaseElement<TextArea> {
    private final GlyphCache cache = new GlyphCache();

    public String text;
    public Style.Text style;

    public TextArea(Group parent, Style.Text style) {
        super(parent);
        this.style = style;
    }

    public TextArea setText(String newText) {
        if (Objects.equals(this.text, newText)) {
            return this;
        }
        this.text = newText;
        this.cache.setText(style.font, newText, 0, newText.length(), style.color, x, y);
        return this;
    }

    @Override
    protected void resize() {
        if ((flags & (FLAG_X_CHANGED | FLAG_Y_CHANGED)) != 0) {
            this.cache.recomputePosition(x, y);
        }
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
            Global.batch.draw(pos.glyph, pos.colorRgba, pos.x, pos.y);
        }
    }
}
