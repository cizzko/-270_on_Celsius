package core.Buttons;

import core.EventHandling.EventHandler;
import java.util.Hashtable;

public class Buttons extends Thread {
    public static int nameCount = 0;
    public static String[] names = new String[512];
    public static Hashtable<String, ButtonsObjects> buttons = new Hashtable<>();
    public static Buttons btn = new Buttons();

    public static void DeleteButton(String buttonName) {
        buttons.remove(buttonName);
    }

    public static void CreateButton(int x, int y, String path, String name, Boolean visible) {
        if (buttons.isEmpty()) {
            btn.setDaemon(true);
            btn.start();
        }
        ButtonsObjects button = new ButtonsObjects(false, visible, x, y, name, path);
        buttons.put(name, button);
        names[nameCount] = name;
        nameCount++;
    }

    public static Hashtable<String, ButtonsObjects> getButtons() {
        return buttons;
    }

    @Override
    public void run() {
        while (true) {
            try {
                for (int i = 0; i < nameCount; i++) {
                    if (EventHandler.getRectangleClick(buttons.get(names[i]).x,buttons.get(names[i]).y, 0, 0, buttons.get(names[i]).path) && buttons.get(names[i]).visible) {
                        buttons.get(names[i]).isClicked = true;
                    }
                    buttons.get(names[i]).isClicked = false;
                }
            } catch (Exception e) {
                System.err.println(e);
            }
        }
    }
}
