package core.g2d;

import core.assets.AssetsManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static core.Global.assets;
import static core.assets.AssetsManager.*;

public final class Shaders {
    private Shaders() {}

    public static Shader defaultShader;
    public static Shader repeat;
    public static Shader world;

    public static Shader prePressureShader;
    public static Shader thermalBuoyancyShader;
    public static Shader sorShader;
    public static Shader pressureGradientShader;
    public static Shader advectionShader;
    public static Shader radiativeCoolingShader;
    public static Shader atmosphericCoolingShader;
    public static Shader solarHeatingShader;
    public static Shader entityHeatExchangeShader;
    public static Shader pressureSmoothShader;

    public static void init() {
        defaultShader = assets.load(Shader.class, "default", LoadType.SYNC).resultNow();
        StackfulRender.init(defaultShader);
    }

    public static void loadAll() {
        repeat = assets.load(Shader.class, "repeat", LoadType.SYNC).resultNow();
        world  = assets.load(Shader.class, "world", LoadType.SYNC,
                (ShaderHandler.Params params) -> params.fragFile = "default").resultNow();

        prePressureShader        = loadComputeShader("prePressure");
        thermalBuoyancyShader    = loadComputeShader("thermalBuoyancy");
        sorShader                = loadComputeShader("sor");
        pressureGradientShader   = loadComputeShader("pressureGradient");
        advectionShader          = loadComputeShader("advection");
        radiativeCoolingShader   = loadComputeShader("radiativeCooling");
        atmosphericCoolingShader = loadComputeShader("atmosphericCooling");
        solarHeatingShader       = loadComputeShader("solarHeating");
        entityHeatExchangeShader = loadComputeShader("entityHeatExchange");
        pressureSmoothShader     = loadComputeShader("pressureSmooth");
    }

    private static Shader loadComputeShader(String name) {
        try {
            Path file = assets.assetsDir().resolve("shaders").resolve(name + ".glsl");
            String source = Files.readString(file);
            return Shader.loadCompute(name, source);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load compute shader: " + name, e);
        }
    }
}
