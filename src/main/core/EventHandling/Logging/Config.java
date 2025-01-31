package core.EventHandling.Logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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

    private static final HashMap<String, String> config = new HashMap<>();
    private static final HashMap<String, String> fastCommands = new HashMap<>();

    // checks if the startup configuration contains any parameters
    public static void checkConfig() {
        if (!configCheckMark) {
            copyFromResource(config, "configDefault.properties", "config.properties");
            copyFromResource(fastCommands, "fastCommands.properties", "fastCommands.properties");
            configCheckMark = true;
        }
    }

    // TODO выглядит как неплохая функция для AssetsManager
    static void copyFromResource(HashMap<String, String> map, String resourceFileName, String externalFileName) {

        var externalFile = assets.workingDir().resolve(externalFileName);
        if (Files.notExists(externalFile)) {
            var resourceFile = assets.assetsDir().resolve(resourceFileName);
            try {
                Files.copy(resourceFile, externalFile);
            } catch (IOException e) {
                e.printStackTrace(); // TODO
            }
        }

        var props = new Properties();
        try (var in = Files.newInputStream(externalFile)) {
            props.load(in);
        } catch (IOException e) {
            e.printStackTrace(); // TODO
        }
        @SuppressWarnings("unchecked")
        var magic = (Map<String, String>) (Map<?, ?>) props;
        map.putAll(magic);
    }

    private static final HashMap<Path, Map<String, String>> propsCache = new HashMap<>();

    public static Map<String, String> getProperties(Path path) {
        var props = Config.propsCache.get(path);
        if (props == null) {
            var tmp = new Properties();
            try (var in = Files.newInputStream(path)) {
                tmp.load(in);
            } catch (IOException e) {
                Logger.printException("Error when get properties, file: " + path, e);
            }
            @SuppressWarnings("unchecked")
            var magic = (Map<String, String>) (Map<?, ?>) tmp;
            props = new HashMap<>(magic);
            Config.propsCache.put(path, props);
        }
        return props;
    }

    public static Map<String, String> getProperties(String path) {
        return getProperties(assets.assetsDir().resolve(path));
    }

    public static String getFromConfig(String key) {
        checkConfig();
        return config.get(key);
    }

    // fast commands
    public static String getFromFC(String key) {
        return fastCommands.get(key);
    }

    public static void updateConfig(String key, String value) {
        config.put(key, value);

        var externalFile = assets.workingDir().resolve("config.properties");
        var props = new Properties();
        props.putAll(config);
        try (var out = Files.newOutputStream(externalFile)) {
            props.store(out, null);
        } catch (Exception e) {
            printException("Error when updating config at path: '" + externalFile + "', key: '" + key + "' value: '" + value + "'", e);
        }
    }
}
