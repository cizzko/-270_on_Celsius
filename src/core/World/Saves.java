package core.World;

import core.EventHandling.Logging.Json;
import core.World.Textures.DynamicWorldObjects;
import core.World.Textures.ShadowMap;
import core.World.Textures.StaticWorldObjects.StaticWorldObjects;
import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import static core.EventHandling.Logging.Logger.printException;
import static core.UI.GUI.CreateElement.buttons;
import static core.Window.defPath;

public class Saves {

    public static void createWorldSave() {
        try {
            HashMap<String, Object> objects = new HashMap<>();
            objects.put("StaticWorldObjects", WorldGenerator.StaticObjects);
            objects.put("DynamicWorldObjects", WorldGenerator.DynamicObjects);
            objects.put("WorldGeneratorCreatures", buttons.get(Json.getName("GenerateCreatures")).isClicked);
            objects.put("ShadowsData", ShadowMap.getAllData());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(objects);
            oos.close();
            byte[] bytes = baos.toByteArray();

            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            DeflaterOutputStream dos = new DeflaterOutputStream(compressed);
            dos.write(bytes);
            dos.close();
            byte[] compressedBytes = compressed.toByteArray();

            FileOutputStream fos = new FileOutputStream(defPath + "\\src\\assets\\World\\Saves\\WorldSaves\\Saves" + LocalDate.now() + ".ser");
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

        StaticWorldObjects[] objects = (StaticWorldObjects[]) data.get("StaticWorldObjects");

        WorldGenerator.SizeY = objects.length;
        WorldGenerator.SizeX = objects.length;

        WorldGenerator.DynamicObjects = (ArrayList<DynamicWorldObjects>) data.get("DynamicWorldObjects");

        WorldGenerator.start((Boolean) data.get("WorldGeneratorCreatures"));
        ShadowMap.setAllData((HashMap<String, Object>) data.get("ShadowsData"));
    }

    public static String[] loadWorldSaves() {
      return ArrayUtils.getAllFile(defPath + "\\src\\assets\\World\\Saves\\WorldSaves", null);
    }
}
