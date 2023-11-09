package core.World.Creatures;

import core.EventHandling.Logging.Logger;
import core.UI.GUI.Menu.Pause;
import core.UI.GUI.Menu.Settings;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Weapons.Weapons;
import core.World.HitboxMap;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.Textures.TextureLoader;
import core.World.WorldGenerator;
import java.awt.*;
import java.util.ArrayList;
import static core.Window.*;
import static core.World.StaticWorldObjects.Structures.Factories.updateFactoriesOutput;
import static core.World.Creatures.Player.Player.*;
import static core.World.HitboxMap.*;
import static core.World.StaticWorldObjects.StaticWorldObjects.getResistance;
import static core.World.StaticWorldObjects.StaticWorldObjects.getType;
import static core.World.WorldGenerator.*;
import static org.lwjgl.glfw.GLFW.*;

//version 1.5
public class Physics extends Thread {
    //default 400
    public static int physicsSpeed = 400, updates = 0;
    private static boolean stop = false;
    public static int lastSpeed = physicsSpeed;

    public static void restart() {
        physicsSpeed = 400;
        updates = 0;
        stop = false;
        new Physics().start();

        Logger.log("Thread: Physics restarted");
    }

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
        dynamicObject.motionVector.y *= 1 - (getTotalResistanceInside(dynamicObject) / 100f);

        boolean intersD = checkIntersStaticD(dynamicObject.x, dynamicObject.y + dynamicObject.motionVector.y, TextureLoader.getSize(dynamicObject.path).width(), TextureLoader.getSize(dynamicObject.path).height());
        boolean intersU = checkIntersStaticU(dynamicObject.x, dynamicObject.y + dynamicObject.motionVector.y, TextureLoader.getSize(dynamicObject.path).width(), TextureLoader.getSize(dynamicObject.path).height());

        if (!intersD) {
            dynamicObject.motionVector.y -= dynamicObject.weight;
        }
        if (intersD && dynamicObject.motionVector.y < 0) {
            decrementHp(dynamicObject);
            dynamicObject.motionVector.y = 0;
        }

        if (dynamicObject.motionVector.y > 0 && intersU) {
            decrementHp(dynamicObject);
            dynamicObject.motionVector.y = 0;
        }

        dynamicObject.y += dynamicObject.motionVector.y;
    }

    public static void updateHorizontalSpeed(DynamicWorldObjects dynamicObject) {
        dynamicObject.motionVector.x *= 1 - (getTotalResistanceInside(dynamicObject) / 100f);

        boolean intersR = checkIntersStaticR(dynamicObject.x + dynamicObject.motionVector.x * 2, dynamicObject.y, TextureLoader.getSize(dynamicObject.path).width(), TextureLoader.getSize(dynamicObject.path).height());
        boolean intersL = checkIntersStaticL(dynamicObject.x + dynamicObject.motionVector.x * 2, dynamicObject.y, TextureLoader.getSize(dynamicObject.path).height());

        if (!intersR && dynamicObject.motionVector.x > 0) {
            if (dynamicObject.motionVector.x - dynamicObject.weight > 0) {
                dynamicObject.motionVector.x -= dynamicObject.weight;
            } else {
                dynamicObject.motionVector.x = 0;
            }
        } else if (intersR) {
            decrementHp(dynamicObject);
            dynamicObject.motionVector.x = 0;
        }

        if (!intersL && dynamicObject.motionVector.x < 0) {
            dynamicObject.motionVector.x += dynamicObject.weight;
        } else if (intersL) {
            decrementHp(dynamicObject);
            dynamicObject.motionVector.x = 0;
        }

        dynamicObject.x += dynamicObject.motionVector.x;
    }

    private static void decrementHp(DynamicWorldObjects object) {
        float vectorX = object.motionVector.x;
        float vectorY = object.motionVector.y;

        if (vectorX > minVectorIntersDamage || vectorX < -minVectorIntersDamage || vectorY > minVectorIntersDamage || vectorY < -minVectorIntersDamage) {
            Point[] staticObjectPoint = HitboxMap.checkIntersOutside(object.x + vectorX * 2, object.y + vectorY, TextureLoader.getSize(object.path).width(), TextureLoader.getSize(object.path).height() + 4);

            if (staticObjectPoint != null) {
                float damage = 0;
                for (Point point : staticObjectPoint) {
                    short staticObject = WorldGenerator.getObject(point.x, point.y);
                    float currentDamage = ((((StaticWorldObjects.getResistance(staticObject) / 100) * StaticWorldObjects.getDensity(staticObject)) + (object.weight + (Math.max(Math.abs(vectorY), Math.abs(vectorX)) - minVectorIntersDamage)) * intersDamageMultiplier)) / staticObjectPoint.length;
                    damage += currentDamage;
                    WorldGenerator.setObject(point.x, point.y, StaticWorldObjects.decrementHp(staticObject, (int) (currentDamage + (getResistance(staticObject) / 100) * StaticWorldObjects.getDensity(staticObject)) / staticObjectPoint.length));
                }
                object.currentHp -= damage;

                //TODO: костыль
                if (object.path.toLowerCase().contains("player")) {
                    lastDamage = (int) damage;
                    lastDamageTime = System.currentTimeMillis();
                }
                if (object.currentHp <= 0 && !object.path.toLowerCase().contains("player")) {
                    DynamicObjects.remove(object);
                }
            }
        }
    }

    private static void updatePhys() {
        ArrayList<DynamicWorldObjects> dynamicObj = DynamicObjects;
        updatePlayerMove();
        updatePlayerJump();

        for (DynamicWorldObjects dynamicObject : dynamicObj) {
            if (dynamicObject != null) {
                updateHorizontalSpeed(dynamicObject);

                if (!dynamicObject.isFlying) {
                    updateVerticalSpeed(dynamicObject);
                }
            }
        }
    }

    public static float getTotalResistanceInside(DynamicWorldObjects dynamicObject) {
        int tarX = (int) (dynamicObject.x / 16);
        int tarY = (int) (dynamicObject.y / 16);
        int tarYSize = (int) Math.ceil(TextureLoader.getSize(dynamicObject.path).height() / 16f);
        int tarXSize = (int) Math.ceil(TextureLoader.getSize(dynamicObject.path).width() / 16f);
        float totalResistance = 0;

        for (int xPos = 0; xPos < tarXSize; xPos++) {
            for (int yPos = 0; yPos < tarYSize; yPos++) {
                if (tarX + tarXSize > SizeX || tarY + tarYSize > SizeY || getObject(tarX + xPos, tarY + yPos) == -1 || getObject(tarX + tarXSize, tarY + tarYSize) == -1) {
                    continue;
                }
                if (getResistance(getObject(tarX + xPos, tarY + yPos)) < 100 && getType(getObject(tarX + xPos, tarY + yPos)) == StaticObjectsConst.Types.SOLID) {
                    totalResistance += getResistance(getObject(tarX + xPos, tarY + yPos));
                }
            }
        }
        return totalResistance > 90 ? 90 : totalResistance;
    }


    private static void updateWorldInteractions() {
        updateInventoryInteraction();
        Weapons.updateAmmo();
        updateFactoriesOutput();
    }
}
