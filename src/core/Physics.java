package core;

import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator;
import core.World.WorldObjects;

public class Physics extends Thread{
    public void run(){
        WorldObjects[][] StaticObjects = WorldGenerator.Generate(50, 50);
        //main.app.offerTask(() -> start());

        int targetFps = 240;
        while(true){
            try {
                Thread.sleep(1000 / targetFps);
            }
            catch (Exception e){
                System.err.println(e);
            }
        }
    }
}
