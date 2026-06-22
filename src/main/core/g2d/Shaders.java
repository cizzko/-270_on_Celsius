package core.g2d;

import static core.Global.assets;
import static core.assets.AssetsManager.*;

public final class Shaders {
    private Shaders() {}

    public static Shader defaultShader;
    public static Shader repeat;
    public static Shader world;

    public static void init() {
        defaultShader = assets.load(Shader.class, "default", LoadType.SYNC).resultNow();
        StackfulRender.init(defaultShader);
    }

    public static void loadAll() {
        repeat = assets.load(Shader.class, "repeat", LoadType.SYNC).resultNow();
        world  = assets.load(Shader.class, "world", LoadType.SYNC,
                (ShaderHandler.Params params) -> params.fragFile = "default").resultNow();
    }
}
