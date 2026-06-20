package core.World.Weather;

import core.GameObject;
import core.Load;
import core.Time;
import core.g2d.Render;
import core.g2d.StackfulRender;
import core.g2d.Texture;
import core.graphic.Color;
import core.graphic.Colorf;
import core.graphic.ShadowMap;
import core.util.Debug;
import org.lwjgl.glfw.GLFW;

import static core.Global.*;
import static core.World.WorldGenerator.WorldGeneratorConstants.COPY_SIZE;
import static core.WorldCoordinates.BLOCK_SIZE;
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

    public static double globalTime = 0f;
    private static float TIME_SPEED = 0.1f;

    private static final float TIME_DURATION = 1200;

    public static float sunLightAt(int x) {
        float time = getTimeAtWorldX(x);
        return time / TIME_DURATION;
    }

    public void update() {
        float effectiveWorldWidth = world.sizeX - COPY_SIZE;

        if (Debug.debugLevel > 0) {
            if (input.pressed(GLFW.GLFW_KEY_9))
                TIME_SPEED += 0.25f;
            if (input.pressed(GLFW.GLFW_KEY_0))
                TIME_SPEED -= 0.25f;
            if (input.justPressed(GLFW.GLFW_KEY_8))
                TIME_SPEED = 0;
        }

        globalTime += Time.delta * TIME_SPEED / BLOCK_SIZE;
        if (globalTime >= effectiveWorldWidth) {
            globalTime -= effectiveWorldWidth;
        } else if (globalTime < 0) {
            globalTime += effectiveWorldWidth;
        }

        this.currentTime = getTimeAtWorldX(player.x());
        float timeFactor = this.currentTime / TIME_DURATION;

        float nightY = -2000f;
        float peakY = 1300f;
        this.y = lerp(nightY, peakY, timeFactor);

        final int minGreen = 85;
        final int maxGreen = 255;
        float ratio = (maxGreen - minGreen) / TIME_DURATION;
        int green = (int) (minGreen + (this.currentTime * ratio));
        green = Math.clamp(green, minGreen, maxGreen);

        sunColor.set(255, green, 40, 255);

        updateGradient();
        updateNightBackground();
    }

    private static float calculateTime(double worldX) {
        float effectiveWidth = world.sizeX - COPY_SIZE;
        double deltaX = globalTime - worldX;

        double angle = (deltaX / effectiveWidth) * 2.0 * Math.PI;

        // -1 на полночь, 1 на полдень
        float cosFactor = (float) -Math.cos(angle);

        float smoothFactor = (cosFactor + 1f) / 2f;

        // smoothstep
        smoothFactor = smoothFactor * smoothFactor * (3f - 2f * smoothFactor);

        return smoothFactor * TIME_DURATION;
    }

    public static float getTimeAtWorldX(double worldX) {
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

        int aGradient = Color.toInt(alpha);
        int deleteGradient = Math.clamp(aGradient, 0, 150);
        int backGradient = Color.clamp(aGradient);

        int color = Color.rgba8888(deleteGradient, deleteGradient, deleteGradient, 0);
        // ShadowMap.setSunFade(color);

        skyColor.set(255, 255, 255, backGradient);

        ShadowMap.setDirty(true);
    }

    @Override
    public void draw() {
        StackfulRender.draw(skyBackgroundTex, skyColor, 0, 0);
        StackfulRender.blending(Render.BLENDING_PREMUL);
        StackfulRender.draw(sunsetTex, sunsetColor, 0, 0);
        StackfulRender.draw(sunTex, sunColor, x, y);
        StackfulRender.blending(Render.BLENDING_NORMAL);
    }
}
