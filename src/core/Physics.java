package core;

import core.World.WorldGenerator;
import core.World.WorldObjects;

public class Physics extends Thread {
    private WorldObjects[][] Objects = WorldGenerator.Generate(50, 50);
    private WorldObjects[][] ObjectsNonProcessed = Objects; //если физика не успела обработаться, то рисует ее прошлый кадр
    private int x;
    private int y;
    volatile private boolean isProcessed = false;

    public WorldObjects[][] getWorldObjects() {
        if (isProcessed == true) {
            ObjectsNonProcessed = Objects;
            return Objects;
        }
        else {
            return ObjectsNonProcessed;
        }
    }

    public void run(){
        //main.app.offerTask(() -> start());

        // TODO: Переделать, переписать, изменить, переиначить.
        short targetFps = 240;
        while(true){
            isProcessed = false;
            x++;
            y++;

            try {
                Thread.sleep(1000 / targetFps);
            }
            catch (Exception e){
                System.err.println(e);
            }

            if (!Objects[x][y - 1].solid || Objects[x][y].player){
                Objects[x][y].y = y --;
            }
            isProcessed = true;
        }
    }
}
