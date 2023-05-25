package core.EventHandling.Logging;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import core.Window;
import java.io.FileReader;

public class json {
    public static String lang = config.jetFromConfig("Language");
    public static String allLanguages;

    public static String getName(String key) {
        try (FileReader reader = new FileReader(Window.defPath + "\\src\\assets\\Translate.json")) {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            key = jsonObject.getAsJsonObject(lang).get(key).getAsString();

        } catch (Exception e) {
            if (lang.equals("eng")) {
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
            try (FileReader reader = new FileReader(Window.defPath + "\\src\\assets\\Translate.json")) {
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);

                String[] availableLanguages = jsonObject.keySet().stream().filter(key -> !key.equalsIgnoreCase("Languages")).toArray(String[]::new);
                allLanguages = String.join(" ", availableLanguages);

                logger.log("Available languages: " + String.join(", ", availableLanguages));
            } catch (Exception e) {
                logger.log("Error while reading languages from JSON file: " + e);
            }
        }
        return allLanguages;
    }
}
