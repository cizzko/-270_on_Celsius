package core.World;

import core.World.Textures.TextureLoader;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Hashtable;

public class WorldGenerator {
    public static WorldObjects[][] GenerateWorld(int SizeX, int SizeY) {
        WorldObjects[][] StaticObjects = new WorldObjects[SizeX + 1][SizeY + 1];
        int rand;

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (y < SizeY / 2) {
                    WorldObjects air = new WorldObjects(false, true, true, false, false, false, false, false, null, ".\\src\\assets\\World\\air.png", x * 16, y * 16);
                    StaticObjects[x][y] = air;
                } else {
                    rand = 1 + (int) (Math.random() * 3);
                    WorldObjects grass = new WorldObjects(false, true, false, false, true, false, false, false, null, ".\\src\\assets\\World\\grass" + rand + ".png", x * 16, y * 16);
                    StaticObjects[x][y] = grass;
                }
            }
        }
        return StaticObjects;
    }

    public static Hashtable GenerateByteBuffer(){
        Hashtable<String, ByteBuffer> byteBuffer = new Hashtable();
        String path;
        File dir = new File(".\\src\\assets\\World");

        for (File file : dir.listFiles()){
            if (file.isFile()) {
                path = file.toString();
                byteBuffer.put(path, TextureLoader.ByteBufferEncoder(path));
            }
        }
        return byteBuffer;
    }

    public static Hashtable GenerateBufferedImage(){
        Hashtable<String, BufferedImage> bufferedImage = new Hashtable();
        String path;
        File dir = new File(".\\src\\assets\\World");

        for (File file : dir.listFiles()){
            if (file.isFile()) {
                path = file.toString();
                bufferedImage.put(path, TextureLoader.BufferedImageEncoder(path));
            }
        }
        return bufferedImage;
    }
}