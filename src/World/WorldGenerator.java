package World;

public class WorldGenerator {
    public static WorldObjects[][] Generate(int SizeY, int SizeX, int Time, Boolean ModePvP, Boolean ModeSurvival, int Players){
        WorldObjects[][] objects = new WorldObjects[SizeX + 20][SizeY + 20];

        //Внимание, насрано!
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
                objects[x][y] = grass;
                GrassY += 4;

        }
        return objects;
    }
}
