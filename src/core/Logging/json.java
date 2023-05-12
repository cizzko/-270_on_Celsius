package core.Logging;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import core.Window;
import java.io.FileReader;
import java.io.IOException;

public class json {
    public static String lang = config.jetFromConfig("Lang");

    public static String getName(String key) {
        try (FileReader reader = new FileReader(Window.defPath + "\\src\\assets\\Translate.json")) {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
            key = jsonObject.getAsJsonObject(lang).get(key).getAsString();

        } catch (IOException e) {
            logger.log(e.toString());
        }
        return key;
    }
}
