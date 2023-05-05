package core.GUI;

import core.Logging.logger;
import core.World.Textures.TextureLoader;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class Video {
    public static ConcurrentHashMap<String, ByteBuffer[]> byteBuffers = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, BufferedImage[]> bufferedImages = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Video> video = new ConcurrentHashMap<>();
    public int layer, frame, fps, x, y, lastFrameTime;
    public boolean isPlayed;

    public Video(int layer, int fps, int x, int y) {
        this.layer = layer;
        this.frame = 1;
        this.isPlayed = true;
        this.lastFrameTime = 0;
        this.fps = fps;
        this.x = x;
        this.y = y;
    }
    public static void drawVideo(String filePath, int layer, int fps, int x, int y) {
        if (byteBuffers.get(filePath) == null || bufferedImages.get(filePath)== null) {
            try {
                File file = new File(filePath);
                ImageInputStream input = ImageIO.createImageInputStream(file);

                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    reader.setInput(input);

                    ByteBuffer[] buffer = new ByteBuffer[reader.getNumImages(true)];
                    BufferedImage[] image = new BufferedImage[reader.getNumImages(true)];

                    for (int i = 0; i < reader.getNumImages(true); i++) {
                        buffer[i] = TextureLoader.ByteBufferEncoder(new File(reader.getInput().toString()).getName());
                        image[i] = TextureLoader.BufferedImageEncoder(new File(reader.getInput().toString()).getName());
                    }
                    video.put(filePath, new Video(layer, fps, x, y));
                    byteBuffers.put(filePath, buffer);
                    bufferedImages.put(filePath, image);
                }
            } catch (Exception e) {
                logger.log(e.toString());
            }
        }
    }
}

