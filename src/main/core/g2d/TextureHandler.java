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
        var pixels = state.imageData.resultNow();
        return Texture.load(pixels, params.target, params.minFilter, params.magFilter, params.wrapS, params.wrapT);
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
        public int target = GL_TEXTURE_2D;
        public int minFilter = GL_NEAREST;
        public int magFilter = GL_NEAREST;
        public int wrapS = GL_CLAMP_TO_EDGE;
        public int wrapT = GL_CLAMP_TO_EDGE;
    }

    public static final class State {
        private Future<BitMap> imageData;
    }
}
