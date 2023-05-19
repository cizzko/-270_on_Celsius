package core.Menu;

import core.Logging.json;
import core.Window;
import java.awt.*;
import static core.GUI.CreateElement.*;

public class MainMenu {
    public static void create() {
        int width = Window.width;   //ширина
        int height = Window.height; //высота

        //x, y, ширина, высота, название, видимость, простая или нет
        createPanel(width - (width / 2) * 2, (int) (height / 1.12), width, height, "defPan", true);

        //x, y, ширина, высота, не трогать, видимость, цвет width ширина height высота
        createButton((int) (width - (width / 3.5) * 2), (int) (height / 1.09f), width / 8, height / 16, json.getName("Exit"), false, false, new Color(236, 236, 236, 55));
        createButton((int) (width - (width / 2.8) * 2), (int) (height / 1.09f), width / 8, height / 16, json.getName("Settings"), false, false, new Color(236, 236, 236, 55));
        createButton((int) (width - (width / 2.05) * 2), (int) (height / 1.09f), width / 8, height / 16, json.getName("Play"), false, false, new Color(255, 80, 0, 55));
    }

    public static void delete() {
        buttons.get(json.getName("Exit")).visible = false;
        buttons.get(json.getName("Settings")).visible = false;
        buttons.get(json.getName("Play")).visible = false;

        panels.get("defPan").visible = false;
    }
}
