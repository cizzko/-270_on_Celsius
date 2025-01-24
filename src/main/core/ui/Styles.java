package core.ui;

import core.Global;
import core.Utils.SimpleColor;
import core.Window;

public class Styles {
    // Задумка со стилями такая:
    //     Все типы элементов имеют свой базовый набор параметров.
    // У кнопки, например, backgroundColor, disabledColor и может ещё что. У стиля кнопки, тип которого Style.TextButton
    // поля (параметры) совпадают с полями кнопки. Стиль служит абстракцией, которая позволяет задавать значения по-умолчанию.
    // Приоритет значений следующий: параметры элемента > параметры стиля. Там мы можем отдельные элементы перекрашивать и т.п.
    // TODO: пока концепт апи интерфейс ещё находится на начальной стадии разработки я не буду заниматься глубокой оптимизацией памяти этих штук,
    //   как и добавлять неиспользуемые в игре параметры элементов в соответствующие типы элементов

    public static final SimpleColor DEFAULT_PANEL_COLOR = SimpleColor.fromRGBA(40, 40, 40, 240);

    public static final Style.ToggleButton DEFAULT_TOGGLE_BUTTON = new Style.ToggleButton() {
        @Override
        public void load() {
            checkUp = Global.atlas.byPath("UI/GUI/checkMarkTrue");
            checkDown = Global.atlas.byPath("UI/GUI/checkMarkFalse");
            width = height = 44;
            backgroundColor = SimpleColor.DIRTY_WHITE;
            borderOffset = 6;
            textOffset = 24;
        }
    };

    public static final Style.TextButton SIMPLE_TEXT_BUTTON = new Style.TextButton() {
        @Override
        public void load() {
            borderWidth = 0;
            disabledColor = SimpleColor.fromRGBA(0, 0, 0, 123);
            backgroundColor = SimpleColor.DEFAULT_ORANGE;
        }
    };

    public static final Style.TextButton TEXT_BUTTON = new Style.TextButton() {
        @Override
        public void load() {
            borderWidth = 6;
            disabledColor = SimpleColor.fromRGBA(0, 0, 0, 123);
            backgroundColor = SimpleColor.DEFAULT_ORANGE;
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
            color = SimpleColor.TEXT_COLOR;
            font = Window.defaultFont;
        }
    };

    public static final Style.Text DEBUG_TEXT = new Style.Text() {
        @Override
        public void load() {
            color = SimpleColor.DIRTY_BRIGHT_BLACK;
            font = Window.defaultFont;
        }
    };

    static {
        loadAll();
    }

    public static void loadAll() {
        // TODO пока не вижу в этом проблемы. Пусть побудет в статическом контексте
        DEFAULT_TOGGLE_BUTTON.load();
        SIMPLE_TEXT_BUTTON.load();
        TEXT_BUTTON.load();
        DEFAULT_PANEL.load();
        SIMPLE_PANEL.load();
        DEFAULT_TEXT.load();
        DEBUG_TEXT.load();
    }
}
