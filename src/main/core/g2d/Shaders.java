package core.g2d;

import core.assets.AssetsManager;

import static core.Global.assets;

public final class Shaders {
    private Shaders() {}

    public static Shader defaultShader;
    public static Shader repeat;
    public static Shader world;

    public static void loadAll() {
        repeat = assets.load(Shader.class, "repeat", AssetsManager.LoadType.SYNC).resultNow();
        world  = assets.load(Shader.class, "world", AssetsManager.LoadType.SYNC,
                (ShaderHandler.Params params) -> params.fragFile = "default").resultNow();

        StackfulRender.defaultShader = defaultShader =
                assets.load(Shader.class, "default", AssetsManager.LoadType.SYNC).resultNow();
    }
}
