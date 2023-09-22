package core.World.Creatures.Player.Inventory.Items.Placeable;

import core.EventHandling.EventHandler;
import core.UI.Sounds.Sound;
import core.World.ArrayUtils;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.Creatures.Player.Player;
import core.World.Textures.SimpleColor;
import core.World.Textures.TextureDrawing;
import core.World.Textures.TextureLoader;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import static core.Window.defPath;
import static core.World.ArrayUtils.findEqualsObjects;

public class Factories {
    public float lastProductionSpeed, productionSpeed, needEnergy, currentHp, currentEnergy, maxHp, timeSinceBreakdown, x, y;
    public long lastProductionTime;
    public int id, maxStoredObjects;
    public String path, sound, name;
    public breaking breakingType;
    public Items[] outputObjects, outputStoredObjects, missOutput, inputObjects, inputStoredObjects, missInput;
    private static boolean mouseGrabbedItem = false;
    public static ArrayList<Factories> factories = new ArrayList<>();

    public enum breaking {
        WEAK_SLOW, //медленная работа
        WEAK_OVERCONSUMPTION, //большое потребление
        AVERAGE_STOP, //остановка работы
        AVERAGE_MISWORKING, //неправильная выработка
        CRITICAL //полная остановка работы, нужно перестроить
    }

    public Factories(float productionSpeed, float needEnergy, float maxHp, int maxStoredObjects, Items[] outputObjects, Items[] inputObjects, String path, String sound, String name) {
        this.productionSpeed = productionSpeed;
        this.lastProductionTime = System.currentTimeMillis();
        this.needEnergy = needEnergy;
        this.maxHp = maxHp;
        this.id = name.hashCode();
        this.maxStoredObjects = maxStoredObjects;
        this.outputObjects = outputObjects;
        this.inputObjects = inputObjects;
        this.currentHp = maxHp;
        this.timeSinceBreakdown = 0;
        this.path = path;
        this.sound = sound;
        this.lastProductionSpeed = productionSpeed;
        this.outputStoredObjects = new Items[maxStoredObjects];
        this.inputStoredObjects = new Items[inputObjects.length];
        this.currentEnergy = 0;
        this.name = name;
    }

    public void breakFactory(breaking breakingType) {
        this.breakingType = breakingType;

        switch (breakingType) {
            case WEAK_SLOW -> productionSpeed /= 1.1f;
            case AVERAGE_STOP -> {
                lastProductionSpeed = productionSpeed;
                productionSpeed = Float.MAX_VALUE;
            }
            case AVERAGE_MISWORKING -> {
                missOutput = outputObjects;
                System.arraycopy(missOutput, 0, outputObjects, 0, missOutput.length - 1);
            }
            case WEAK_OVERCONSUMPTION -> {
                missInput = inputObjects;
                System.arraycopy(missInput, 0, inputObjects, 0, missInput.length + 1);
                inputObjects[inputObjects.length] = inputObjects[(int) (Math.random() * inputObjects.length)];
            }
        }
    }

    public void removeBreakEffect(breaking breakingType) {
        this.breakingType = (breakingType == breaking.CRITICAL ? breaking.CRITICAL : null);

        switch (breakingType) {
            case WEAK_SLOW -> productionSpeed *= 1.1f;
            case AVERAGE_STOP -> productionSpeed = lastProductionSpeed;
            case AVERAGE_MISWORKING -> {
                outputObjects = missOutput;
                missOutput = null;
            }
            case WEAK_OVERCONSUMPTION -> {
                inputObjects = missInput;
                missInput = null;
            }
        }
    }

