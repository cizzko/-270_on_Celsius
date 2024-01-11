package core.EventHandling.Logging;

import java.io.*;
import java.util.HashMap;
import java.util.Properties;
import static core.EventHandling.Logging.Logger.printException;
import static core.Window.assetsDir;
import static core.Window.pathTo;

public class Config {
    private static boolean configCheckMark = false;
    private static final HashMap<String, Properties> props = new HashMap<>(3);
    private static final HashMap<String, Object> values = new HashMap<>();

    public static void checkConfig() {
        if (!configCheckMark) {
            Properties prop = getProperties(assetsDir("config.properties"));

            if (prop == null || prop.isEmpty() || prop.keys() == null) {
                try (PrintWriter printWriter = new PrintWriter(new FileWriter(pathTo("/log.txt")))) {
                    printWriter.println("Config empty or keys not found, it will be reset to default values");
                    resetConfig();
                } catch (IOException e) {
                    printException("Error when print to log", e);
                }
            }
            configCheckMark = true;
        }
    }

    private static void resetConfig() {
        try (FileInputStream config = new FileInputStream(assetsDir("config.properties"));
             FileInputStream configDefault = new FileInputStream(assetsDir("configDefault.properties"));
             FileOutputStream out = new FileOutputStream(assetsDir("config.properties"))) {

            Properties configProp = new Properties();
            Properties defaultConfig = new Properties();

            configProp.load(config);
            defaultConfig.load(configDefault);

            String[] defaultKeys = defaultConfig.values().toArray(new String[0]);
            String[] defaultValues = defaultConfig.keySet().toArray(new String[0]);

            for (int i = 0; i < defaultValues.length; i++) {
                configProp.setProperty(defaultValues[i], defaultKeys[i]);
            }
            configProp.store(out, null);

        } catch (Exception e) {
            Logger.printException("Error when reset config: ", e);
        }
    }

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
                Logger.printException("Error when get properties, file: " + path, e);
            }
        }
        return props.get(path);
    }

    public static String getFromConfig(String key) {
        checkConfig();
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
        }
    }
}
