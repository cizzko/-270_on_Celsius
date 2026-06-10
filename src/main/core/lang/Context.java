package core.lang;

import com.fasterxml.jackson.databind.ObjectMapper;

public record Context(
        LanguageSettings languageSettings,
        ObjectMapper objectMapper,
        boolean removeUnknownKeys) {
}