    public static void update() {
        for (Factories factory : factories) {
            if (factory != null) {
                float x = factory.x;
                float y = factory.y;

                if (TextureDrawing.isOnCamera(x, y, TextureLoader.getSize(factory.path).width, TextureLoader.getSize(factory.path).height)) {
                    TextureDrawing.drawTexture(factory.path, x, y, 3, new SimpleColor(255, 255, 255, 255), false, false);
                }
            }
        }
        Factories factory = findFactoryUnderMouse();

        if (System.currentTimeMillis() - EventHandler.lastMouseMovedTime > 1000 && factory != null) {
            float xMouse = EventHandler.getMousePos().x;
            float yMouse = EventHandler.getMousePos().y;
            boolean input = factory.inputStoredObjects != null;
            boolean output = factory.outputStoredObjects != null;

            if (input && ArrayUtils.findFreeCell(factory.inputStoredObjects) != 0) {
                TextureDrawing.drawRectangle((int) xMouse, (int) yMouse, ArrayUtils.findDistinctObjects(factory.inputStoredObjects) * 54 + 24, 64, new SimpleColor(40, 40, 40, 240));
                drawObjects(xMouse, yMouse, factory.inputStoredObjects, defPath + "\\src\\assets\\UI\\GUI\\buildMenu\\factoryIn.png");
            }
            if (output && ArrayUtils.findFreeCell(factory.outputStoredObjects) != 0) {
                xMouse += (ArrayUtils.findFreeCell(factory.inputStoredObjects) != 0 ? 78 : 0);

                TextureDrawing.drawRectangle((int) xMouse, (int) yMouse, ArrayUtils.findDistinctObjects(factory.outputStoredObjects) * 54 + 24, 64, new SimpleColor(40, 40, 40, 240));
                drawObjects(xMouse, yMouse, factory.outputStoredObjects, defPath + "\\src\\assets\\UI\\GUI\\buildMenu\\factoryOut.png");
            }
        }

        if (EventHandler.getMousePress() && Inventory.currentObjectType != null) {
            mouseGrabbedItem = true;
        }
        if (!EventHandler.getMousePress() && mouseGrabbedItem) {
            mouseGrabbedItem = false;
            Factories factoryUM = findFactoryUnderMouse();

            if (factoryUM != null) {
                int cell = ArrayUtils.findFreeCell(factoryUM.inputStoredObjects);
                Point current = Inventory.currentObject;

                if (cell != -1 && current != null) {
                    factoryUM.inputStoredObjects[cell] = Inventory.inventoryObjects[current.x][current.y];
                    Inventory.decrementItem(current.x, current.y);
                }
            }
        }
    }

    public static void drawObjects(float x, float y, Items[] items, String iconPath) {
        if (items != null && ArrayUtils.findFreeCell(items) != 0) {
            TextureDrawing.drawTexture(iconPath, x, y + 16, 1, true);

            for (int i = 0; i < ArrayUtils.findDistinctObjects(items); i++) {
                if (items[i] != null) {
                    Inventory.drawInventoryItem((x + (i * 54)) + 24, y + 10, findEqualsObjects(items, items[i]), items[i].path);
                }
            }
        }
    }


    public static void updateFactoriesOutput() {
        for (Factories factory : factories) {
            if (factory != null && factory.breakingType != Factories.breaking.CRITICAL && factory.currentEnergy == factory.needEnergy && Arrays.stream(factory.inputObjects).filter(Objects::nonNull).allMatch(obj -> Arrays.stream(factory.inputStoredObjects).anyMatch(storedObj -> storedObj != null && storedObj.id == obj.id)) && System.currentTimeMillis() - factory.lastProductionTime >= factory.productionSpeed) {
                int outputCell = ArrayUtils.findFreeCell(factory.outputObjects);

                for (int i = 0; i < (outputCell == -1 ? factory.outputObjects.length : outputCell); i++) {
                    for (int j = 0; j < factory.outputStoredObjects.length; j++) {
                        int cell = ArrayUtils.findFreeCell(factory.outputStoredObjects);

                        if (cell != -1) {
                            factory.inputStoredObjects = new Items[factory.inputObjects.length];
                            factory.lastProductionTime = System.currentTimeMillis();
                            factory.outputStoredObjects[cell] = factory.outputObjects[i];
                            Sound.SoundPlay(factory.sound, Sound.types.EFFECT, true);
                            break;
                        }
                    }
                }
            }
        }
    }

    public static Factories findFactoryUnderMouse() {
        for (Factories factory : factories) {
            float x = factory.x;
            float y = factory.y;

            if (Player.getWorldMousePoint().x > x && Player.getWorldMousePoint().y > y && Player.getWorldMousePoint().x < x + TextureLoader.getSize(factory.path).width && Player.getWorldMousePoint().y < y + TextureLoader.getSize(factory.path).height) {
                return factory;
            }
        }
        return null;
    }
}
