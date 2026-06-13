
package core.ui;

import core.g2d.Fill;
import core.graphic.Color;
import core.math.MathUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;

import static core.UIScene.debugBorders;
import static core.UIScene.log;

public class Table extends LayoutGroup<Table> {

    private final ArrayList<Cell<?>> cells = new ArrayList<>();

    private float padTop, padBottom, padLeft, padRight;

    private int columns;   // итоговое число колонок
    private int rows;      // итоговое число строк
    private Align align = Align.BOTTOM_LEFT;

    public float actualWidth, actualHeight;

    private boolean rowsChanged;
    private boolean columnsChanged;

    // Результаты первого прохода
    private float[] columnMinWidth;
    private float[] columnPrefWidth;
    private float[] rowMinHeight;
    private float[] rowPrefHeight;
    private float[] columnWidth;   // финальные ширины колонок
    private float[] rowHeight;     // финальные высоты строк

    // координаты относительно таблицы
    private float[] rowY;
    private float[] colX;
    private float[] rowExpand, colExpand;

    public boolean debug;

    private final Cell<?> defaultCell = new Cell<>(null);

    public Table(@Nullable String id) {
        super(id);
    }

    public Table align(Align a) { this.align = a; return this; }

    public <E extends LayoutElement<?>> Cell<E> cell(E element, Consumer<Cell<E>> action) {
        var cell = cell(element);
        action.accept(cell);
        return cell;
    }

    public Cell<?> defaultCell() { return defaultCell; }

    public <E extends LayoutElement<?>> Cell<E> cell(E element) {
        var cell = new Cell<E>(element);
        cell.apply(defaultCell);

        if (!cells.isEmpty()) {
            var prev = cells.getLast();
            if (!prev.endRow) {
                cell.column = MathUtil.addExact(prev.column, prev.colspan);
                cell.row    = prev.row;
            } else {
                cell.column = 0;
                cell.row    = MathUtil.incrementExact(prev.row);
            }
        } else {
            cell.column = 0;
            cell.row    = 0;
        }

        add(element);
        cells.add(cell);
        return cell;
    }

    public Table row() {
        if (!cells.isEmpty()) {
            cells.getLast().endRow = true;
        }
        return this;
    }

    public Table pad(float pad) { return pad(pad, pad, pad, pad); }

    public Table pad(float top, float right, float bottom, float left) {
        this.padTop    = top;
        this.padRight  = right;
        this.padBottom = bottom;
        this.padLeft   = left;
        return this;
    }

    public void layout() {
        if (cells.isEmpty()) return;

        // Убеждаемся, что последняя строка закрыта
        cells.getLast().endRow = true;

        computeSize();
        computeLayout();
    }

    private void computeSize() {
        int columns = 0;
        int rows    = 0;
        for (var c : cells) {
            columns = Math.max(columns, c.column + c.colspan);
            rows    = Math.max(rows,    c.row + 1);
        }

        boolean columnsChanged = this.columns != columns;
        boolean rowsChanged    = this.rows != rows;

        if (columnsChanged) {
            columnMinWidth    = new float[columns];
            columnPrefWidth   = new float[columns];
        } else {
            Arrays.fill(columnMinWidth, 0);
            Arrays.fill(columnPrefWidth, 0);
        }

        if (rowsChanged) {
            rowMinHeight    = new float[rows];
            rowPrefHeight   = new float[rows];
        } else {
            Arrays.fill(rowMinHeight, 0);
            Arrays.fill(rowPrefHeight, 0);
        }

        this.rowsChanged = rowsChanged;
        this.rows = rows;
        this.columnsChanged = columnsChanged;
        this.columns = columns;

        // Ячейки БЕЗ colspan - напрямую влияют на размер колонки
        for (var c : cells) {

            float minW  = c.minWidth() + c.padX();
            float prefW = c.prefWidth() + c.padX();
            float minH  = c.minHeight() + c.padY();
            float prefH = c.prefHeight() + c.padY();

            rowMinHeight   [c.row]    = Math.max(rowMinHeight   [c.row],    minH);
            rowPrefHeight  [c.row]    = Math.max(rowPrefHeight  [c.row],    prefH);
            if (c.colspan != 1) continue;
            columnMinWidth [c.column] = Math.max(columnMinWidth [c.column], minW);
            columnPrefWidth[c.column] = Math.max(columnPrefWidth[c.column], prefW);
        }

        {
            float totalMinWidth = 0, totalPrefWidth = 0;
            float totalMinHeight = 0, totalPrefHeight = 0;
            for (int col = 0; col < columns; col++) {
                totalMinWidth  += columnMinWidth[col];
                totalPrefWidth += columnPrefWidth[col];
            }

            for (int row = 0; row < rows; row++) {
                totalMinHeight  += rowMinHeight[row];
                totalPrefHeight += rowPrefHeight[row];
            }

            prefWidth = totalPrefWidth + padLeft + padRight;
            prefHeight = totalPrefHeight + padTop + padBottom;

            minWidth = totalMinWidth + padLeft + padRight;
            minHeight = totalMinHeight + padTop + padBottom;
        }

        // Ячейки colspan - распределяем нехватку равномерно
        for (var c : cells) {
            if (c.colspan == 1) continue;

            float minW  = c.minWidth()  + c.padX();
            float prefW = c.prefWidth() + c.padX();

            // Сумма уже выделенных min/pref по span
            float spanMinW  = 0, spanPrefW = 0;
            for (int col = c.column; col < c.column + c.colspan; col++) {
                spanMinW  += columnMinWidth [col];
                spanPrefW += columnPrefWidth[col];
            }

            // Если не хватает - равномерно добавляем
            assert c.colspan != 0;
            float extraMin  = (minW  - spanMinW)  / c.colspan;
            float extraPref = (prefW - spanPrefW) / c.colspan;
            if (extraMin  > 0 || extraPref > 0) {
                for (int col = c.column; col < c.column + c.colspan; col++) {
                    if (extraMin  > 0) columnMinWidth [col] += extraMin;
                    if (extraPref > 0) columnPrefWidth[col] += extraPref;
                }
            }
        }

        // columnPrefWidth >= columnMinWidth
        for (int col = 0; col < columns; col++) {
            columnPrefWidth[col] = Math.max(columnPrefWidth[col], columnMinWidth[col]);
        }
        for (int r = 0; r < rows; r++) {
            rowPrefHeight[r] = Math.max(rowPrefHeight[r], rowMinHeight[r]);
        }
    }

