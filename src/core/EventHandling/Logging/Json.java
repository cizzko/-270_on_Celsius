package core.EventHandling.Logging;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.FileReader;
import static core.Window.defPath;

public class Json {
    public static String lang = Config.jetFromConfig("Language");
    public static String allLanguages;

    public static String getName(String key) {
        try (FileReader reader = new FileReader(defPath + "\\src\\assets\\Translate.json")) {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            key = jsonObject.getAsJsonObject(lang).get(key).getAsString();

        } catch (Exception e) {
            if (lang.equals("eng")) {
                Logger.log("Some key (" + key + ") at language '" + lang + "' not found, see '" + defPath + "\\src\\assets\\Translate.json'");
                Logger.logExit(1);
            }

            Logger.log("Some key (" + key + ") at language '" + lang + "' not found, language set to 'eng'");
            Config.updateConfig("Language", "eng");
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

                Logger.log("Available languages: " + String.join(", ", availableLanguages) + "\n");
            } catch (Exception e) {
                Logger.log("Error while reading languages from JSON file: " + e);
            }
        }
        return allLanguages;
    }
}
