package core.lang;

import java.util.List;

public record LanguageSettings(String reference, String bundle, Format format, List<String> supported) {

    public String fileName(String lang) {
        return bundle + '_' + lang + '.' + format.ext();
    }

    public boolean supports(String candidate) {
        return candidate.equals(reference) ||
               supported.contains(candidate);
    }
}
