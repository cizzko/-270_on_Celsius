package core.World;

import core.Physics;
import core.World.Textures.TextureDrawing;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Hashtable;

import static core.Window.glfwWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

public class World extends Thread{
    public void run(){
        Physics physics = new Physics();
        WorldObjects[][] objectsOld;
        WorldObjects[][] objects;
        Hashtable<String, ByteBuffer> byteBuffer = WorldGenerator.GenerateByteBuffer();
        Hashtable<String, BufferedImage> bufferedImage = WorldGenerator.GenerateBufferedImage();

        while(true){
            objectsOld = physics.getWorldObjects();
            objects = physics.getWorldObjects();

            if(objects.equals(objectsOld)){
                for (int y = 0; y < 50; y++) {
                    for (int x = 0; x < 50; x++) {
                        TextureDrawing.draw(objects[x][y].path, objects[x][y].x, objects[x][y].y, byteBuffer.get(objects[x][y].path), bufferedImage.get(objects[x][y].path));

                        if(y == 50){
                            glfwSwapBuffers(glfwWindow);
                        }
                    }
                }
            }
        }
    }
}
