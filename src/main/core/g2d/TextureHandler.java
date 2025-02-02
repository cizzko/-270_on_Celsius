package core.g2d;

import core.assets.AssetHandler;
import core.assets.AssetReleaser;
import core.assets.AssetResolver;
import core.assets.TextureLoader;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Future;

import static org.lwjgl.opengl.GL46.*;

public final class TextureHandler extends AssetHandler<Texture, Void, TextureHandler.State> {
    public TextureHandler() {
        super(Texture.class, "");
    }

    @Override
    public void release(AssetReleaser rel, Texture asset) {
        glDeleteTextures(asset.glHandle);
        asset.glHandle = 0;
    }

    @Override
    public void loadAsync(AssetResolver res, String name, Void params, State state) {
        state.imageData = res.fork(() -> {
            Path file = dir.resolve(name);
            try (var in = Files.newInputStream(file)) {
                return TextureLoader.decodeImage(ImageIO.read(in));
            }
        });
    }

    @Override
    public Texture loadSync(String name, Void params, TextureHandler.State state) {
        final int glTarget = GL_TEXTURE_2D;
        int glHandle = glGenTextures();

        glBindTexture(glTarget, glHandle);
        glTexParameteri(glTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(glTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(glTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(glTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        int w, h;
        try (var img = state.imageData.resultNow()) {
            w = img.width();
            h = img.height();
            glTexImage2D(glTarget, 0, GL_RGBA, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, img.data());
        }

        glBindTexture(glTarget, 0);
        return new Texture(glHandle, w, h, 0, 0, 1, 1);
    }

    @Override
    protected Void createParams() {
        return null;
    }

    @Override
    protected State createState() {
        return new State();
    }

    public static final class State {
        private Future<BitMap> imageData;
    }
}
