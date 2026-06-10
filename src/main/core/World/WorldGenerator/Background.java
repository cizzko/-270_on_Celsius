package core.World.WorldGenerator;

import core.Constants;
import core.g2d.Render;
import core.g2d.StackfulRender;
import core.g2d.Texture;
import core.graphic.Color;

import static core.Global.player;
import static core.Global.world;

public class Background {
    private static final float SCALE_X = 1.5f;
    private static final float SCALE_Y = 1.5f;
    private static final long FADE_DURATION = 3000;
    private static final float BACKDROP_WIDTH = 3000f * SCALE_X;
    private static int color = Color.rgba8888(255, 255, 255, 255);

    static Biomes currentBiome = null;
    static Biomes previousBiome = null;
    static long swap = 0;

    private static float backgroundScrollX = 0f;
    private static double lastPlayerX = 0f;
    private static boolean isInitialized = false;

    public static void update() {
        Biomes biome = world.getBiomes(player.blockX());
        long t = System.currentTimeMillis();

        if (currentBiome == null) {
            currentBiome = biome;
        } else if (biome != currentBiome) {
            previousBiome = currentBiome;
            currentBiome = biome;
            swap = t;
        }

        float leftBorder = Constants.World.SWAP_AREA;
        float rightBorder = world.sizeX - Constants.World.SWAP_AREA;
        float worldWidth = rightBorder - leftBorder;
        double playerX = player.x();

        if (!isInitialized) {
            lastPlayerX = playerX;
            isInitialized = true;
        }

        double deltaPlayerX = playerX - lastPlayerX;
        if (Math.abs(deltaPlayerX) > worldWidth * 0.5f) {
            if (deltaPlayerX > 0) {
                deltaPlayerX -= worldWidth;
            } else {
                deltaPlayerX += worldWidth;
            }
        }

        backgroundScrollX -= (float) (deltaPlayerX * 1.2f);
        backgroundScrollX %= BACKDROP_WIDTH;
        if (backgroundScrollX > 0) {
            backgroundScrollX -= BACKDROP_WIDTH;
        }
        lastPlayerX = playerX;

        float drawX = backgroundScrollX;
        long last = t - swap;
        if (last > FADE_DURATION) {
            last = FADE_DURATION;
        }

        int alphaNew = (int) (last * 255 / FADE_DURATION);
        int alphaOld = 255 - alphaNew;

        final Biomes bPrev = previousBiome;
        final Biomes bCurr = currentBiome;

        StackfulRender.pushState(() -> {
            StackfulRender.z(Render.LAYER_BACKGROUND);
            StackfulRender.scale(SCALE_X, SCALE_Y);

            if (bPrev != null && alphaOld > 0) {
                draw(bPrev.getBackdrop(), Color.withA(color, alphaOld), drawX);
            }
            if (bCurr != null) {
                draw(bCurr.getBackdrop(), Color.withA(color, bPrev != null ? alphaNew : 255), drawX);
            }
        });

        if (last >= FADE_DURATION) {
            previousBiome = null;
        }
    }

    private static void draw(Texture backdrop, int color, float startX) {
        StackfulRender.draw(backdrop, color, startX, 0);
        StackfulRender.draw(backdrop, color, startX + BACKDROP_WIDTH, 0);
        StackfulRender.draw(backdrop, color, startX - BACKDROP_WIDTH, 0);
    }
}
