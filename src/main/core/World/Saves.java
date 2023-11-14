package core.World;

import core.Utils.ArrayUtils;
import core.Window;
import core.World.Creatures.DynamicWorldObjects;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.Textures.ShadowMap;
import core.World.Weather.Sun;
import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import static core.EventHandling.Logging.Logger.printException;
import static core.Window.assetsDir;

public class Saves {
    public static void createWorldSave() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(WorldGenerator.getWorldData());
            oos.close();
            byte[] bytes = baos.toByteArray();

            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            DeflaterOutputStream dos = new DeflaterOutputStream(compressed);
            dos.write(bytes);
            dos.close();
            byte[] compressedBytes = compressed.toByteArray();

            FileOutputStream fos = new FileOutputStream(assetsDir("\\World\\Saves\\WorldSaves\\save" + LocalDate.now() + "|" + Window.versionStamp + ".ser"));
            fos.write(compressedBytes);
            fos.close();
        } catch (Exception e) {
            printException("Error when serialization (saving) world", e);
        }
    }

    public static void loadWorldSave(String path) {
        HashMap<String, Object> data = new HashMap<>();
        try (FileInputStream fis = new FileInputStream(path);
            InflaterInputStream iis = new InflaterInputStream(fis);
            ObjectInputStream ois = new ObjectInputStream(iis)) {

            data = (HashMap<String, Object>) ois.readObject();
        } catch (Exception e) {
            printException("Error when load world save, path: " + path, e);
        }

        ShadowMap.setAllData((HashMap<String, Object>) data.get("ShadowsData"));
        Inventory.inventoryObjects = (Items[][]) data.get("Inventory");
        Sun.currentTime = (float) data.get("WorldCurrentTime");

        WorldGenerator.DynamicObjects = (ArrayList<DynamicWorldObjects>) data.get("DynamicWorldObjects");
        WorldGenerator.SizeX = (int) data.get("WorldSizeX");
        WorldGenerator.SizeY = (int) data.get("WorldSizeY");
        WorldGenerator.temperatureDecrement = (float) data.get("WorldTemperatureDecrement");
        WorldGenerator.currentWorldTemperature = (float) data.get("WorldCurrentTemperature");
        WorldGenerator.intersDamageMultiplier = (float) data.get("WorldIntersDamageMultiplier");
        WorldGenerator.minVectorIntersDamage = (float) data.get("WorldMinVectorIntersDamage");
        WorldGenerator.dayCount = (int) data.get("WorldDayCount");

        StaticObjectsConst.setDestroyed();
        setBlocks((String[]) data.get("StaticWorldObjects"));
        WorldGenerator.start(data.get("WorldGenerateCreatures").equals("true"));
    }

    public static String[] loadWorldSaves() {
      return ArrayUtils.getAllFiles(assetsDir("World/Saves/WorldSaves"), ".ser");
    }

    private static void setBlocks(String[] names) {
        WorldGenerator.StaticObjects = new short[names.length];

        for (int i = 0; i < names.length; i++) {
            WorldGenerator.StaticObjects[i] = StaticWorldObjects.createStatic(names[i]);
        }
    }
}
