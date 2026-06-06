package core;

import com.fasterxml.jackson.core.type.TypeReference;
import core.EventHandling.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

import static core.Global.lang;

public final class LangTranslation {
    private static final Logger log = LogManager.getLogger("Lang");

    public static final String TRANSLATE_JSONC = "langs/Translate.jsonc";
    private static final String REFERENCE_LOCALE = "en";

    private String language;
    private boolean languageHasChanged = false;
    public boolean languageHasChanged() {
        return languageHasChanged;
    }

    private final HashMap<String, String> map = new HashMap<>();
    private final ArrayList<String> languages = new ArrayList<>();

    public void load() throws IOException {
        // detect language
        if (Config.getBoolean("DetectLanguage")) {
            String detected = null;
            for (String candidate : new String[]{Locale.getDefault().getLanguage(), Config.getString("Language", "en")}) {
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
            language = Config.getString("Language", "en");
        }

        loadFile();
    }

    private void loadFile() throws IOException {
        try (var reader = Global.assets.resourceStream(TRANSLATE_JSONC)) {
            var deserialized = Config.json.readValue(reader, new TypeReference<Map<String, Map<String, String>>>() {});
            loadLanguages(deserialized);
            var langMap = deserialized.get(language);
            if (langMap != null) {
                loadTranslations(langMap);
            } else {
                log.warn("Unknown language '{}'", language);
            }
        }
    }

    private void loadTranslations(Map<String, String> translation) {
        map.clear();
        map.putAll(translation);
    }

    private void loadLanguages(Map<String, Map<String, String>> json) {
        languages.clear();
        languages.addAll(json.keySet());
    }

    public String get(String key) {
        String val = map.get(key);
        if (val == null) {
            map.put(key, key);
            log.warn("[Lang] Lang: '{}', key: '{}' not found", language, key);
            return key;
        }
        return val;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguage(String newLanguage) {
        if (language.equals(newLanguage) || !languages.contains(newLanguage)) {
            return;
        }
        languageHasChanged = true;
        Global.scheduler.post(() -> languageHasChanged = false);

        var oldLanguage = language;
        language = newLanguage;
        try {
            loadFile();
        } catch (IOException e) {
            log.error("Failed to change language from '{}' to '{}'", oldLanguage, newLanguage, e);
        }
    }

    public String getCurrentLanguage() {
        return language;
    }
}
