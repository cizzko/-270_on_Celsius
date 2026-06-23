package core.ui.widget;

import core.Global;
import core.g2d.Drawable;
import core.g2d.Fill;
import core.g2d.Font;
import core.g2d.StackfulRender;
import core.graphic.Color;
import core.ui.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static core.Global.input;
import static org.lwjgl.glfw.GLFW.*;

public final class TextField extends LayoutElement<TextField> {
    private static final int NEW_LINE_WIDTH = 8;

    static final char[] CHARS = new char[2];
    private static final Logger log = LogManager.getLogger(TextField.class);

    public final Style.TextField style;
    private final StringBuilder text = new StringBuilder(32);

    // Позиция каретки. Это индекс в text массиве куда будет вставлен символ, но он может быть равен length, что
    // означает запись в конец строки, т.е. запись на новую позицию
    private int caret;
    private int length;
    private boolean hasSelection;
    // Граница выделения. Этот символ будет в выделенной области
    private int selection;
    private InputValidator validator;
    private Align align = Align.LEFT;
    private Behavior behavior = Behavior.SLIDING_WINDOW;

    public enum Behavior {
        SLIDING_WINDOW
    }

    public Consumer<String> enterCallback;

    public TextField(@Nullable String id, Style.TextField style) {
        super(id);
        this.style = style;
        addListener(new Listener(GLFW_MOUSE_BUTTON_1, null));

        prefHeight = style.font.lineHeight();

        ch = prefHeight;
    }

    static class Line {
        int begin, end;

        int length() { return end - begin; }
    }

    ObjectArrayList<Line> lines = new ObjectArrayList<>();
    void recomputeLines() {
        lines.clear();
        if (length == 0) {
            return;
        }

        Line line = new Line();
        for (int i = 0; i < length; ++i) {
            if (text.charAt(i) == '\n') {
                line.end = i;
                lines.add(line);
                line = new Line();
                line.begin = i + 1;
            }
        }

        line.end = length;
        lines.add(line);
    }

    int caretRow() {
        for (int row = 0; row < lines.size(); row++) {
            Line line = lines.get(row);
            if (line.begin <= caret && caret <= line.end) {
                return row;
            }
        }
        return lines.size() - 1;
    }

    //светло-синий
    private static final int selectionColor = Color.rgba8888(20, 90, 210, 200), caretColor = Styles.DIRTY_BRIGHT_WHITE.rgba8888();

    //размеры каретки
    private final float cw = 3, ch;

    //частота мигания каретки
    private final static float blinkTime = 0.32f;
    private long lastBlink;

    private final GlyphCache glyphCache = new GlyphCache();
    private GlyphSize[] positions = EMPTYF;

    static class GlyphSize {
        float x, y;
        int width, height;
    }

    private float cx, cy;
    private float tx, ty;

    private boolean cursorOn = true;
    private boolean textChanged;

    private int minI, maxI;

    private int maxLength = -1; // Ограничение длины к кодепоинтах.

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public void updateThis(float dt) {
        updateText();
        calculateCaretPosition();
        calculateSelectionArea();
        updateBlink();
    }

    public TextField validator(InputValidator validator) {
        this.validator = validator;
        return this;
    }

    private static final GlyphSize[] EMPTYF = new GlyphSize[0];

