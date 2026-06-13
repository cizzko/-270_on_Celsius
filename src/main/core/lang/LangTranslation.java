package core.lang;

import core.util.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static core.Global.assets;
import static core.Global.gameSettings;

public final class LangTranslation {
    private static final Logger log = LogManager.getLogger("Lang");

    private static final String REFERENCE_LOCALE = "en";

    private String language;

    private LangSource source;
    private LanguageSettings settings;

    public Path langsDir() { return assets.assetsDir().resolve("langs"); }

    public void load() throws IOException {
        var langsDir = langsDir();
        try (var is = Files.newInputStream(langsDir.resolve("langs.json"))) {
            settings = Config.json.readValue(is, LanguageSettings.class);
        }

        // if (Config.getBoolean("DetectLanguage")) {
        //     String detected = null;
        //     for (String candidate : new String[]{Locale.getDefault().getLanguage(), Config.getString("Language", "en")}) {
        //         if (settings.supports(candidate)) {
        //             detected = candidate;
        //             break;
        //         }
        //     }
        //     if (detected == null) {
        //         detected = REFERENCE_LOCALE;
        //     }
        //
        //     language = detected;
        //     gameSettings.language = language;
        //     gameSettings.save();
        // } else {
        // }
        language = gameSettings.language;

        reloadSource();
        log.info("Loaded language: {}", language);
    }

    private void reloadSource() throws IOException {
        if (settings.reference().equals(language)) {
            source = ReferenceLangSource.INSTANCE;
        } else {
            var langFile = langsDir().resolve(settings.fileName(language));
            var ctx = new Context(settings, Config.json, false);
            source = new RegularLangSource(settings.format().readTable(langFile, ctx));
        }
    }

    public String get(@Translation String key) {
        return source.get(key);
    }

    public Stream<String> supportedLanguages() {
        return Stream.concat(Stream.of(settings.reference()), settings.supported().stream());
    }

    public void setLanguage(String newLanguage) {
        if (language.equals(newLanguage)) {
            return;
        }

        language = newLanguage;
        try {
            reloadSource();
        } catch (IOException e) {
            log.error("Failed to load source for '{}'", language, e);
        }
    }

    public String currentLanguage() {
        return language;
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.PARAMETER})
    public @interface Translation { }

    sealed interface LangSource {
        String get(String key);
    }

    enum ReferenceLangSource implements LangSource {
        INSTANCE;

        public String get(String key) {
            return key;
        }
    }

    record RegularLangSource(Map<String, String> trMap) implements LangSource {
        public String get(String key) {
            return trMap.getOrDefault(key, key);
        }
    }
}
