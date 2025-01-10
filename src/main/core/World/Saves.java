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

    public static void createWorldSave() {
        // todo имя сохранений
        createWorldSave("WorldSave" + (int) (Math.random() * 10000));
    }

    public static void createWorldBackup() {
        createWorldSave("WorldBackup");
    }

    public static void loadWorldSave(String path) {
        HashMap<String, Object> data = new HashMap<>();
        try (var fis = assets.resourceStream(path);
            InflaterInputStream iis = new InflaterInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(iis)) {

            data = (HashMap<String, Object>) ois.readObject();
        } catch (Exception e) {
            printException("Error when load world save, path: " + path, e);
        }

        if (!data.get("VersionCreation").equals(Window.versionStamp)) {
            Logger.log("Save: '" + path + "' maybe deprecated, current game version: '" + Window.versionStamp + "', game version at save: '" + data.get("VersionCreation") + "'");
        }

        ShadowMap.setAllData((HashMap<String, Object>) data.get("ShadowsData"));
        Inventory.inventoryObjects = (Items[][]) data.get("Inventory");
        Sun.currentTime = (float) data.get("WorldCurrentTime");

        WorldGenerator.DynamicObjects = (ArrayDeque<DynamicWorldObjects>) data.get("DynamicWorldObjects");
        WorldGenerator.SizeX = (int) data.get("WorldSizeX");
        WorldGenerator.SizeY = (int) data.get("WorldSizeY");
        WorldGenerator.intersDamageMultiplier = (float) data.get("WorldIntersDamageMultiplier");
        WorldGenerator.minVectorIntersDamage = (float) data.get("WorldMinVectorIntersDamage");
        WorldGenerator.dayCount = (int) data.get("WorldDayCount");

        TemperatureMap.setData(data);
        StaticObjectsConst.setDestroyed();
        setBlocks((String[]) data.get("StaticWorldObjects"));
        WorldGenerator.start(data.get("WorldGenerateCreatures").equals("true"));
    }

    public static String[] loadWorldSaves() {
        return ArrayUtils.getAllFiles(assets.assetsDir("World/Saves/WorldSaves"), ".ser");
    }

    private static void setBlocks(String[] names) {
        WorldGenerator.StaticObjects = new short[names.length];

        for (int i = 0; i < names.length; i++) {
            WorldGenerator.StaticObjects[i] = StaticWorldObjects.createStatic(names[i]);
        }
    }
}
