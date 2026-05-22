package core.EventHandling;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static core.Global.assets;

public class Config {
    private static final Logger log = LogManager.getLogger();

    public static final String INTERPOLATE_SUNSET_KEY = "InterpolateSunset";
    public static final String PRELOAD_RESOURCES_KEY  = "PreloadResources";
    public static final String VERTICAL_SYNC_KEY      = "VerticalSync";
    public static final String SHOW_PROMPTS_KEY    = "ShowPrompts";
    public static final String DETECT_LANGUAGE_KEY = "DetectLanguage";

    public static final ObjectMapper json =  new ObjectMapper();

    public static final HashMap<String, String> config = new HashMap<>();

    public static String getString(String key, String def) {
        String v = config.get(key);

        if (v == null) {
            log.error("String '{}' not found in config", key);
            return def;
        }
        return v;
    }

    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public static boolean getBoolean(String key, boolean def) {
        String v = config.get(key);
        if (v == null) {
            log.error("Boolean '{}' not found in config", key);
            return def;
        }
        return Boolean.parseBoolean(v);
    }

    public static int getInt(String key, int def) {
        String v = config.get(key);
        if (v == null) {
            log.error("{} in config is null", key);
            return def;
        }
        return Integer.parseInt(v);
    }

    public static int getInt(String key) {
        return getInt(key, 0);
    }

    public static void updateConfig(String key, String value) {
        config.put(key, value);

        var externalFile = assets.workingDir().resolve("config.properties");
        var props = new Properties();
        props.putAll(config);
        try (var out = Files.newOutputStream(externalFile)) {
            props.store(out, null);
        } catch (Exception e) {
            log.error("Exception while saving '{}'", externalFile, e);
        }
    }
}
