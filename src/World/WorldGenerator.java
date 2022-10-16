package World;

import World.Textures.TextureDrawing;
import World.Textures.TextureLoader;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class WorldGenerator {
    public static void Generate(int SizeY, int SizeX, int Time, Boolean ModePvP, Boolean ModeSurvival, int Players) {
        WorldObjects[][] StaticObjects = new WorldObjects[SizeX + 20][SizeY + 20];

        int x = 0;
        int y = 0;
        while(true){
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
            WorldObjects grass = new WorldObjects(false, false, false, false, false, false, false, null, "D:\\-270_on_Celsius\\src\\assets\\grass1.png", x + 4, y + 4);
            StaticObjects[x][y] = grass;
            x++;
            if(x >= SizeX){
                x = 0;
                y += 1;
            }
            if(y >= SizeY + 1){
                System.out.println("WorldObjects generated");
                break;
            }
        }


        int X = 0;
        int Y = 0;
        boolean dec = false;
        ByteBuffer buff = TextureLoader.ByteBufferEncoder(StaticObjects[X][Y].path);
        BufferedImage image = TextureLoader.BufferedImageEncoder(StaticObjects[X][Y].path);

        for(int d = 0; d <= SizeX * SizeY; d++){
            //если следующая картинка отличается путем от текущей, то загрузит ее, должно стоять перед рисованием
            //dec просто переменная, создается перед циклом с параметром false
            //buff - ByteBuffer (название), image - BufferedImage (название)
            if (dec == true){
                buff = TextureLoader.ByteBufferEncoder(StaticObjects[X][Y].path);
                image = TextureLoader.BufferedImageEncoder(StaticObjects[X][Y].path);
                dec = false;
            }
            if(StaticObjects[X + 1][Y + 1].path != StaticObjects[X][Y].path){
                dec = true;
            }
            //тут должно быть что то типа
            TextureDrawing.drawOnPath(StaticObjects[X][Y].path, StaticObjects[X][Y].x, StaticObjects[X][Y].y, buff, image);
            //но нужно пробежаться по всем ячейкам массива (X Y), как в прошлом цикле
            //ну и реализация обновления экрана кнч
            //попробуй запустить прогу, только в path на 29 строке введи свой путь
            //и глянь в верхний левый угол после запуска
        }
    }
}
