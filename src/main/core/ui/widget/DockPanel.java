package core.ui.widget;

import core.ui.Cell;
import core.ui.Style;
import core.ui.Table;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Consumer;

public final class DockPanel extends Panel {

    record Entry(Button button, Table content) {}

    private final ArrayList<Entry> dock = new ArrayList<>();

    public DockPanel(@Nullable String id, Style.Panel style) {
        super(id, style);
    }

    public DockPanel add(Button button, Table content, Consumer<Cell<Button>> action) {
        button.action(this::toggleThis);
        dock.add(new Entry(button, content));

        cell(button, action);
        return this;
    }

    public DockPanel add(Button button, Table content) {
        button.action(this::toggleThis);
        dock.add(new Entry(button, content));

        cell(button);
        return this;
    }

    public DockPanel showDefault(Table content) {
        for (Entry entry : dock) {
            if (entry.content == content) {
                toggleThis(entry.button);
                break;
            }
        }
        return this;
    }

    private void toggleThis(Button thisButton) {
        for (Entry entry : dock) {
            boolean showContent = entry.button == thisButton;
            if (showContent) {
                thisButton.isClickable = false;
                thisButton.isClicked = true;
            } else {
                entry.button.reset();
            }
            entry.content.setVisible(showContent);
            entry.content.setTouchable(showContent);
            entry.content.setTouchableChildren(showContent);
        }
    }
}
