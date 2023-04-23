package core.World.creatures;

import core.World.Textures.TextureLoader;
import core.World.Textures.DynamicWorldObjects;
import static core.Window.glfwWindow;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class CreaturesLogic extends Thread {
    private static int count = 0, thisFrame = 1;
    private static long deltaTime = System.currentTimeMillis();

    public void run() {
        while (!glfwWindowShouldClose(glfwWindow)) {
            if (System.currentTimeMillis() - deltaTime > 10000 && (int) (Math.random() * 100) == 1 && count < 11) {
                for (int x = 0; x < DynamicObjects.length; x++) {
                    if (DynamicObjects[x] == null) {
                        DynamicObjects[x] = new DynamicWorldObjects(2, 0.1f, ".\\src\\assets\\World\\creatures\\butterfly", true, false, 0, 70);
                        count++;

                        for (int y = 1; y < DynamicObjects[x].framesCount + 1; y++) {
                            TextureLoader.ByteBufferEncoder(DynamicObjects[x].path + y + ".png");
                            TextureLoader.BufferedImageEncoder(DynamicObjects[x].path + y + ".png");
                        }
                        deltaTime = System.currentTimeMillis();
                        break;
                    }
                }
            }
            for (int x = 0; x < DynamicObjects.length; x++) {
                int randX = (int) (Math.random() * 60) - 25;
                int randY = (int) (Math.random() * 50) - 19;

                if (DynamicObjects[x] != null && !DynamicObjects[x].isPlayer) {
                    if (randX + DynamicObjects[x].x < SizeX * 16 && randY + DynamicObjects[x].y < SizeY * 16 && randX + DynamicObjects[x].x > 0 && randY + DynamicObjects[x].y > 0 && DynamicObjects[x].currentFrame != thisFrame) {
                        DynamicObjects[x].x += randX;
                        DynamicObjects[x].y += randY;
                        thisFrame = DynamicObjects[x].currentFrame;
                    }
                }
            }
        }
    }
}
