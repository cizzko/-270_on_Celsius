package core.ui;

import core.math.MathUtil;

import java.util.function.Consumer;

public final class Cell<E extends LayoutElement<?>> {
    public static final float UNSET = -1;

    // Настройки ячейки
    public float padTop, padBottom, padLeft, padRight;
    // управляет пространством МЕЖДУ колонками
    public float expandX, expandY;       // 0..1 - доля расширения
    // управляет пространством ВНУТРИ ячейки
    public float fillX, fillY;           // 0..1 - заполнение пространства
    public byte colspan;
    public byte align;

    // итоговые координаты
    public final E widget;

    private float prefWidth, prefHeight;
    private float minWidth,  minHeight;
    private float maxWidth,  maxHeight;

    byte column, row;
    boolean endRow;

    public Cell(E widget) {
        this.widget = widget;
        reset();
    }

    public float padX() { return padLeft + padRight; }
    public float padY() { return padTop + padBottom; }

    public Cell<E> padLeft(float v)   { padLeft = v; return this; }
    public Cell<E> padRight(float v)  { padRight = v; return this; }
    public Cell<E> padTop(float v)    { padTop = v; return this; }
    public Cell<E> padBottom(float v) { padBottom = v; return this; }

    public Cell<E> expandX(float v) { this.expandX = v; return this; }
    public Cell<E> expandY(float v) { this.expandY = v; return this; }

    public Cell<E> fillX(float v) { this.fillX = v; return this; }
    public Cell<E> fillY(float v) { this.fillY = v; return this; }
    public Cell<E> fill(float v)  { this.fillX = this.fillY = v; return this; }
    public Cell<E> fill()         { return fill(1); }
    public Cell<E> grow() {
        expandX = 1;
        expandY = 1;
        fillX = 1;
        fillY = 1;
        return this;
    }

    public Cell<E> growX () {
        expandX = 1;
        fillX = 1;
        return this;
    }

    public Cell<E> growY () {
        expandY = 1;
        fillY = 1;
        return this;
    }

    public Cell<E> expand() { expandX = expandY = 1; return this; }

    public Cell<E> expandX() { expandX = 1; return this; }
    public Cell<E> expandY() { expandY = 1; return this; }

    public Cell<E> expand(float x, float y) {
        expandX = x;
        expandY = y;
        return this;
    }

    public Cell<E> fixed(float width, float height) {
        fixedX(width);
        fixedY(height);
        return this;
    }

    public Cell<E> fixedX(float width) {
        minWidth = width;
        prefWidth = width;
        fillX(0);
        return this;
    }

    public Cell<E> fixedY(float height) {
        minHeight = height;
        prefHeight = height;
        fillY(0);
        return this;
    }

    public Cell<E> colspan(int v) { this.colspan = MathUtil.toByteExact(v); return this; }

    public Cell<E> align(Align a) { this.align = a.id(); return this; }

    public Cell<E> pad(float v) { return pad(v, v, v, v); }

    public Cell<E> pad(float t, float r, float b, float l) {
        padTop = t;
        padRight = r;
        padBottom = b;
        padLeft = l;
        return this;
    }

    public Cell<E> minWidth(float v)    { minWidth = v; return this; }
    public Cell<E> minHeight(float v)   { minHeight = v; return this; }
    public Cell<E> prefWidth(float v)   { prefWidth = v; return this; }
    public Cell<E> prefHeight(float v)  { prefHeight = v; return this; }
    public Cell<E> maxWidth(float v)    { maxWidth = v; return this; }
    public Cell<E> maxHeight(float v)   { maxHeight = v; return this; }

    public Cell<E> with(Consumer<E> action) {
        action.accept(widget);
        return this;
    }

    public Cell<E> reset() {
        padTop = padBottom = padLeft = padRight = 0;
        expandX = expandY = 0;
        fillX = fillY = 0;
        colspan = 1;
        align = Align.CENTER.id();
        prefWidth = prefHeight = UNSET;
        minWidth  = minHeight  = UNSET;
        maxWidth  = maxHeight  = UNSET;
        return this;
    }

    float minWidth()    { return minWidth == UNSET ? widget.minWidth() : minWidth; }
    float minHeight()   { return minHeight == UNSET ? widget.minHeight() : minHeight; }
    float prefWidth()   { return prefWidth == UNSET ? widget.prefWidth() : prefWidth; }
    float prefHeight()  { return prefHeight == UNSET ? widget.prefHeight() : prefHeight; }
    float maxWidth()    { return maxWidth == UNSET ? widget.maxWidth() : maxWidth; }
    float maxHeight()   { return maxHeight == UNSET ? widget.maxHeight() : maxHeight; }

    void apply(Cell<?> defaultCell) {
         padTop = defaultCell.padTop;
         padBottom = defaultCell.padBottom;
         padLeft = defaultCell.padLeft;
         padRight = defaultCell.padRight;
         expandX = defaultCell.expandX;
         expandY = defaultCell.expandY;
         fillX = defaultCell.fillX;
         fillY = defaultCell.fillY;
         colspan = defaultCell.colspan;
         align = defaultCell.align;
         prefWidth = defaultCell.prefWidth;
         minWidth = defaultCell.minWidth;
         maxWidth = defaultCell.maxWidth;
         prefHeight = defaultCell.prefHeight;
         minHeight = defaultCell.minHeight;
         maxHeight = defaultCell.maxHeight;
    }

    @Override
    public String toString() {
        return "Cell{" +
               "padTop=" + padTop +
               ", padBottom=" + padBottom +
               ", padLeft=" + padLeft +
               ", padRight=" + padRight +
               ", expandX=" + expandX +
               ", expandY=" + expandY +
               ", fillX=" + fillX +
               ", fillY=" + fillY +
               ", colspan=" + colspan +
               ", align=" + align +
               ", widget=" + widget +
               ", prefWidth=" + prefWidth() +
               ", prefHeight=" + prefHeight() +
               ", minWidth=" + minWidth() +
               ", minHeight=" + minHeight() +
               ", maxWidth=" + maxWidth() +
               ", maxHeight=" + maxHeight() +
               ", column=" + column +
               ", row=" + row +
               ", endRow=" + endRow +
               '}';
    }
}
