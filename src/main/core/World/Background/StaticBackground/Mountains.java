package core.World.Background.StaticBackground;

import core.Global;
import core.World.Textures.TextureDrawing;
import core.g2d.Atlas;
import core.graphic.Layer;

import static core.Global.batch;

public class Mountains {
    private static int posX = Global.atlas.byPath("\\World\\mountains.png").x();

    public static void update() {
        batch.z(Layer.BACKGROUND);
    }
}
