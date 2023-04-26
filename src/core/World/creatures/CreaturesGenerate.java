package core.World.creatures;

import core.World.Textures.DynamicWorldObjects;
import core.World.WorldGenerator;

import java.nio.file.Path;
import java.nio.file.Paths;
import static core.Window.glfwWindow;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;

public class CreaturesGenerate extends Thread {
    private static int count = 0;
    private static long deltaTime = System.currentTimeMillis();
    private static String path;


    public static String getEntityName(String pathString) {
        Path path = Paths.get(pathString);
        String fileName = path.getFileName().toString();
        String nameWithoutExtension = fileName;
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex >= 0) {
            nameWithoutExtension = fileName.substring(0, dotIndex);
        }

        String[] nameParts = nameWithoutExtension.split("\\d+");
        return nameParts[0];
    }

    public void run() {
        while (!glfwWindowShouldClose(glfwWindow)) {
            if (System.currentTimeMillis() - deltaTime > 1000 && (int) (Math.random() * 1000000000) == 1 && count < 10) {
                generate();
                deltaTime = System.currentTimeMillis();
            }
            ButteflyLogic.update();
            BirdLogic.update();
        }
    }

    private static void generate() {
        //кто сделал этот костыль?
        //я
        //больше так не делай
        int rand = (int) (Math.random() * 2);
        if (rand == 1) path = ".\\src\\assets\\World\\creatures\\bird";
        if (rand == 0) path = ".\\src\\assets\\World\\creatures\\butterfly";

        for (int x = 0; x < DynamicObjects.length; x++) {
            if (DynamicObjects[x] == null) {
                DynamicObjects[x] = new DynamicWorldObjects(2, 0.1f, path, true, false, 0, 260);
                count++;
                break;
            }
            if (DynamicObjects[x].x > SizeX * 16 || DynamicObjects[x].y > SizeY * 16) {
                DynamicObjects[x] = null;
            }
        }
    }
}
