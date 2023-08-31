package core.EventHandling.Logging;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;
import static core.EventHandling.Logging.Logger.logExit;
import static core.Window.defPath;

public class Config {
    private static final HashMap<String, Properties> props = new HashMap<>(3);
    private static final HashMap<String, String> values = new HashMap<>();

    private static String getFromProp(String path, String key) {
        if (values.containsKey(key)) {
            return values.get(key);
        }

        String value = getProperties(path).getProperty(key);
        values.put(key, value);

        return value;
    }

    public static Properties getProperties(String path) {
        if (props.get(path) == null) {
            props.put(path, new Properties());
            try {
                props.get(path).load(new FileInputStream(path));
            } catch (IOException e) {
                throw new RuntimeException("Error when get properties, file: '" + path + "', error: " + e);
            }
        }
        return props.get(path);
    }

    public static String getFromConfig(String key) {
        return getFromProp(defPath + "\\src\\assets\\Config.properties", key);
    }

    //fast commands
    public static String getFromFC(String key) {
        return getFromProp(defPath + "\\src\\assets\\fastCommands.properties", key);
    }

    public static void updateConfig(String key, String value) {
        try (FileInputStream fis = new FileInputStream(defPath + "\\src\\assets\\Config.properties");
             FileOutputStream fos = new FileOutputStream(defPath + "\\src\\assets\\Config.properties")) {
            Properties configProp = props.get(defPath + "\\src\\assets\\Config.properties");

            configProp.load(fis);
            configProp.setProperty(key, value);
            configProp.store(fos, null);
            values.put(key, value);

        } catch (Exception e) {
            logExit(1, "Error at update config: '" + e + "' at path: '" + defPath + "\\src\\assets\\Config.properties', key: '" + key + "' value: '" + value + "'", true);
        }
    }
}
