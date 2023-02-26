package core.Buttons;

import core.World.EventHandler;
import java.util.Hashtable;

public class Button extends Thread {
    public static int nameCount = 0;
    public static String[] names = new String[512];
    public static Hashtable<String, ButtonsObjects> buttons = new Hashtable<>();
    public static Button btn = new Button();

    public static void DeleteButton(String buttonName) {
        buttons.remove(buttonName);
    }

    public static void CreateButton(int x, int y, String path, String name, Boolean visible) {
        if (buttons.isEmpty() == true) {
            btn.setDaemon(true);
            btn.start();
        }
        ButtonsObjects button = new ButtonsObjects(false, visible, x, y, name, path);
        buttons.put(name, button);
        names[nameCount] = name;
        nameCount++;
    }

    @Override
    public void run() {
        while (true) {
            try {
                for (int i = 0; i < nameCount; i++) {
                    if (EventHandler.getRectangleClick(buttons.get(names[i]).x, buttons.get(names[i]).y, 0, 0, names[i]) == true) {

                    }
                }
            }
            catch(Exception e){
                System.err.println(e);
            }
        }
    }
}
