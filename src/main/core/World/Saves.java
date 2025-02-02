package core.World;

import core.Constants;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.zip.DeflaterOutputStream;

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

                FileOutputStream fos = new FileOutputStream(assets.assetsDir().resolve("World/Saves/WorldSaves/" + name + ".ser").toString());
                fos.write(compressedBytes);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lastSaved = System.currentTimeMillis();
                saving = false;
            }
        }
    }

    private static HashMap<String, Object> addMeta(HashMap<String, Object> map) {
        map.put("VersionCreation", Constants.versionStamp);
        map.put("DateCreation", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm")));

        return map;
    }
}
