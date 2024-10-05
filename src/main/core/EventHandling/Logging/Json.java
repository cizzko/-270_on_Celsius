package core.EventHandling.Logging;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;

import static core.EventHandling.Logging.Logger.printException;
import static core.Global.assets;

public class Json {
    private static final HashMap<String, String> words = new HashMap<>(), keys = new HashMap<>();
    private static final String translateFile = assets.assetsDir("Translate.jsonc");

    public static String lang, allLanguages;

    // returns value from translate
    public static String getName(String key) {
        if (words.get(key) == null) {
            try (FileReader reader = new FileReader(translateFile, StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
                words.put(key, jsonObject.getAsJsonObject(lang).get(key).getAsString());
                keys.put(jsonObject.getAsJsonObject(lang).get(key).getAsString(), key);

            } catch (Exception e) {
                printException("Key '" + key + "' at language '" + lang + "' not found, see file: " + translateFile, e);
                return key;
            }
        }
        return words.get(key);
    }

    public static String getKey(String value) {
        return keys.get(value);
    }

    // returns all available languages
    public static String getAllLanguages() {
        if (allLanguages == null) {
            try (FileReader reader = new FileReader(translateFile, StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

                String[] availableLanguages = jsonObject.keySet().stream().filter(key -> !key.equalsIgnoreCase("Languages")).toArray(String[]::new);
                allLanguages = String.join(" ", availableLanguages);
            } catch (Exception e) {
                printException("Error while reading languages from JSON file", e);
            }
        }
        return allLanguages;
    }

    // returns array of all available languages
    public static String[] getAllLanguagesArray() {
        String[] availableLanguages = null;

        try (FileReader reader = new FileReader(translateFile, StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

            availableLanguages = jsonObject.keySet().stream().filter(key -> !key.equalsIgnoreCase("Languages")).toArray(String[]::new);
        } catch (Exception e) {
            printException("Error while reading languages from JSON file", e);
        }
        return availableLanguages;
    }

    // detect current system language
    public static void detectLanguage() {
        try {
            if (Config.getFromConfig("DetectLanguage").equals("true") && allLanguages.contains(lang = Locale.getDefault().getLanguage())) {
                Config.updateConfig("Language", Locale.getDefault().getLanguage());
                lang = Locale.getDefault().getLanguage();
            } else {
                lang = Config.getFromConfig("Language");
            }
        } catch (Exception e) {
            Config.updateConfig("DetectLanguage", "false");
            printException("Some error when detecting language, path: '" + translateFile + "', auto - detect deactivated, language set to " + Config.getFromConfig("Language"), e);
        }
    }
}
