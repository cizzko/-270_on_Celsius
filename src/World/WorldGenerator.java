package World;

import World.Textures.TextureDrawing;
import World.Textures.TextureLoader;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public class WorldGenerator {
    public static void Generate(int SizeY, int SizeX, int Time, Boolean ModePvP, Boolean ModeSurvival, int Players) {
        WorldObjects[][] objects = new WorldObjects[SizeX + 20][SizeY + 20];

        //Внимание, насрано!
        //ПЕРЕДЕЛАТЬ

        int x = 0;
        BufferedImage image = null;
        ByteBuffer buffer = null;
        int GrassY = 0;
        int GrassX = 0;
        for (int y = 0; x < SizeY; y++) {
            if (x == SizeX) {
                break;
            }
            if (y == SizeY) {
                y = 0;
                GrassY = 0;
                GrassX += 4;
                x++;
            }
                WorldObjects grass = new WorldObjects(false, false, false, false, false, false, false, null, "D:\\22\\-270_on_Celsius\\src\\assets\\grass1.png", GrassX, GrassY);
                GrassY += 4;
                objects[x][y] = grass;
                System.out.println(objects[x][y].x - 4);

        }
    }
}
