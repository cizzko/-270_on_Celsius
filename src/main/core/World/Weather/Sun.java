package core.World.Weather;

import core.GameObject;
import core.Load;
import core.Time;
import core.World.Textures.ShadowMap;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator.Biomes;
import core.g2d.StackfulRender;
import core.g2d.Texture;
import core.util.Color;

import static core.EventHandling.Config.getBoolean;
import static core.Global.player;
import static core.Global.world;
import static core.math.MathUtil.lerp;

public class Sun extends GameObject {
    private final Color skyColor = new Color();
    private final Color sunColor = new Color();
    private final Color sunsetColor = new Color();

    private static final float scaleX = 1.5f, scaleY = 1.5f;
    private static int lastX, nextX;
    private static Biomes lastBiome = null;
    private long lastTime;

    public float x = 520, y = 0;
    public float currentTime = 0;

    @Load("World/Sky/skyBackground0.png")
    protected Texture skyBackgroundTex;

    @Load("World/Sun/InterpolatedSunset.png")
    protected Texture sunsetTex;

    @Load("World/Sun/sun.png")
    protected Texture sunTex;

    protected String sunsetTexName() {
        String sunsetType = getBoolean("InterpolateSunset") ? "" : "non";
        return "World/Sun/" + sunsetType + "InterpolatedSunset.png";
    }

    public static float globalTime = 0f;
    private static final float TIME_SPEED = 0.1f;

    public void update() {
        float blockSize = TextureDrawing.blockSize;
        float totalWorldWidth = world.sizeX * blockSize;

        globalTime += Time.delta * TIME_SPEED;
        if (globalTime >= totalWorldWidth) {
            globalTime -= totalWorldWidth;
        } else if (globalTime < 0) {
            globalTime += totalWorldWidth;
        }

        this.currentTime = getTimeAtWorldX(player.x());

        float timeFactor = this.currentTime / 1200f;

        float nightY = -2000f;
        float peakY = 1300f;
        this.y = nightY + (peakY - nightY) * timeFactor;

        final int minGreen = 85;
        final int maxGreen = 255;
        float ratio = (maxGreen - minGreen) / 1200f;
        int green = (int) (minGreen + (this.currentTime * ratio));
        green = Math.max(minGreen, Math.min(maxGreen, green));

        sunColor.set(255, green, 40, 255);

        updateGradient();
        updateNightBackground();
    }

    public static float getTimeAtWorldX(int worldBlockX) {
        float blockSize = TextureDrawing.blockSize;
        float totalWorldWidth = world.sizeX * blockSize;

        float worldPixelX = worldBlockX * blockSize;
        float deltaX = globalTime - worldPixelX;

        double angleRad = (deltaX / totalWorldWidth) * 2.0 * Math.PI;
        float cosFactor = (float) Math.cos(angleRad);

        float angleFactor = (float) (Math.acos(cosFactor) / Math.PI);
        float distanceFactor = 1.0f - angleFactor;

        return distanceFactor * 1200f;
    }

    public static float getTimeAtWorldX(float worldPixelX) {
        float blockSize = TextureDrawing.blockSize;
        float totalWorldWidth = world.sizeX * blockSize;

        float deltaX = globalTime - worldPixelX;

        double angleRad = (deltaX / totalWorldWidth) * 2.0 * Math.PI;
        float cosFactor = (float) Math.cos(angleRad);

        float angleFactor = (float) (Math.acos(cosFactor) / Math.PI);
        float distanceFactor = 1.0f - angleFactor;

        return distanceFactor * 1200f;
    }

    //todo докалибровать
    private void updateGradient() {
        float alpha = 0;
        float startSunset = -100f;
        float endSunset = 800f;

        if (currentTime >= startSunset && currentTime <= endSunset) {
            alpha = lerp(0f, 1f, (currentTime - startSunset) / (endSunset - startSunset));
        } else if (currentTime > endSunset) {
            alpha = lerp(1f, 0f, (currentTime - endSunset) / (900f - endSunset));
        }

        int aGradient = (int) (250 * alpha);
        aGradient = Math.max(0, Math.min(250, aGradient));
        sunsetColor.set(aGradient, 0, 20, aGradient);
    }

    private void updateNightBackground() {
        float progress = currentTime / 900f;
        float alpha = 1f - progress;
        alpha = Math.max(0f, Math.min(1f, alpha));

        int aGradient = (int) (255 * alpha);
        int deleteGradient = Math.max(0, Math.min(150, aGradient));
        int backGradient = Math.max(0, Math.min(255, aGradient));

        Color color = Color.fromRgba8888(deleteGradient, deleteGradient, deleteGradient, 0);
        ShadowMap.deleteAllColor(color);
        ShadowMap.deleteAllColorDynamic(color);

        skyColor.set(255, 255, 255, backGradient);
    }

    @Override
    public void draw() {
        //todo что то с блендингом пикселей
        StackfulRender.draw(skyBackgroundTex, skyColor, 0, 0);
        StackfulRender.draw(sunsetTex, sunsetColor, 0, 0);
        StackfulRender.pushRenderList();
        StackfulRender.flush();
        StackfulRender.draw(sunTex, sunColor, 520, y);
    }
}
