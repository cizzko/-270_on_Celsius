package core.World.StaticWorldObjects;

import core.World.Textures.TextureLoader;
import java.awt.*;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class StaticWAnimations {
    private static final HashMap<Point, StaticWAnimations> textures = new HashMap<>();
    private static final HashMap<Byte, TextureLoader.GifImageData> frames = new HashMap<>();

    private long lastFrameTime;
    public short totalFrames, framesSpeed;
    private short currentFrame;

    public record AnimData(short currentFrame, int width, int height, ByteBuffer currentFrameImage) {}

    private StaticWAnimations(short totalFrames, short framesSpeed) {
        this.totalFrames = totalFrames;
        this.lastFrameTime = System.currentTimeMillis();
        this.currentFrame = 0;
        this.framesSpeed = framesSpeed;
    }

    public static AnimData getCurrentFrame(short id, Point pos) {
        var path = StaticWorldObjects.getTexture(id);

        if (path != null && path.name().endsWith(".gif")) {
            StaticWAnimations animation = textures.getOrDefault(pos, null);
            TextureLoader.GifImageData data = frames.getOrDefault(StaticWorldObjects.getId(id), null);

            if (animation == null) {
                data = TextureLoader.framesDecoder(path.name());
                ByteBuffer[] textureFrames = data.data();

                animation = new StaticWAnimations((short) textureFrames.length, (short) 0);

                frames.put(StaticWorldObjects.getId(id), new TextureLoader.GifImageData(data.width(), data.height(), textureFrames));
                textures.put(pos, animation);
            }
            if (animation.framesSpeed > 0 && System.currentTimeMillis() - animation.lastFrameTime >= animation.framesSpeed) {
                animation.currentFrame++;
                animation.lastFrameTime = System.currentTimeMillis();

                if (animation.currentFrame >= animation.totalFrames) {
                    animation.currentFrame = 0;
                }
            }
            return new AnimData(animation.currentFrame, data.width(), data.height(), data.data()[animation.currentFrame]);
        }
        return null;
    }
}
