package core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.EventHandling.Logging.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static core.Global.lang;

public final class LangTranslation {
    private static final Logger log = LogManager.getLogger();

    public static final String TRANSLATE_JSONC = "langs/Translate.jsonc";
    private static final String REFERENCE_LOCALE = "en";

    private String language;

    private final HashMap<String, String> map = new HashMap<>();
    private final ArrayList<String> languages = new ArrayList<>();

    public void load() throws IOException {
        // detect language
        if (Config.getFromConfig("DetectLanguage").equals("true")) {
            String detected = null;
            for (String candidate : new String[]{Locale.getDefault().getLanguage(), Config.getFromConfig("Language")}) {
                if (lang.getLanguages().contains(candidate)) {
                    detected = candidate;
                    break;
                }
            }
            if (detected != null) {
                Config.updateConfig("Language", detected);
            } else {
                detected = REFERENCE_LOCALE;
            }

            language = detected;
        } else {
            language = Config.getFromConfig("Language");
        }

        JsonObject json;
        try (var reader = Global.assets.resourceReader(TRANSLATE_JSONC)) {
            json = JsonParser.parseReader(reader)
                    .getAsJsonObject();
        }
        loadLanguages(json);
        loadTranslations(json.getAsJsonObject(language));
    }

    private void loadTranslations(JsonObject json) {
        map.clear();
        json.asMap().forEach((strId, translation) -> {
            map.put(strId, translation.getAsString());
        });
    }

    private void loadLanguages(JsonObject json) {
        languages.clear();
        languages.addAll(json.keySet());
    }

    public String get(String key) {
        String val = map.get(key);
        if (val == null) {
            log.warn("[Lang] Lang: '{}', key: '{}' not found", language, key);
            return key;
        }
        return val;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguage(String newLanguage) {
        if (!languages.contains(newLanguage) || language.equals(newLanguage)) {
            return;
        }
        JsonObject json;
        try (var reader = Global.assets.resourceReader(TRANSLATE_JSONC)) {
            json = JsonParser.parseReader(reader)
                    .getAsJsonObject();
        } catch (IOException e) {
            log.error("Failed to reload language from: '{}' to '{}'", language, newLanguage, e);
            return;
        }
        loadLanguages(json);
    }

    public String getCurrentLanguage() {
        return language;
    }
}
