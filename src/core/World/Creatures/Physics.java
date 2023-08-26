package core.World.Creatures;

import core.EventHandling.Logging.Logger;
import core.UI.GUI.Menu.Pause;
import core.UI.GUI.Menu.Settings;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Weapons.Weapons;
import core.World.Textures.DynamicWorldObjects;
import core.World.Textures.TextureLoader;
import static core.Window.*;
import static core.World.Creatures.Player.Inventory.Items.Placeable.Factories.updateFactoriesOutput;
import static core.World.Creatures.Player.Player.*;
import static core.World.HitboxMap.*;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.*;

//version 1.2
public class Physics extends Thread {
    public static int physicsSpeed = 400, updates = 0;
    private static boolean stop = false;
    public static int lastSpeed = physicsSpeed;
    //default 400

    public void run() {
        Logger.log("Thread: Physics started");
        Inventory.create();

        long lastUpdateTime = System.nanoTime();

        while (!glfwWindowShouldClose(glfwWindow)) {
            if (System.nanoTime() - lastUpdateTime >= 1.0 / physicsSpeed * 1000000000) {
                updatePhys();
                updateWorldInteractions();

                updates++;
                lastUpdateTime = System.nanoTime();
            }
            if ((Settings.createdSettings || Pause.created) && !stop) {
                lastSpeed = physicsSpeed;
                physicsSpeed = 1;
                stop = true;
            } else if (!Settings.createdSettings && !Pause.created && stop) {
                physicsSpeed = lastSpeed;
                stop = false;
            }
        }
    }

    public static void updateVerticalSpeed(DynamicWorldObjects dynamicObject) {
        boolean intersD = checkIntersStaticD(dynamicObject.x, dynamicObject.y + dynamicObject.motionVector.y, TextureLoader.getSize(dynamicObject.path).width, TextureLoader.getSize(dynamicObject.path).height);
        boolean intersU = checkIntersStaticU(dynamicObject.x, dynamicObject.y + dynamicObject.motionVector.y, TextureLoader.getSize(dynamicObject.path).width, TextureLoader.getSize(dynamicObject.path).height);

        dynamicObject.y += dynamicObject.motionVector.y;

        if (!intersD) {
            dynamicObject.motionVector.y -= dynamicObject.weight;
        }
        if (intersD && dynamicObject.motionVector.y < 0) {
            dynamicObject.motionVector.y = 0;
        }

        if (dynamicObject.motionVector.y > 0 && intersU) {
            dynamicObject.motionVector.y = 0;
        }
    }

    public static void updateHorizontalSpeed(DynamicWorldObjects dynamicObject) {
        boolean intersR = checkIntersStaticR(dynamicObject.x + dynamicObject.motionVector.x * 2, dynamicObject.y, TextureLoader.getSize(dynamicObject.path).width, TextureLoader.getSize(dynamicObject.path).height);
        boolean intersL = checkIntersStaticL(dynamicObject.x + dynamicObject.motionVector.x * 2, dynamicObject.y, TextureLoader.getSize(dynamicObject.path).height);

        dynamicObject.x += dynamicObject.motionVector.x;

        if (!intersR && dynamicObject.motionVector.x > 0) {
            if (dynamicObject.motionVector.x - dynamicObject.weight > 0) {
                dynamicObject.motionVector.x -= dynamicObject.weight;
            } else {
                dynamicObject.motionVector.x = 0;
            }
        } else if (intersR) {
            dynamicObject.motionVector.x = 0;
        }

        if (!intersL && dynamicObject.motionVector.x < 0) {
            dynamicObject.motionVector.x += dynamicObject.weight;
        } else if (intersL) {
            dynamicObject.motionVector.x = 0;
        }
    }


    private static void updatePhys() {
        updatePlayerMove();
        updatePlayerJump();

        for (core.World.Textures.DynamicWorldObjects dynamicObject : DynamicObjects) {
            if (dynamicObject != null) {
                updateVerticalSpeed(dynamicObject);
                updateHorizontalSpeed(dynamicObject);
            }
        }
    }

    private static void updateWorldInteractions() {
        updateInventoryInteraction();
        Weapons.updateAmmo();
        updateFactoriesOutput();
    }
}