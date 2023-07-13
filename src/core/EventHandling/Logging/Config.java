package core.EventHandling.Logging;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;
import static core.EventHandling.Logging.Logger.logExit;
import static core.Window.defPath;

public class Config {
    private static final Properties prop = new Properties();
    private static final HashMap<String, String> values = new HashMap<>();

    public static String getFromConfig(String key) {
        if (values.containsKey(key)) {
            return values.get(key);
        }

        if (prop.isEmpty()) {
            try (FileInputStream fis = new FileInputStream(defPath + "\\src\\assets\\Config.properties")) {
                prop.load(fis);
            } catch (Exception e) {
                logExit(1, "Error at reading config: '" + e + "' at path: " + defPath + "\\src\\assets\\Config.properties", true);
            }
        }
        String value = prop.getProperty(key);
        values.put(key, value);

        return value;
    }


    public static void updateConfig(String key, String value) {
        try (FileInputStream fis = new FileInputStream(defPath + "\\src\\assets\\Config.properties");
             FileOutputStream fos = new FileOutputStream(defPath + "\\src\\assets\\Config.properties")) {

            prop.load(fis);
            prop.setProperty(key, value);
            prop.store(fos, null);
            values.put(key, value);

        } catch (Exception e) {
            logExit(1, "Error at update config: '" + e + "' at path: '" + defPath + "\\src\\assets\\Config.properties', key: '" + key + "' value: '" + value + "'", true);
        }
    }
}
