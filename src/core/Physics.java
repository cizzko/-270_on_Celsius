package core;

import core.World.WorldGenerator;
import core.World.WorldObjects;

public class Physics extends Thread {
    private final WorldObjects[][] Objects = WorldGenerator.GenerateWorld(50, 50);
    private int x = 0;
    private int y = 0;

    public WorldObjects[][] getWorldObjects() {
        return Objects;
    }

    public void run() {
        //main.app.offerTask(() -> start());

        // TODO: Переделать, переписать, изменить, переиначить.
        short targetFps = 240;
        while (true) {
            try {
                Thread.sleep(1000 / targetFps);
            } catch (Exception e) {
                System.out.println(e);
            }

            if (Objects[x][y + 1] != null && !Objects[x][y + 1].solid && Objects[x][y].player) {
                Objects[x][y].y = Objects[x][y].y--;
            }

            x++;
            if (x == 50) {
                y++;
                x = 0;
            }
            if (y == 50) {
                y = 0;
                x = 0;
            }
        }
    }
}
