package core.World.WorldGenerator;

import core.graphic.Layer;

import static core.Global.*;
import static core.World.WorldGenerator.WorldGenerator.DynamicObjects;

public class Backdrop {
    private static final int scaleX = 2, scaleY = 2;

    public static void update() {
        Biomes currentBack = world.getBiomes((int) DynamicObjects.getFirst().getX());

        batch.z(Layer.BACKGROUND);
        batch.pushState(() -> {
            batch.scale(scaleX, scaleY);
            batch.draw(atlas.byPath(currentBack.getBackdrop()), 0, 0);
        });
    }
}
