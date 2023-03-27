package core;

import core.World.WorldGenerator;
import core.World.WorldObjects;

public class Physics extends Thread {
    private final WorldObjects[][] Objects = WorldGenerator.GenerateWorld(40, 20);
    public WorldObjects[][] getWorldObjects() {
        return Objects;
    }

    public void run() {
        //main.app.offerTask(() -> start());
        // TODO: Переделать, переписать, изменить, переиначить.

        /* while (true) {
            try{
                Thread.sleep(1000 / 600);
            }
            catch (Exception e){}

            for (int x = 0; x < 50; x++) {
                for (int y = 0; y < 50; y++) {
                    if (Objects[x][y + 1] != null && !Objects[x][y + 1].solid && Objects[x][y].player) {
                        Objects[x][y].y += 0.1f;
                    }
                }
            }
        } */

    }
}
