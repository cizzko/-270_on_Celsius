package core.EventHandling.Logging;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import core.Window;
import java.io.FileReader;

public class json {
    public static String lang = config.jetFromConfig("Language");

    public static String getName(String key) {
        try (FileReader reader = new FileReader(Window.defPath + "\\src\\assets\\Translate.json")) {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            key = jsonObject.getAsJsonObject(lang).get(key).getAsString();

        } catch (Exception e) {
            logger.log("Some key (" + key + ") at language '" + lang + "' not found, language set to 'eng'");
            config.updateConfig("Language", "eng");
            lang = "eng";
        }
        return key;
    }
}
