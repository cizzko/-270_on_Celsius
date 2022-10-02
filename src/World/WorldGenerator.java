package World;

import java.util.HashMap;

public class WorldGenerator {
    public static void Generate(int SizeY, int SizeX, int Time, Boolean ModePvP, Boolean ModeSurvival, int Players){
        WorldObjects[][] test = new WorldObjects[SizeX + 20][SizeY + 20];

        //Внимаение, насрано!
        //переделать

        int x = 0;
        int GrassY = 0;
        int GrassX = 0;
        for (int y = 0; x < SizeY; y++) {
            if(x == SizeX){
                break;
            }
                if (y == SizeY) {
                    y = 0;
                    GrassY = 0;
                    GrassX += 4;
                    x++;
            }
                WorldObjects grass = new WorldObjects(false, false, false, false, false, false, false, null, GrassX, GrassY);
                test[x][y] = grass;
                GrassY += 4;

        }
    }
}
