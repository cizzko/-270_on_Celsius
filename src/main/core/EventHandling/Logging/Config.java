package core.EventHandling.Logging;

import core.Global;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;

import static core.EventHandling.Logging.Logger.printException;
import static core.Global.assets;

public class Config {
    private static boolean configCheckMark = false;
    private static final HashMap<String, Properties> props = new HashMap<>(3);
    private static final HashMap<String, Object> values = new HashMap<>();

    // checks if the startup configuration contains any parameters
    public static void checkConfig() {
        if (!configCheckMark) {
            Properties prop = getProperties(assets.pathTo("config.properties"));

            if (prop.isEmpty()) {
                try (PrintWriter printWriter = new PrintWriter(new FileWriter(assets.pathTo("log.txt"), StandardCharsets.UTF_8))) {
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
        Properties configProp = new Properties();
        Properties configDefaultProp = new Properties();

        try (var configDefault = assets.resourceStream(assets.assetsDir("configDefault.properties"))) {
            configDefaultProp.load(configDefault);
        } catch (IOException e) {
            e.printStackTrace(); // Падает с рекурсией
        }

        configProp.putAll(configDefaultProp);

        try (var configOutput = Files.newOutputStream(Path.of(assets.pathTo("config.properties")))) {
            configProp.store(configOutput, null);
        } catch (IOException e) {
            e.printStackTrace(); // Падает с рекурсией
        }
    }

    // when need caching values && properties
    public static Object getFromProp(String path, String key) {
        Object obj = values.get(key);
        if (obj != null) {
            return obj;
        }

        Object value = getProperties(path).getProperty(key);
        values.put(key, value);

        return value;
    }

    // when need caching only properties
    public static Properties getProperties(String path) {
        Properties props = Config.props.get(path);
        if (props == null) {
            props = new Properties();
            Config.props.put(path, props);
            try (var in = assets.resourceStream(path)) {
                if (in != null) {
                    props.load(in);
                }
            } catch (IOException e) {
                Logger.printException("Error when get properties, file: " + path, e);
            }
        }
        return props;
    }

    public static String getFromConfig(String key) {
        checkConfig();
        return (String) getFromProp(assets.assetsDir("config.properties"), key);
    }

    // fast commands
    public static String getFromFC(String key) {
        return (String) getFromProp(assets.assetsDir("fastCommands.properties"), key);
    }

    public static void updateConfig(String key, String value) {
        values.put(key, value);
        String configFile = assets.assetsDir("config.properties");
        Properties configProp = props.get(configFile);
        try (var out = Files.newOutputStream(Path.of(configFile))) {
            configProp.store(out, null);
        } catch (Exception e) {
            printException("Error when updating config at path: '" + configFile + "', key: '" + key + "' value: '" + value + "'", e);
        }
    }
}
