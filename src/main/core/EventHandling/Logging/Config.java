package core.EventHandling.Logging;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import static core.EventHandling.Logging.Logger.logExit;
import static core.EventHandling.Logging.Logger.printException;
import static core.Window.assetsDir;

public class Config {
    private static final HashMap<String, Properties> props = new HashMap<>(3);
    private static final HashMap<String, Object> values = new HashMap<>();

    //if need caching values && prop
    public static Object getFromProp(String path, String key) {
        if (values.get(key) != null) {
            return values.get(key);
        }

        Object value = getProperties(path).getProperty(key);
        values.put(key, value);

        return value;
    }

    //if need caching only prop
    public static Properties getProperties(String path) {
        if (props.get(path) == null) {
            props.put(path, new Properties());
            try {
                props.get(path).load(new FileInputStream(path));
            } catch (IOException e) {
                throw new RuntimeException("Error when get properties, file: " + path, e);
            }
        }
        return props.get(path);
    }

    public static String getFromConfig(String key) {
        return (String) getFromProp(assetsDir("config.properties"), key);
    }

    //fast commands
    public static String getFromFC(String key) {
        return (String) getFromProp(assetsDir("fastCommands.properties"), key);
    }

    public static void updateConfig(String key, String value) {
        String configFile = assetsDir("config.properties");

        try (FileInputStream fis = new FileInputStream(configFile);
             FileOutputStream fos = new FileOutputStream(configFile)) {
            Properties configProp = props.get(configFile);

            configProp.load(fis);
            configProp.setProperty(key, value);
            configProp.store(fos, null);
            values.put(key, value);

        } catch (Exception e) {
            printException("Error when updating config at path: '" + configFile + "', key: '" + key + "' value: '" + value + "'", e);
            logExit(1);
        }
    }
}
