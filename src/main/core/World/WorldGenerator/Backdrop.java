package core.World.WorldGenerator;

import core.graphic.Layer;

import static core.Global.*;
import static core.World.Textures.TextureDrawing.blockSize;

public class Backdrop {
    //todo привязка
    private static final float scaleX = 1.5f, scaleY = 1.5f;
    //сначала сделать чтоб работало, потом делать красивее..
    //todo есть идейка с дублированием и связыванием задников надо обдумать
    private static int lastX, nextX;
    private static Biomes lastBiome = null;

    public static void update() {
        Biomes currentBiome = world.getBiomes(player.getBlockX());

        if (currentBiome != lastBiome) {
            lastBiome = currentBiome;
            //lastX = (int) DynamicObjects.getFirst().getX() / blockSize;
        }

        batch.z(Layer.BACKGROUND);
        batch.pushState(() -> {
            batch.scale(scaleX * 2, scaleY * 2);
            batch.draw(atlas.byPath(currentBiome.getBackdrop()),((lastX - (player.getX() / blockSize)) * 2) - 1500, 0);
            batch.scale(scaleX, scaleY);
            batch.draw(atlas.byPath(currentBiome.getBackdrop()),((lastX - (player.getX() / blockSize)) * 3) - 1500, 0);
        });
    }
}