    private void updateText() {
        if (!textChanged) {
            return;
        }
        textChanged = false;
        recomputeLines();

        StringBuilder text = this.text;
        if (text.isEmpty()) {
            glyphCache.reset();
            positions = EMPTYF;
            return;
        }

        Font font = style.font;
        int len = length;
        glyphCache.setText(font, text, 0, len, style.textColor);

        float x = this.x + switch (align) {
            case LEFT, BOTTOM_LEFT, TOP_LEFT -> 0;
            case RIGHT, BOTTOM_RIGHT, TOP_RIGHT ->
                    width - glyphCache.width();
            default -> // CENTER, TOP, BOTTOM
                    (width - glyphCache.width()) / 2f;
        };

        float y = this.y + switch (align) {
            case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> 0;
            case TOP, TOP_LEFT, TOP_RIGHT ->
                    height - glyphCache.height();
            default -> // CENTER, LEFT, RIGHT
                    (height - glyphCache.height()) / 2f;
        };

        this.tx = x;
        this.ty = y;

        if (positions.length != len) {
            this.positions = new GlyphSize[len];
        }
        float bx = x;
        float by = y;
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            var gl = font.getGlyph(c);
            if (positions[i] == null) {
                positions[i] = new GlyphSize();
            }
            positions[i].x = bx;
            positions[i].y = by;
            int w = gl.width();
            if (c == '\n') {
                w = NEW_LINE_WIDTH;
            }
            positions[i].width = w;
            positions[i].height = gl.height();

            if (c == '\n') {
                bx = x;
                by -= 30;
            } else {
                bx += w;
            }
        }
    }

    private void calculateSelectionArea() {
        if (length == 0) {
            return;
        }
        if (!hasSelection()) {
            return;
        }

        int minIndex = Math.min(caret, selection);
        int maxIndex = Math.max(caret, selection);

        if (maxIndex - 1 < 0) {
            TextField.log.debug("caret: {} | selection: {} | selected: [{}, {}]",
                    caret, selection, minIndex, maxIndex);
        }

        minI = minIndex;
        maxI = maxIndex - 1;
    }

    private void calculateCaretPosition() {
        int len = length;
        float x = this.x;
        float y = this.y;

        if (caret == len) {
            if (caret - 1 >= 0) { // конец строки
                var prev = positions[caret - 1];
                cx = prev.x + prev.width;
                cy = prev.y;
            } else { // строка пуста
                cx = x;
                cy = y;
            }
        } else {
            var current = positions[caret];
            cx = current.x;
            cy = current.y;
        }
    }

    private void updateBlink() {
        if (Global.uiScene.keyboardFocus() == this) {
            long time = System.nanoTime();
            if ((time - lastBlink) / 1000000000.0f > blinkTime) {
                cursorOn = !cursorOn;
                lastBlink = time;
            }
        } else {
            cursorOn = false;
        }
    }

    @Override
    public void draw() {
        Drawable background = style.background;
        if (background != null) {
            StackfulRender.draw(background, color, x, y, width, height);
        } else {
            float w = Math.min(width, glyphCache.width());
            Fill.rect(x, y, w, glyphCache.height(), color);
        }

        if (hasSelection()) {
            float lineHeight = style.font.lineHeight();

            var min = positions[minI];
            float lineX = min.x;
            float lineY = min.y;
            float lineWidth = 0;
            for (int i = minI; i <= maxI; i++) {
                lineWidth += positions[i].width;
                if (text.charAt(i) == '\n') {
                    Fill.rect(lineX, lineY, lineWidth, lineHeight, selectionColor);

                    if (i + 1 <= maxI) {
                        var next = positions[i + 1];

                        lineX = next.x;
                        lineY = next.y;
                        lineWidth = next.width;

                        i ++;
                    }
                }
            }
            if (text.charAt(maxI) != '\n') {
                Fill.rect(lineX, lineY, lineWidth, lineHeight, selectionColor);
            }
        }

        var glyphs = glyphCache.glyphs();
        int count = glyphCache.count();
        for (int i = 0; i < count; i++) {
            var pos = glyphs.get(i);
            StackfulRender.draw(pos.glyph, pos.rgba8888, tx + pos.offsetX, ty + pos.offsetY);
        }

        if (cursorOn) {
            Fill.rect(cx, cy, cw, ch, caretColor);
        }
    }

    public interface InputValidator {
        boolean accept(int codepoint);
    }

    // region Реализация

    final class Listener extends ClickListener {
        public Listener(int button, ClickType clickType) {
            super(button, clickType);
        }

        @Override
        protected void onPress(float x, float y) {
            setCursorPosition(x, y);
            selection = caret;
            hasSelection = true;
        }

        @Override
        public void onTouchUp(float x, float y, int button) {
            if (selection == caret) {
                resetSelection();
            }
            super.onTouchUp(x, y, button);
        }

        @Override
        protected void onRelease(float x, float y) {
            int count = tapCount % 4;
            switch (count) {
                case 0 -> resetSelection();
                case 2 -> setSelection(0, length);
            }
        }

        private void setCursorPosition(float x, float y) {
            lastBlink = 0;
            cursorOn = false;
            caret = Math.max(0, letterUnderCursor(x, y));
        }

        @Override
        public void onKeyDown(int key, int scancode, int mods) {
            switch (key) {
                case GLFW_KEY_HOME -> {
                    if (length == 0) {
                        return;
                    }
                    int row = caretRow();
                    int lineBegin = lines.get(row).begin;

                    if (shift()) {
                        int c = caret;

                        setCaret(lineBegin);
                        if (!hasSelection) {
                            selection = c;
                            hasSelection = true;
                        }
                    } else {
                        setCaret(lineBegin);
                        resetSelection();
                    }
                }
                case GLFW_KEY_END -> {
                    if (length == 0) {
                        return;
                    }
                    int row = caretRow();
                    int lineEnd = lines.get(row).end;

                    if (shift()) {
                        int c = caret;
                        setCaret(lineEnd);
                        if (!hasSelection) {
                            selection = c;
                            hasSelection = true;
                        }
                    } else {
                        setCaret(lineEnd);
                        resetSelection();
                    }
                }
                default -> {
                    onRepeatableKey(key, mods);
                }
            }
        }

        private boolean shift() {
            return input.pressed(GLFW_KEY_LEFT_SHIFT) || input.pressed(GLFW_KEY_RIGHT_SHIFT);
        }

        private boolean ctrl() {
            return input.pressed(GLFW_KEY_LEFT_CONTROL) || input.pressed(GLFW_KEY_RIGHT_CONTROL);
        }

        private void delete(int d) {
            if (hasSelection()) {
                deleteSelection();
            } else {
                if (d > 0) {
                    if (caret < length) {
                        int charc = 1;
                        text.deleteCharAt(caret);
                        length -= charc;
                    }
                } else {
                    if (caret > 0) {
                        int charc = 1;
                        int prev = caret - 1;
                        text.deleteCharAt(prev);
                        length -= charc;
                        caret -= charc;
                    }
                }
            }
            textChanged = true;
            resetSelection();
        }

        private void deleteSelection() {
            int minIndex = Math.min(selection, caret);
            int maxIndex = Math.max(selection, caret);
            if (minIndex == 0 && maxIndex == length) {
                text.setLength(0);
                length = caret = 0;
            } else {
                text.delete(minIndex, maxIndex);
                length -= maxIndex - minIndex;
                caret = minIndex;
            }
        }

        @Override
        public void onKeyRepeat(int key, int scancode, int mods) {
            onRepeatableKey(key, mods);
        }

        private void onRepeatableKey(int key, int mods) {
            switch (key) {
                case GLFW_KEY_BACKSPACE -> delete(-1);
                case GLFW_KEY_DELETE -> delete(1);
                case GLFW_KEY_ENTER -> {
                    if (enterCallback != null) { // TODO чёт придумать
                        enterCallback.accept(text.toString());

                        setSelection(0, length);
                        delete(1);
                    } else {
                        onCodepoint('\n', mods);
                    }
                }
                case GLFW_KEY_RIGHT -> {
                    if (length == 0) {
                        return;
                    }

                    if (shift()) {
                        int c = caret;
                        setCaret(caret + 1);
                        if (!hasSelection) {
                            selection = c;
                            hasSelection = true;
                        }
                    } else {
                        setCaret(caret + 1);
                        resetSelection();
                    }
                }
                case GLFW_KEY_LEFT -> {
                    if (length == 0) {
                        return;
                    }

                    if (shift()) {
                        int c = caret;
                        setCaret(caret - 1);
                        if (!hasSelection) {
                            selection = c;
                            hasSelection = true;
                        }
                    } else {
                        setCaret(caret - 1);
                        resetSelection();
                    }
                }
                case GLFW_KEY_DOWN -> {
                    // ПОМНИ: ДАУН ЭТО АП, АП ЭТО ДАУН
                    if (length == 0) {
                        return;
                    }

                    int row = caretRow();
                    if (row + 1 < lines.size()) {
                        var nextLine = lines.get(row + 1);
                        int length = nextLine.length();
                        int col = caret - lines.get(row).begin;
                        if (col > length) {
                            col = length;
                        }
                        setCaret(nextLine.begin + col);
                    }
                }
                case GLFW_KEY_UP -> {
                    if (length == 0) {
                        return;
                    }

                    int row = caretRow();
                    if (row > 0) {
                        var prevLine = lines.get(row - 1);
                        int length = prevLine.length();
                        int col = caret - lines.get(row).begin;
                        if (col > length) {
                            col = length;
                        }
                        setCaret(prevLine.begin + col);
                    }
                }
                case GLFW_KEY_V -> {
                    if (ctrl()) {
                        paste(input.getClipboardText());
                    }
                }
                case GLFW_KEY_A -> {
                    if (ctrl() && length > 0) {
                        setCaret(0);
                        selection = length;
                        hasSelection = true;
                    }
                }
                case GLFW_KEY_C -> {
                    if (ctrl()) {
                        if (hasSelection) {
                            var selectionStr = text.substring(Math.min(selection, caret), Math.max(selection, caret));
                            input.setClipboardText(selectionStr);
                        }
                    }
                }
            }
        }

        private void paste(String content) {
            if (content == null) {
                return;
            }
            StringBuilder buffer = new StringBuilder();
            int textLength = text.length();
            if (hasSelection) {
                textLength -= Math.abs(caret - selection);
            }
            for (int i = 0, n = content.length(); i < n; i++) {
                if (!withinMaxLength(textLength + buffer.length())) {
                    break;
                }
                char c = content.charAt(i);
                if (c == '\r' || c == '\n') {
                    continue;
                }
                if (validator == null || validator.accept(c)) {
                    buffer.append(c);
                }
            }
            content = buffer.toString();

            if (hasSelection) {
                deleteSelection();
                resetSelection();
            }
            text.insert(caret, content);
            length = textLength + content.length();
            caret += content.length();
            textChanged = true;
        }

        private boolean withinMaxLength(int size) {
            return maxLength <= 0 || size < maxLength;
        }

        @Override
        public void onMouseDragged(float x, float y) {
            super.onMouseDragged(x, y);
            setCursorPosition(x, y);
        }

        int findClosestIndex(float x, float y) {
            var pos = positions;
            if (pos.length == 0) {
                return -1;
            }
            int best = 0;
            float bestDst2 = dst2To(pos[0], x, y);
            for (int i = 1; i < pos.length; i++) {
                float d = dst2To(pos[i], x, y);
                if (d < bestDst2) {
                    bestDst2 = d;
                    best = i;
                }
            }
            float extent = Math.max(pos[best].width, pos[best].height);
            if (bestDst2 > extent * extent) {
                return pos.length;
            }
            return best;
        }

        private float dst2To(GlyphSize p, float x, float y) {
            float cx = p.x + p.width / 2f;
            float cy = p.y + p.height / 2f;
            float dx = cx - x;
            float dy = cy - y;
            return dx*dx + dy*dy;
        }

        private int letterUnderCursor(float x, float y) {
            return findClosestIndex(x, y);
        }

        @Override
        public void onCodepoint(int codepoint, int mods) {
            if (hasSelection()) {
                deleteSelection();
                resetSelection();
            }
            if (!withinMaxLength(length + 1)) {
                return;
            }
            if (validator != null && !validator.accept(codepoint)) {
                return;
            }
            write(codepoint);
        }

        private void write(int codepoint) {
            int charc = Character.toChars(codepoint, CHARS, 0);
            text.insert(caret, CHARS, 0, charc);
            caret += charc;
            length += charc;
            textChanged = true;
        }
    }

    private void setSelection(int start, int end) {
        start = Math.min(length, start);
        end = Math.min(length, end);
        if (start == end) {
            resetSelection();
            return;
        }
        if (end < start) {
            int tmp = end;
            end = start;
            start = tmp;
        }

        this.selection = start;
        this.caret = end;
        this.hasSelection = true;
    }

    private void resetSelection() {
        hasSelection = false;
    }

    private boolean hasSelection() {
        return hasSelection && Math.abs(caret - selection) > 0;
    }

    private void setCaret(int pos) {
        caret = Math.clamp(pos, 0, length);
    }

    // endregion
}
