package World;

import World.Textures.TextureDrawing;
import World.Textures.TextureLoader;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetCurrentContext;

public class WorldGenerator {
    public static void Generate(int SizeY, int SizeX, int Time, Boolean ModePvP, Boolean ModeSurvival, int Players) {
        WorldObjects[][] StaticObjects = new WorldObjects[SizeX + 20][SizeY + 20];

        int x = 0;
        int y = 0;
        while (true) {
            /*

            y
            | 4
            | 3
            | 2
            | 1
            | 0  1  2  3  4
            |-- -- -- -- -- x

            сначала заполняет все по x на первой строке, потом перескакивает на строчку выше, и пока не заполнится
            если 'x' и 'y' равны размеру буффера объектов, значит все ячейки заполнены, можно выходить
            break; прерывает цикл
            */
            WorldObjects grass = new WorldObjects(false, false, false, false, false, false, false, null, "D:\\-270_on_Celsius\\src\\assets\\TestImageForDrawing.png", x + 200, y + 200);
            StaticObjects[x][y] = grass;
            x++;
            if (x >= SizeX) {
                x = 0;
                y += 1;
            }
            if (y >= SizeY + 1) {
                System.out.println("WorldObjects is generated");
                break;
            }
        }
        Drawing(SizeY, SizeX, StaticObjects);
    }

    public static void Drawing(int SizeY, int SizeX, WorldObjects StaticObjects[][]) {
        glfwGetCurrentContext();
        boolean dec = false;
        int X = 0;
        int Y = 0;

        ByteBuffer buff = TextureLoader.ByteBufferEncoder(StaticObjects[0][0].path);
        BufferedImage image = TextureLoader.BufferedImageEncoder(StaticObjects[0][0].path);

        for (int d = 0; d <= SizeX * SizeY; d++) {
            //если следующая картинка отличается путем от текущей, то загрузит ее, должно стоять перед рисованием
            //dec просто переменная, создается перед циклом с параметром false
            //buff - ByteBuffer (название), image - BufferedImage (название)
            if (dec == true) {
                buff = TextureLoader.ByteBufferEncoder(StaticObjects[X][Y].path);
                image = TextureLoader.BufferedImageEncoder(StaticObjects[X][Y].path);
                dec = false;
            }
            if (X + 1 < SizeX && Y + 1 < SizeY) {
                if (StaticObjects[X + 1][Y + 1].path != StaticObjects[X][Y].path) {
                    dec = true;
                }
            }
            TextureDrawing.drawOnPath(StaticObjects[X][Y].path, StaticObjects[X][Y].x, StaticObjects[X][Y].y, buff, image);

            X++;
            if (X >= SizeX) {
                X = 0;
                X += 1;
            }
            if (Y >= SizeY + 1) {
                Y = 0;
                X = 0;
                buff.clear();
                image = null;
            }
        }
    }
}