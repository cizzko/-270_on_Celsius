package core.Logging;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class config {
    private static Properties prop = new Properties();
    public static String jetFromConfig(String key) {
        if (prop.isEmpty()) {
            try (FileInputStream fis = new FileInputStream(".\\src\\config.properties")) {
                prop.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return prop.getProperty(key);
    }
}
