package core.g2d;

import core.assets.AssetsManager;

import static core.Global.assets;
import static core.util.FutureUtil.*;

public final class Shaders {
    private Shaders() {}

    public static Shader defaultShader;
    public static Shader repeat;
    public static Shader world;

    public static void loadAll() {
        repeat = join(assets.load(Shader.class, "repeat", AssetsManager.LoadType.SYNC));
        world = join(assets.load(Shader.class, "world", AssetsManager.LoadType.SYNC, (ShaderHandler.Params params) -> {
            params.fragFile = "default";
        }));

        StackfulRender.defaultShader = defaultShader = join(assets.load(Shader.class, "default", AssetsManager.LoadType.SYNC));
    }
}
