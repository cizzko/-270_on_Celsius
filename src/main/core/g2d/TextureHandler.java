package core.g2d;

import core.assets.AssetHandler;
import core.assets.AssetReleaser;
import core.assets.AssetResolver;
import core.graphic.BitMap;
import core.graphic.TextureLoader;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;

import static org.lwjgl.opengl.GL46.*;

public final class TextureHandler extends AssetHandler<Texture, TextureHandler.Params, TextureHandler.State> {
    public TextureHandler() {
        super(Texture.class, "");
    }

    @Override
    public void release(AssetReleaser rel, Texture asset) {
        asset.close();
    }

    @Override
    public void loadAsync(AssetResolver res, String name, Params params, State state) {
        state.imageData = res.fork(() -> {
            Path file = dir.resolve(name);
            try (var in = Files.newInputStream(file)) {
                return TextureLoader.decodeImage(ImageIO.read(in));
            }
        });
    }

    @Override
    public Texture loadSync(AssetResolver res, String name, Params params, State state) {
        final int glTarget = GL_TEXTURE_2D;
        short glHandle = Texture.genId();

        glBindTexture(glTarget, glHandle);
        glTexParameteri(glTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(glTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(glTarget, GL_TEXTURE_WRAP_S, params.glClamp);
        glTexParameteri(glTarget, GL_TEXTURE_WRAP_T, params.glClamp);

        int w, h;
        try (var img = res.join(state.imageData)) {
            res.checkIfFailed();

            w = img.width();
            h = img.height();
            glTexImage2D(glTarget, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, img.data());
        }

        glBindTexture(glTarget, 0);
        Texture texture = new Texture(glHandle, w, h, (short) 0, (short) 0, (short) 1, (short) 1);
        ResourceCache.texturesById.put(glHandle, texture);
        return texture;
    }

    @Override
    protected Params createParams() {
        return new Params();
    }

    @Override
    protected State createState() {
        return new State();
    }


    public static final class Params {
        public int glClamp = GL_CLAMP_TO_EDGE;
    }

    public static final class State {
        private Future<BitMap> imageData;
    }
}
