package core.EventHandling.Logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Properties;

import static core.EventHandling.Logging.Logger.printException;
import static core.Global.assets;

public class Config {
    public static final String INTERPOLATE_SUNSET_KEY = "InterpolateSunset";
    public static final String PRELOAD_RESOURCES_KEY  = "PreloadResources";
    public static final String VERTICAL_SYNC_KEY      = "VerticalSync";
    public static final String SEND_ANONYMOUS_STATISTIC_KEY = "SendAnonymousStatistics";
    public static final String SHOW_PROMPTS_KEY    = "ShowPrompts";
    public static final String DETECT_LANGUAGE_KEY = "DetectLanguage";

    private static boolean configCheckMark = false;
    private static final HashMap<String, Properties> props = new HashMap<>(3);
    private static final HashMap<String, Object> values = new HashMap<>();

    // checks if the startup configuration contains any parameters
    public static void checkConfig() {
        if (!configCheckMark) {
            copyFromResource("configDefault.properties", "config.properties");
            copyFromResource("fastCommands.properties", "fastCommands.properties");
            configCheckMark = true;
        }
    }

    // TODO выглядит как неплохая функция для AssetsManager
    static void copyFromResource(String resourceFileName, String externalFileName) {

        Path configPath = Path.of(assets.pathTo(externalFileName));
        if (Files.notExists(configPath)) {
            try (var in = assets.resourceStream(assets.assetsDir(resourceFileName)); var out = Files.newOutputStream(configPath)) {
                in.transferTo(out);
            } catch (IOException e) {
                e.printStackTrace(); // Падает с рекурсией
            }
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
        return (String) getFromProp(assets.pathTo("config.properties"), key);
    }

    // fast commands
    public static String getFromFC(String key) {
        return (String) getFromProp(assets.pathTo("fastCommands.properties"), key);
    }

    public static void updateConfig(String key, String value) {
        values.put(key, value);
        String configFile = assets.pathTo("config.properties");
        Properties configProp = props.get(configFile);
        try (var out = Files.newOutputStream(Path.of(configFile))) {
            configProp.store(out, null);
        } catch (Exception e) {
            printException("Error when updating config at path: '" + configFile + "', key: '" + key + "' value: '" + value + "'", e);
        }
    }
}
