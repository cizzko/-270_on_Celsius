package core.g2d;

import core.assets.AssetHandler;
import core.assets.AssetReleaser;
import core.assets.AssetResolver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.Future;

import static org.lwjgl.opengl.GL20.glDeleteProgram;

public final class ShaderHandler extends AssetHandler<Shader, Void, ShaderHandler.State> {
    public ShaderHandler() {
        super(Shader.class, "shaders");
    }

    @Override
    public void release(AssetReleaser rel, Shader asset) {
        glDeleteProgram(asset.glHandle);
    }

    @Override
    public void loadAsync(AssetResolver res, String name, Void params, State state) {
        state.vertSource = res.fork(() -> Files.readString(dir.resolve(name + ".vert"), StandardCharsets.UTF_8));
        state.fragSource = res.fork(() -> Files.readString(dir.resolve(name + ".frag"), StandardCharsets.UTF_8));
    }

    @Override
    public Shader loadSync(String name, Void params, ShaderHandler.State state) {
        String vertSource = state.vertSource.resultNow();
        String fragSource = state.fragSource.resultNow();
        return Shader.load(vertSource, fragSource);
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
        private Future<String> vertSource, fragSource;
    }
}
