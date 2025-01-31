package core.ui;

import core.Global;
import core.util.Color;
import core.g2d.Font;

import static core.Global.assets;

public class Styles {
    // Задумка со стилями такая:
    //     Все типы элементов имеют свой базовый набор параметров.
    // У кнопки, например, backgroundColor, disabledColor и может ещё что. У стиля кнопки, тип которого Style.TextButton
    // поля (параметры) совпадают с полями кнопки. Стиль служит абстракцией, которая позволяет задавать значения по-умолчанию.
    // Приоритет значений следующий: параметры элемента > параметры стиля. Там мы можем отдельные элементы перекрашивать и т.п.
    // TODO: пока концепт апи интерфейс ещё находится на начальной стадии разработки я не буду заниматься глубокой оптимизацией памяти этих штук,
    //   как и добавлять неиспользуемые в игре параметры элементов в соответствующие типы элементов

    public static final Color DEFAULT_PANEL_COLOR = Color.fromRgba8888(40, 40, 40, 240);
    public static final Color TEXT_COLOR = Color.fromRgba8888(210, 210, 210, 255);
    public static final Color DIRTY_BRIGHT_BLACK = Color.fromRgba8888(10, 10, 10, 255);
    public static final Color DIRTY_BRIGHT_WHITE = Color.fromRgba8888(230, 230, 230, 255);
    public static final Color DIRTY_BLACK = Color.fromRgba8888(10, 10, 10, 55);
    public static final Color DIRTY_WHITE = Color.fromRgba8888(230, 230, 230, 55);
    public static final Color DEFAULT_ORANGE = Color.fromRgba8888(255, 80, 0, 55);

    public static final Style.ToggleButton DEFAULT_TOGGLE_BUTTON = new Style.ToggleButton() {
        @Override
        public void load() {
            font = assets.load(Font.class, "arial.ttf").resultNow();
            checkUp = Global.atlas.byPath("UI/GUI/checkMarkTrue");
            checkDown = Global.atlas.byPath("UI/GUI/checkMarkFalse");
            width = height = 44;
            backgroundColor = DIRTY_WHITE;
            borderOffset = 6;
            textOffset = 24;
        }
    };

    public static final Style.TextButton SIMPLE_TEXT_BUTTON = new Style.TextButton() {
        @Override
        public void load() {
            font = assets.load(Font.class, "arial.ttf").resultNow();
            borderWidth = 0;
            disabledColor = Color.fromRgba8888(0, 0, 0, 123);
            backgroundColor = DEFAULT_ORANGE;
        }
    };

    public static final Style.TextButton TEXT_BUTTON = new Style.TextButton() {
        @Override
        public void load() {
            font = assets.load(Font.class, "arial.ttf").resultNow();
            borderWidth = 6;
            disabledColor = Color.fromRgba8888(0, 0, 0, 123);
            backgroundColor = DEFAULT_ORANGE;
        }
    };

    public static final Style.Panel DEFAULT_PANEL = new Style.Panel() {
        @Override
        public void load() {
            borderWidth = 20;
            backgroundColor = DEFAULT_PANEL_COLOR;
        }
    };

    public static final Style.Panel SIMPLE_PANEL = new Style.Panel() {
        @Override
        public void load() {
            borderWidth = 0;
            backgroundColor = DEFAULT_PANEL_COLOR;
        }
    };

    public static final Style.Text DEFAULT_TEXT = new Style.Text() {
        @Override
        public void load() {
            color = TEXT_COLOR;
            font = assets.load(Font.class, "arial.ttf").resultNow();
        }
    };

    public static final Style.Text DEBUG_TEXT = new Style.Text() {
        @Override
        public void load() {
            color = DIRTY_BRIGHT_BLACK;
            font = assets.load(Font.class, "arial.ttf").resultNow();
        }
    };

    public static final Style.Slider DEFAULT_SLIDER = new Style.Slider() {
        @Override
        public void load() {
            font = assets.load(Font.class, "arial.ttf").resultNow();
            sliderColor = Styles.DEFAULT_PANEL_COLOR;
            dotColor = Styles.DEFAULT_ORANGE;
        }
    };

    public static void loadAll() {
        // TODO пока не вижу в этом проблемы. Пусть побудет в статическом контексте
        DEFAULT_TOGGLE_BUTTON.load();
        SIMPLE_TEXT_BUTTON.load();
        TEXT_BUTTON.load();
        DEFAULT_PANEL.load();
        SIMPLE_PANEL.load();
        DEFAULT_TEXT.load();
        DEBUG_TEXT.load();
        DEFAULT_SLIDER.load();
    }
}
