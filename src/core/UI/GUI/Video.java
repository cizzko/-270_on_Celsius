package core.UI.GUI;

import core.EventHandling.Logging.logger;
import core.World.Textures.TextureLoader;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.scale.AWTUtil;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class Video {
    public static ConcurrentHashMap<String, ByteBuffer> byteBuffer = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, Video> video = new ConcurrentHashMap<>();
    public int layer, frame, totalFrames, fps, x, y, height, width;
    public long lastFrameTime;
    public boolean isPlaying;

    public Video(int layer, int fps, int x, int y, int height, int width) {
        this.layer = layer;
        this.frame = 1;
        this.width = width;
        this.height = height;
        this.totalFrames = 0;
        this.isPlaying = true;
        this.lastFrameTime = System.currentTimeMillis();
        this.fps = fps;
        this.x = x;
        this.y = y;
    }

    public static void drawVideo(String path, int layer, int fps, int x, int y, int width, int height) {
        if (video.get(path) == null || !video.get(path).isPlaying) {
            new Thread(() -> {
                try {
                    File videoFile = new File(path);
                    FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(videoFile));

                    int framesCount = grab.getVideoTrack().getMeta().getTotalFrames();
                    long frameDuration = 1000 / fps;

                    video.put(path, new Video(layer, fps, x, y, height, width));
                    video.get(path).totalFrames = framesCount;
                    video.get(path).isPlaying = true;

                    for (int i = 0; i < framesCount;) {
                        long currentTime = System.currentTimeMillis();
                        long elapsedTime = currentTime - video.get(path).lastFrameTime;

                        if (elapsedTime >= frameDuration || video.get(path).frame == 1) {
                            byteBuffer.put(path, TextureLoader.ByteBufferEncoder(AWTUtil.toBufferedImage(grab.getNativeFrame())));
                            i++;

                            video.get(path).lastFrameTime = System.currentTimeMillis();
                            video.get(path).frame++;
                        }
                    }
                } catch (Exception e) {
                    logger.log(e.toString());
                }
            }).start();
        }
    }
}

