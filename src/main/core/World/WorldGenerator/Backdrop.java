package core.World.WorldGenerator;

import core.graphic.Layer;

import static core.Global.*;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.World.WorldGenerator.WorldGenerator.DynamicObjects;

public class Backdrop {
    //todo привязка
    private static final int scaleX = 2, scaleY = 2;
    //сначала сделать чтоб работало, потом делать красивее..
    private static int lastX, nextX;
    private static Biomes lastBiome = null;

    public static void update() {
        Biomes currentBiome = world.getBiomes((int) DynamicObjects.getFirst().getX() / blockSize);

        if (currentBiome != lastBiome) {
            lastBiome = currentBiome;
            lastX = (int) DynamicObjects.getFirst().getX() / blockSize;

            for (int i = 0; i < 22; i++) {
                if (world.getBiomes(i + lastX) != currentBiome) {
                   nextX = i + lastX;
                   break;
                }
            }
        }

        batch.z(Layer.BACKGROUND);
        batch.pushState(() -> {
            batch.scale(scaleX, scaleY);
            batch.draw(atlas.byPath(currentBiome.getBackdrop()),((lastX - (DynamicObjects.getFirst().getX() / blockSize)) * 3) - 1500, 0);
        });
    }
}