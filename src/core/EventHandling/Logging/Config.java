package core.EventHandling.Logging;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;
import static core.EventHandling.Logging.Logger.logExit;
import static core.Window.defPath;

public class Config {
    private static final Properties propConfig = new Properties(), propFC = new Properties();
    private static final HashMap<String, String> values = new HashMap<>();

    public static String getFromConfig(String key) {
        if (values.containsKey(key)) {
            return values.get(key);
        }

        if (propConfig.isEmpty()) {
            try (FileInputStream fis = new FileInputStream(defPath + "\\src\\assets\\Config.properties")) {
                propConfig.load(fis);
            } catch (Exception e) {
                logExit(1, "Error at reading config: '" + e + "' at path: " + defPath + "\\src\\assets\\Config.properties", true);
            }
        }
        String value = propConfig.getProperty(key);
        values.put(key, value);

        return value;
    }

    public static String getFromFC(String key) {
        if (values.containsKey(key)) {
            return values.get(key);
        }

        if (propFC.isEmpty()) {
            try (FileInputStream fis = new FileInputStream(defPath + "\\src\\assets\\fastCommands.properties")) {
                propFC.load(fis);
            } catch (Exception e) {
                logExit(1, "Error at reading fast commands: '" + e + "' at path: " + defPath + "\\src\\assets\\fastCommands.properties", false);
            }
        }
        String value = propFC.getProperty(key);
        values.put(key, value);

        return value;
    }


    public static void updateConfig(String key, String value) {
        try (FileInputStream fis = new FileInputStream(defPath + "\\src\\assets\\Config.properties");
             FileOutputStream fos = new FileOutputStream(defPath + "\\src\\assets\\Config.properties")) {

            propConfig.load(fis);
            propConfig.setProperty(key, value);
            propConfig.store(fos, null);
            values.put(key, value);

        } catch (Exception e) {
            logExit(1, "Error at update config: '" + e + "' at path: '" + defPath + "\\src\\assets\\Config.properties', key: '" + key + "' value: '" + value + "'", true);
        }
    }
}
