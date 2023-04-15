package core.Buttons;

import core.EventHandling.EventHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Buttons extends Thread {
    public static ConcurrentHashMap<String, ButtonsObjects> buttons = new ConcurrentHashMap<>();
    public static Buttons btn = new Buttons();

    public static void CreateButton(int x, int y, String path, String name, Boolean visible) {
        if (buttons.isEmpty()) {
            btn.setDaemon(true);
            btn.start();
        }
        ButtonsObjects button = new ButtonsObjects(false, visible, x, y, name, path);
        buttons.put(name, button);
    }

    @Override
    public void run() {
        while (true) {
            for (Map.Entry<String, ButtonsObjects> entry : buttons.entrySet()) {
                String button = entry.getKey();

                if (EventHandler.getRectangleClick(buttons.get(button).x, buttons.get(button).y, buttons.get(button).path) && buttons.get(button).visible) {
                    buttons.get(button).isClicked = true;
                }
                buttons.get(button).isClicked = false;
            }
        }
    }
}
