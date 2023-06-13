package core.EventHandling.Logging;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.FileReader;
import java.util.HashMap;
import static core.Window.defPath;

public class Json {
    private static final HashMap<String, String> words = new HashMap<>();
    public static String lang = Config.getFromConfig("Language");
    public static String allLanguages;

    public static String getName(String key) {
        if (words.get(key) == null) {
            try (FileReader reader = new FileReader(defPath + "\\src\\assets\\Translate.json")) {
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
                words.put(key, jsonObject.getAsJsonObject(lang).get(key).getAsString());

            } catch (Exception e) {
                if (lang.equals("eng")) {
                    Logger.logExit(1, "Some key (" + key + ") at language '" + lang + "' not found, see '" + defPath + "\\src\\assets\\Translate.json'");
                }

                Logger.log("Some key (" + key + ") at language '" + lang + "' not found, language set to 'eng'");
                Config.updateConfig("Language", "eng");
                lang = "eng";
            }
        }
        return words.get(key);
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
