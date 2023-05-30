package core.EventHandling.Logging;

import java.io.*;
import java.util.Properties;
import static core.Window.defPath;

public class Config {
    private static final Properties prop = new Properties();

    public static String jetFromConfig(String key) {
        if (prop.isEmpty()) {
            try (FileInputStream fis = new FileInputStream(defPath + "\\src\\assets\\Config.properties")) {
                prop.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return prop.getProperty(key);
    }


    public static void updateConfig(String key, String value) {
        try (FileInputStream fis = new FileInputStream(defPath + "\\src\\assets\\Config.properties");
             FileOutputStream fos = new FileOutputStream(defPath + "\\src\\assets\\Config.properties")) {

            prop.load(fis);
            prop.setProperty(key, value);
            prop.store(fos, null);

        } catch (IOException e) {
            Logger.log(e.toString());
        }
    }
}
