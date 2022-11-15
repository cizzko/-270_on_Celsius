package World;

import World.Textures.TextureDrawing;
import World.Textures.TextureLoader;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class WorldGenerator {
    public static void Generate(int SizeY, int SizeX) {
        WorldObjects[][] StaticObjects = new WorldObjects[SizeX + 1][SizeY + 1];

        for (int x = 0; x < SizeX; x++) {
            for (int y = 0; y < SizeY; y++) {
                if (x < SizeX / 2) {
                    WorldObjects air = new WorldObjects(false, false, true, false, false, false, false, null, "D:\\-270_on_Celsius\\src\\assets\\World\\air.png", x * 16, y * 32);
                    StaticObjects[x][y] = air;
                } else {
                    WorldObjects grass = new WorldObjects(false, false, false, false, true, false, false, null, "D:\\-270_on_Celsius\\src\\assets\\World\\grass1.png", x * 16, y * 32);
                    StaticObjects[x][y] = grass;
                }
            }
        }

        ByteBuffer buff = TextureLoader.ByteBufferEncoder(StaticObjects[0][0].path);
        BufferedImage image = TextureLoader.BufferedImageEncoder(StaticObjects[0][0].path);

        for (int X = 0; X < SizeX; X++) {
            for (int Y = 0; Y < SizeY; Y++) {
                if (X + 1 != SizeX) {
                    if (!StaticObjects[X + 1][Y].path.equals(StaticObjects[X][Y].path)) {
                        buff = TextureLoader.ByteBufferEncoder(StaticObjects[X][Y].path);
                        image = TextureLoader.BufferedImageEncoder(StaticObjects[X][Y].path);
                    }
                }
                else {
                    if (Y + 1 != SizeY && !StaticObjects[0][Y + 1].path.equals(StaticObjects[X][Y].path)) {
                        buff = TextureLoader.ByteBufferEncoder(StaticObjects[X][Y].path);
                        image = TextureLoader.BufferedImageEncoder(StaticObjects[X][Y].path);
                    }
                }
                TextureDrawing.draw(StaticObjects[X][Y].path, StaticObjects[X][Y].x, StaticObjects[X][Y].y, buff, image);
            }
        }
    }
}