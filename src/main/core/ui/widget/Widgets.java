package core.ui.widget;

import core.Global;
import core.lang.LangTranslation.Translation;
import core.ui.Style;
import core.ui.Styles;
import core.g2d.Drawable;
import core.ui.Table;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class Widgets {
    private Widgets() {}

    public static Panel panel(@Nullable String id, Style.Panel style) { return new Panel(id, style); }
    public static Panel panel(@Nullable String id) { return panel(id, Styles.DEFAULT_PANEL); }

    public static Table table(@Nullable String id) { return new Table(id); }

    public static Console console(@Nullable String id) { return new Console(id, Styles.SIMPLE_PANEL); }

    public static Image image(@Nullable String id, Drawable drawable) { return new Image(id, drawable); }

    public static Image atlasImage(String id) { return image(id, Global.atlas.get(id)); }

    public static Label label(@Nullable String id, Style.Text style, String label) { return new Label(id, style).text(label); }

    public static Button button(@Nullable String id, Style.TextButton style, String label, Runnable action) {
        return new Button(id, style)
                .label(label)
                .action(action);
    }

    public static Button buttonLocalized(@Translation String id, Style.TextButton style) {
        return new Button(id, style)
                .labelTranslation(id);
    }

    public static Button buttonLocalized(@Translation String id, Style.TextButton style, Runnable action) {
        return new Button(id, style)
                .labelTranslation(id)
                .action(action);
    }

    public static Button buttonLocalized(@Translation String id, Style.TextButton style, Consumer<? super Button> action) {
        return new Button(id, style)
                .labelTranslation(id)
                .action(action);
    }

    public static Button button(@Nullable String id, Style.TextButton style, String label, Consumer<? super Button> action) {
        return new Button(id, style)
                .label(label)
                .action(action);
    }

    public static Button button(@Nullable String id, Style.TextButton style, String label) {
        return new Button(id, style)
                .label(label);
    }

    public static ToggleButton toggleButton(@Nullable String id, Style.ToggleButton style, String label, Runnable action) {
        return toggleButton(id, style, label, false, action);
    }

    public static ToggleButton toggleButtonLocalized(@Translation String id, Style.ToggleButton style, boolean defaultState, Runnable action) {
        return new ToggleButton(id, style, defaultState)
                .labelTranslation(id)
                .action(action);
    }

    public static ToggleButton toggleButton(@Nullable String id, Style.ToggleButton style, String label, boolean defaultState, Runnable action) {
        return new ToggleButton(id, style, defaultState)
                .label(label)
                .action(action);
    }

    public static Slider slider(@Nullable String id, Style.Slider style) { return new Slider(id, style); }

    public static Stack stack(@Nullable String id) { return new Stack(id); }

    public static DockPanel dockPanel(@Nullable String id, Style.Panel style) {
        return new DockPanel(id, style);
    }

    public static DropDownMenu dropDownMenu(@Nullable String id, Style.Panel style) {
        return new DropDownMenu(id, style);
    }
}
