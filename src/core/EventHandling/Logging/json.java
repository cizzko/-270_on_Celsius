package core.EventHandling.Logging;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.FileReader;
import static core.Window.defPath;

public class json {
    public static String lang = config.jetFromConfig("Language");
    public static String allLanguages;

    public static String getName(String key) {
        try (FileReader reader = new FileReader(defPath + "\\src\\assets\\Translate.json")) {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            key = jsonObject.getAsJsonObject(lang).get(key).getAsString();

        } catch (Exception e) {
            if (lang.equals("eng")) {
                logger.log("Some key (" + key + ") at language '" + lang + "' not found, see '" + defPath + "\\src\\assets\\Translate.json'");
                logger.logExit(1);
            }

            logger.log("Some key (" + key + ") at language '" + lang + "' not found, language set to 'eng'");
            config.updateConfig("Language", "eng");
            lang = "eng";
        }
        return key;
    }

    public static String getAllLanguages() {
        if (allLanguages == null) {
            try (FileReader reader = new FileReader(defPath + "\\src\\assets\\Translate.json")) {
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

                String[] availableLanguages = jsonObject.keySet().stream().filter(key -> !key.equalsIgnoreCase("Languages")).toArray(String[]::new);
                allLanguages = String.join(" ", availableLanguages);

                logger.log("Available languages: " + String.join(", ", availableLanguages) + "\n");
            } catch (Exception e) {
                logger.log("Error while reading languages from JSON file: " + e);
            }
        }
        return allLanguages;
    }
}