    private void computeLayout() {
        float usableWidth  = width  - padLeft - padRight;
        float usableHeight = height - padTop  - padBottom;

        if (rowsChanged) {
            rowHeight = Arrays.copyOf(rowPrefHeight, rows);
        } else {
            System.arraycopy(rowPrefHeight, 0, rowHeight, 0, rows);
        }

        if (columnsChanged) {
            columnWidth = Arrays.copyOf(columnPrefWidth, columns);
        } else {
            System.arraycopy(columnPrefWidth, 0, columnWidth, 0, columns);
        }

        // горизонтальное расширение
        // TODO могли вычислять ранее
        float totalPrefW = 0;
        for (float w : columnPrefWidth) totalPrefW += w;

        float extraW = usableWidth - totalPrefW;
        if (extraW > 0) {
            // Суммируем expandX по колонкам
            if (columnsChanged || colExpand == null) {
                colExpand = new float[columns];
            } else {
                Arrays.fill(colExpand, 0);
            }

            float totalExpand = 0;
            for (var c : cells) {
                if (c.expandX > 0 && c.colspan == 1) {
                    colExpand[c.column] = Math.max(colExpand[c.column], c.expandX);
                }
            }
            for (float e : colExpand) totalExpand += e;

            if (totalExpand > 0) {
                float invTotalExpand = 1.0f / totalExpand;
                for (int col = 0; col < columnWidth.length; col++) {
                    columnWidth[col] += extraW * (colExpand[col] * invTotalExpand);
                }
            }
        }

        // вертикальное расширение
        float totalPrefH = 0;
        for (float h : rowPrefHeight) totalPrefH += h;

        float extraH = usableHeight - totalPrefH;

        if (extraH > 0) {
            if (rowsChanged || rowExpand == null) {
                rowExpand = new float[rows];
            } else {
                Arrays.fill(rowExpand, 0);
            }
            for (var c : cells) {
                if (c.expandY > 0) {
                    rowExpand[c.row] = Math.max(rowExpand[c.row], c.expandY);
                }
            }
            float totalExpandH = 0;
            for (float e : rowExpand) totalExpandH += e;

            if (totalExpandH > 0) {
                float invTotalExpandH = 1.0f / totalExpandH;
                for (int r = 0; r < rows; r++) {
                    rowHeight[r] += extraH * (rowExpand[r] * invTotalExpandH);
                }
            }
        }

        // Слева направо вычисляем позиции
        if (columnsChanged) {
            colX = new float[columns];
        } // не нужно заполнять нулями поскольку это префиксные суммы и они перезаписываются
        colX[0] = padLeft;
        for (int col = 1; col < columns; col++) {
            colX[col] = colX[col - 1] + columnWidth[col - 1];
        }

        // Сверху вниз вычисляем позиции
        if (rowsChanged) {
            rowY = new float[rows];
        } // не нужно заполнять нулями поскольку это префиксные суммы и они перезаписываются



        // Снизу вверх: row 0 внизу, row 1 выше и т.д.
        rowY[0] = padBottom;
        for (int r = 1; r < rows; r++) {
            rowY[r] = rowY[r - 1] + rowHeight[r - 1];
        }

        actualWidth = padLeft+padRight;
        actualHeight = padBottom+padTop;
        for (float v : columnWidth) actualWidth += v;
        for (float v : rowHeight) actualHeight += v;

        onSizeComplete();

        float layoutX = this.x;
        float layoutY = this.y;
        float layoutWidth = this.width;
        float layoutHeight = this.height;

        if (layoutWidth != 0)  assert actualWidth <= layoutWidth   : toString() + " " + (actualWidth) + " > " + layoutWidth;
        if (layoutHeight != 0) assert actualHeight <= layoutHeight : toString() + " " + (actualHeight) + " > " + layoutHeight;

        float x = layoutX + 0 ; // + padLeft;
        float y = layoutY + 0 ; // + padBottom;


        x += switch (align) {
            case LEFT, BOTTOM_LEFT, TOP_LEFT -> 0;
            case RIGHT, BOTTOM_RIGHT, TOP_RIGHT ->
                    layoutWidth - actualWidth;
            default -> // CENTER, TOP, BOTTOM
                    (layoutWidth - actualWidth) * 0.5f;
        };

        y += switch (align) {
            case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT -> 0;
            case TOP, TOP_LEFT, TOP_RIGHT ->
                    layoutHeight - actualHeight;
            default -> // CENTER, LEFT, RIGHT
                    (layoutHeight - actualHeight) * 0.5f;
        };

        assert x >= 0 : this;
        assert y >= 0 : this;

        for (var c : cells) {

            // Ширина ячейки = сумма колонок, покрытых colspan
            float cellW = 0;
            for (int col = c.column; col < c.column + c.colspan; col++) {
                cellW += columnWidth[col];
            }

            float widgetX      = x + colX[c.column];
            float widgetY      = y + rowY[c.row];
            float widgetWidth  = cellW;
            float widgetHeight = rowHeight[c.row];

            float spaceW = widgetWidth  - c.padX();
            float spaceH = widgetHeight - c.padY();

            // TODO max sizes
            widgetWidth  = (c.fillX > 0) ? (spaceW * c.fillX) : Math.min(c.prefWidth(),  spaceW);
            widgetHeight = (c.fillY > 0) ? (spaceH * c.fillY) : Math.min(c.prefHeight(), spaceH);

            float ox = widgetX + c.padLeft;
            float oy = widgetY + c.padBottom;

            var align = Align.TABLE[c.align];
            widgetX = switch (align) {
                case LEFT, BOTTOM_LEFT, TOP_LEFT ->
                        ox;
                case RIGHT, BOTTOM_RIGHT, TOP_RIGHT ->
                        ox + spaceW - widgetWidth;
                default -> // CENTER, TOP, BOTTOM
                        ox + (spaceW - widgetWidth) * 0.5f;
            };

            widgetY = switch (align) {
                case BOTTOM, BOTTOM_LEFT, BOTTOM_RIGHT ->
                        oy;
                case TOP, TOP_LEFT, TOP_RIGHT ->
                        oy + spaceH - widgetHeight;
                default -> // CENTER, LEFT, RIGHT
                        oy + (spaceH - widgetHeight) * 0.5f;
            };

            c.widget.set(widgetX, widgetY, widgetWidth, widgetHeight);
        }
    }

