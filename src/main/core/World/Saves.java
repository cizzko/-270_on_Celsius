package core.World;

import core.EventHandling.Logging.Logger;
import core.Utils.ArrayUtils;
import core.Window;
import core.World.Creatures.DynamicWorldObjects;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.StaticWorldObjects.TemperatureMap;
import core.World.Textures.ShadowMap;
import core.World.Weather.Sun;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import static core.EventHandling.Logging.Logger.printException;
import static core.Global.assets;
import static core.Global.world;

public class Saves {
    private static boolean saving = false;
    public static long lastSaved = System.currentTimeMillis();

    private static void createWorldSave(String name) {
        if (!saving) {
            try {
                saving = true;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(addMeta(WorldGenerator.getWorldData()));
                oos.close();
                byte[] bytes = baos.toByteArray();

                ByteArrayOutputStream compressed = new ByteArrayOutputStream();
                DeflaterOutputStream dos = new DeflaterOutputStream(compressed);
                dos.write(bytes);
                dos.close();
                byte[] compressedBytes = compressed.toByteArray();

                FileOutputStream fos = new FileOutputStream(assets.assetsDir("\\World\\Saves\\WorldSaves\\" + name + ".ser"));
                fos.write(compressedBytes);
                fos.close();
            } catch (Exception e) {
                printException("Error when serialization (saving) world", e);
            } finally {
                lastSaved = System.currentTimeMillis();
                saving = false;
            }
        }
    }

    private static HashMap<String, Object> addMeta(HashMap<String, Object> map) {
        map.put("VersionCreation", Window.versionStamp);
        map.put("DateCreation", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm")));

        return map;
    }
}
