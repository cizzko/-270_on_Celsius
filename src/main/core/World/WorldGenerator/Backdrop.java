package core.World.WorldGenerator;

import core.g2d.StackfulRender;
import core.g2d.Render;

import static core.Global.*;
import static core.graphic.GuiDrawing.blockSize;

public class Backdrop {
    //todo привязка
    private static final float scaleX = 1.5f, scaleY = 1.5f;
    //сначала сделать чтоб работало, потом делать красивее..
    //todo есть идейка с дублированием и связыванием задников надо обдумать
    private static int lastX, nextX;
    private static Biomes lastBiome = null;

    public static void update() {
        Biomes currentBiome = world.getBiomes(player.blockX());

        if (currentBiome != lastBiome) {
            lastBiome = currentBiome;
        }

        StackfulRender.pushState(() -> {
            StackfulRender.z(Render.LAYER_BACKGROUND);
            StackfulRender.scale(scaleX * 2, scaleY * 2);
            StackfulRender.draw(atlas.get(currentBiome.getBackdrop()), ((lastX - (player.x() / blockSize)) * 2) - 1500, 0);
            StackfulRender.scale(scaleX, scaleY);
            StackfulRender.draw(atlas.get(currentBiome.getBackdrop()), ((lastX - (player.x() / blockSize)) * 3) - 1500, 0);
        });
    }
}