    protected void onSizeComplete() {

    }

    public ArrayList<Cell<?>> cells() { return cells; }

    @Override
    protected void drawAfterMe() {
        // if (!debug)
        //     return;
        if (!debugBorders)
            return;
        // Используем уже вычисленные координаты из computeLayout()
        for (Cell<?> c : cells) {
            var widget = c.widget;

            // Обводка самого виджета (красная)
            Fill.rectangleBorder(widget.x(), widget.y(), widget.width(), widget.height(), Color.red);

            // Обводка ячейки с учётом padding (зелёная)
            float cellX = x + colX[c.column] + c.padLeft;
            float cellY = y + rowY[c.row] + c.padTop;

            // Ширина ячейки со span (минус padding)
            float cellW = 0;
            for (int col = c.column; col < c.column + c.colspan; col++) {
                cellW += columnWidth[col];
            }
            cellW -= c.padLeft + c.padRight;

            float cellH = rowHeight[c.row] - c.padTop - c.padBottom;

            Fill.rectangleBorder(cellX, cellY, cellW, cellH, Color.green);
        }
    }

    private void debug() {
        for (Cell<?> cell : cells) {
            log.info("[R:{};C:{}] {}", cell.row, cell.column, cell.widget);
            log.info("Cell: (x:{}, y:{}, w:{}, h:{})",
                    cell.widget.x, cell.widget.y, cell.widget.width, cell.widget.height);
        }
    }

    // @Override
    // public String toString() {
    //     return super.toString() + " " + cells.stream()
    //             .map(Cell::toString)
    //             .collect(Collectors.joining("\n"));
    // }
}
