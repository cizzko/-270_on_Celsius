package core.World.Weather;

import core.GameObject;
import core.Load;
import core.Time;
import core.g2d.StackfulRender;
import core.g2d.Texture;
import core.graphic.Color;
import core.graphic.ShadowMap;

import static core.Constants.World.COPY_SIZE;
import static core.Global.player;
import static core.Global.world;
import static core.WorldCoordinates.*;
import static core.math.MathUtil.lerp;

public class Sun extends GameObject {
    private final Color skyColor = new Color();
    private final Color sunColor = new Color();
    private final Color sunsetColor = new Color();

    public float x = 578, y = 0;
    public float currentTime = 0;

    @Load("World/Sky/skyBackground0.png")
    protected Texture skyBackgroundTex;

    @Load("World/Sun/InterpolatedSunset.png")
    protected Texture sunsetTex;

    @Load("World/Sun/sun.png")
    protected Texture sunTex;

    public static float globalTime = 0f;
    private static final float TIME_SPEED = 0.1f;

    public void update() {
        float effectiveWorldWidth = world.sizeX - COPY_SIZE;

        globalTime += Time.delta * TIME_SPEED / BLOCK_SIZE;
        if (globalTime >= effectiveWorldWidth) {
            globalTime -= effectiveWorldWidth;
        } else if (globalTime < 0) {
            globalTime += effectiveWorldWidth;
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
        green = Math.clamp(green, minGreen, maxGreen);

        sunColor.set(255, green, 40, 255);

        updateGradient();
        updateNightBackground();
    }

    private static float calculateTime(float worldX) {
        float effectiveWidth = world.sizeX - COPY_SIZE;
        float deltaX = globalTime - worldX;

        double angle = (deltaX / effectiveWidth) * 2.0 * Math.PI;
        float cosFactor = (float) Math.cos(angle);
        float angleFactor = (float) (Math.acos(cosFactor) / Math.PI);

        return (1.0f - angleFactor) * 1200f;
    }

    public static float getTimeAtWorldX(float worldX) {
        return calculateTime(worldX);
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
        aGradient = Math.clamp(aGradient, 0, 250);
        sunsetColor.set(aGradient, 0, 20, aGradient);
    }

    private void updateNightBackground() {
        float progress = currentTime / 900f;
        float alpha = 1f - progress;
        alpha = Math.clamp(alpha, 0f, 1f);

        int aGradient = (int) (255 * alpha);
        int deleteGradient = Math.clamp(aGradient, 0, 150);
        int backGradient = Math.clamp(aGradient, 0, 255);

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
        StackfulRender.draw(sunTex, sunColor, x, y);
    }
}
