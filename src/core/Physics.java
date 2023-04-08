package core;

import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator;
import core.World.WorldObjects;

public class Physics extends Thread {
    private final WorldObjects[] DynamicObjects = WorldGenerator.DynamicObjects;

    public WorldObjects[] getDynamicObjects() {
        return DynamicObjects;
    }

    public void run() {
        //main.app.offerTask(() -> start());
        // TODO: Переделать, переписать, изменить, переиначить.

        while (true) {
            try{ Thread.sleep(1000 / 600); } catch (Exception e){}

        }
    }
}