package core.g2d;

import core.assets.AssetsManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static core.Global.assets;

public final class Shaders {
    private Shaders() {}

    public static Shader defaultShader;
    public static Shader repeat;
    public static Shader world;

    //todo часть отведена под будущий гпу компут
    //я не стал его пока включать из за блокировки рендера
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

    public static void loadAll() {
        repeat = assets.load(Shader.class, "repeat", AssetsManager.LoadType.SYNC).resultNow();
        world  = assets.load(Shader.class, "world", AssetsManager.LoadType.SYNC,
                (ShaderHandler.Params params) -> params.fragFile = "default").resultNow();
        StackfulRender.defaultShader = defaultShader =
                assets.load(Shader.class, "default", AssetsManager.LoadType.SYNC).resultNow();

        prePressureShader        = loadCompute("prePressure");
        thermalBuoyancyShader    = loadCompute("thermalBuoyancy");
        sorShader                = loadCompute("sor");
        pressureGradientShader   = loadCompute("pressureGradient");
        advectionShader          = loadCompute("advection");
        radiativeCoolingShader   = loadCompute("radiativeCooling");
        atmosphericCoolingShader = loadCompute("atmosphericCooling");
        solarHeatingShader       = loadCompute("solarHeating");
        entityHeatExchangeShader = loadCompute("entityHeatExchange");
        pressureSmoothShader     = loadCompute("pressureSmooth");
    }

    private static Shader loadCompute(String name) {
        try {
            Path file = assets.assetsDir().resolve("shaders").resolve(name + ".glsl");
            String source = Files.readString(file);
            return Shader.loadCompute(name, source);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load compute shader: " + name, e);
        }
    }
}