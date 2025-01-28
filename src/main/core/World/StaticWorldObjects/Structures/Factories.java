package core.World.StaticWorldObjects.Structures;

import core.EventHandling.Logging.Config;
import core.Global;
import core.World.Textures.TextureDrawing;
import core.ui.Sounds.Sound;
import core.Utils.ArrayUtils;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.InventoryEvents;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.Creatures.Player.Player;
import core.Utils.Color;
import core.World.StaticWorldObjects.StaticBlocksEvents;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.WorldUtils;
import core.g2d.Fill;
import core.math.Point2i;

import java.util.*;

import static core.Global.*;
import static core.World.Creatures.Player.Player.playerSize;
import static core.World.Textures.TextureDrawing.blockSize;
import static core.World.Textures.TextureDrawing.drawText;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

// there is no need to put them manually, they are automatically added to the array if the placed static block is in the factories folder
public class Factories implements StaticBlocksEvents, InventoryEvents {
    public float needEnergy, currentHp, currentEnergy, maxHp, timeSinceBreakdown, x, y;
    public int maxProductionProgress, currentProductionProgress;
    public short id, maxStoredObjects;
    public String path, sound, name;
    public breaking breakingType;
    public Items[] outputObjects, outputStoredObjects, inputObjects, inputStoredObjects, fuel, storedFuel;
    private static long lastMouseClickTime;
    private static final HashMap<String, Factories> factoriesConst = new HashMap<>();
    private static final HashSet<Point2i> factories = new HashSet<>();

    @Override
    public void placeStatic(int cellX, int cellY, short id) {
        // todo костыль года
        if (id != 0 && id != -1 && StaticWorldObjects.getTexture(id) != null && StaticWorldObjects.getFileName(id).toLowerCase().contains("factories")) {
            setFactoryConst(StaticWorldObjects.getFileName(id));
            factories.add(new Point2i(cellX, cellY));
        }
    }

    @Override
    public void destroyStatic(int cellX, int cellY, short id) {
        // todo костыль века
        if (id != 0 && id != -1 && StaticWorldObjects.getTexture(id) != null && StaticWorldObjects.getTexture(id).name().toLowerCase().contains("factories")) {
            factories.remove(new Point2i(cellX, cellY));
        }
    }

    @Override
    public void itemDropped(int blockX, int blockY, Items item) {
        if (world.get(blockX, blockY) != -1) {
            Point2i root = Player.findRoot(blockX, blockY);

            if (root == null) {
                root = new Point2i(blockX, blockY);
            }
            if (factories.contains(root)) {
                Factories factory = factoriesConst.get(StaticWorldObjects.getFileName(world.get(root.x, root.y)));

                Point2i current = Inventory.currentObject;
                int cell = ArrayUtils.findFreeCell(factory.inputStoredObjects);

                if (cell != -1 && current != null) {
                    for (int i = 0; i < factory.inputObjects.length; i++) {
                        if (factory.inputObjects[i].id == item.id) {
                            factory.inputStoredObjects[cell] = Inventory.getCurrent();
                            Inventory.decrementItem(current.x, current.y);

                            return;
                        }
                    }
                }

                cell = ArrayUtils.findFreeCell(factory.storedFuel);

                if (cell != -1 && current != null) {
                    for (int i = 0; i < factory.fuel.length; i++) {
                        if (factory.fuel[i].id == item.id) {
                            factory.storedFuel[cell] = Inventory.getCurrent();
                            Inventory.decrementItem(current.x, current.y);

                            return;
                        }
                    }
                }
            }
        }
    }

    public enum breaking {
        WEAK_SLOW, // slow working
        WEAK_OVERCONSUMPTION, // high consumption
        AVERAGE_STOP, // stop working
        AVERAGE_MISWORKING, // misworking
        CRITICAL // full stop working, need rebuild
    }

    public Factories() {}

    private Factories(int maxProductionProgress, float needEnergy, float maxHp, short maxStoredObjects, short id, String path, String sound, String name, Items[] outputObjects, Items[] inputObjects, Items[] fuel) {
        this.maxProductionProgress = maxProductionProgress;
        this.currentProductionProgress = 0;
        this.needEnergy = needEnergy;
        this.maxHp = maxHp;
        this.id = id;
        this.maxStoredObjects = maxStoredObjects;
        this.outputObjects = outputObjects;
        this.inputObjects = inputObjects;
        this.currentHp = maxHp;
        this.timeSinceBreakdown = 0;
        this.path = path;
        this.sound = sound;
        this.fuel = fuel;
        this.outputStoredObjects = new Items[maxStoredObjects];
        this.inputStoredObjects = new Items[inputObjects.length];
        this.storedFuel = new Items[fuel == null ? 0 : fuel.length];
        this.currentEnergy = 0;
        this.name = name;
    }

    public static void setFactoryConst(String name) {
        String originalName = name;
        name = assets.assetsDir("World/ItemsCharacteristics/" + name + ".properties");

        if (factoriesConst.get(name) == null) {
            byte id = StaticWorldObjects.generateId(name);
            Properties props = Config.getProperties(name);
            int productionSpeed = Integer.parseInt((String) props.get("ProductionSpeed"));
            int needEnergy = Integer.parseInt((String) props.get("NeedEnergy"));
            int maxHp = Integer.parseInt((String) props.get("MaxHp"));
            short maxStoredObjects = Short.parseShort((String) props.get("MaxStoredObjects"));
            String path = (String) props.get("Path");
            String sound = (String) props.get("Sound");
            String factoryName = (String) props.get("Name");
            Items[] outputObjects = transformItems((String) props.get("OutputObjects"));
            Items[] inputObjects = transformItems((String) props.get("InputObjects"));
            Items[] fuel = transformItems((String) props.get("Fuel"));

            factoriesConst.put(originalName, new Factories(productionSpeed, needEnergy, maxHp,
                    maxStoredObjects, (short) ((((byte) maxHp & 0xFF) << 8) | (id & 0xFF)), assets.pathTo(path),
                    sound, factoryName, outputObjects, inputObjects, fuel));
        }
    }

    public static Factories getFactoryConst(String name) {
        return factoriesConst.getOrDefault(name, null);
    }

    private static Items[] transformItems(String items) {
        if (items == null) {
            return null;
        }

        String[] inItems = items.split(",");
        Items[] outItems = new Items[inItems.length];

        for (int i = 0; i < inItems.length; i++) {
            outItems[i] = Items.createItem(inItems[i]);
        }
        return outItems;
    }

    public void breakFactory(breaking breakingType) {
        this.breakingType = breakingType;

        switch (breakingType) {
            case WEAK_SLOW -> maxProductionProgress *= (int) ((Math.random() * 4) + 1);
            case AVERAGE_STOP -> maxProductionProgress = Integer.MAX_VALUE;
            case AVERAGE_MISWORKING -> System.arraycopy(outputObjects, 0, outputObjects, 0, outputObjects.length - 1);
            case WEAK_OVERCONSUMPTION -> needEnergy *= (float) ((Math.random() * 4) + 1);
        }
    }

    public void removeBreakEffect(breaking breakingType) {
        this.breakingType = (breakingType == breaking.CRITICAL ? breaking.CRITICAL : null);

        switch (breakingType) {
            case WEAK_SLOW, AVERAGE_STOP -> maxProductionProgress = Integer.parseInt((String) Config.getProperties(path).get("ProductionSpeed"));
            case AVERAGE_MISWORKING -> outputObjects = transformItems((String) Config.getProperties(path).get("OutputObjects"));
            case WEAK_OVERCONSUMPTION -> needEnergy = Integer.parseInt((String) Config.getProperties(path).get("NeedEnergy"));
        }
    }

    public static void draw() {
        if (System.currentTimeMillis() - input.getLastMouseMoveTimestamp() > 1000) {
            Factories factory = findFactoryUnderMouse();

            if (factory != null) {
                float xMouse = input.mousePos().x;
                float yMouse = input.mousePos().y;
                boolean input = factory.inputStoredObjects != null;
                boolean output = factory.outputStoredObjects != null;
                Color color = Color.fromRgba8888(0, 0, 0, 170);

                if (input && ArrayUtils.findFreeCell(factory.inputStoredObjects) != 0) {
                    int width1 = ArrayUtils.findDistinctObjects(factory.inputStoredObjects) * 54 + playerSize;

                    Fill.rect(xMouse, yMouse, width1, 64, color);
                    TextureDrawing.drawObjects(xMouse, yMouse, factory.inputStoredObjects, atlas.byPath("UI/GUI/buildMenu/factoryIn.png"));
                }
                if (output && ArrayUtils.findFreeCell(factory.outputStoredObjects) != 0) {
                    xMouse += (ArrayUtils.findFreeCell(factory.inputStoredObjects) != 0 ? 78 : 0);
                    int width = ArrayUtils.findDistinctObjects(factory.outputStoredObjects) * 54 + playerSize;

                    Fill.rect(xMouse, yMouse, width, 64, color);
                    TextureDrawing.drawObjects(xMouse, yMouse, factory.outputStoredObjects, atlas.byPath("UI/GUI/buildMenu/factoryOut.png"));
                }
            }
        }
    }

    //todo сделать нормально
    private static boolean mouseDoubleClick() {
        if (Global.input.justClicked(GLFW_MOUSE_BUTTON_LEFT)) {
            lastMouseClickTime = System.currentTimeMillis();
        }

        return System.currentTimeMillis() - lastMouseClickTime <= 60 && !Global.input.clicked(GLFW_MOUSE_BUTTON_LEFT);
    }

    public static void updateFactoriesOutput() {
        Factories factory = findFactoryUnderMouse();

        if (mouseDoubleClick() && factory != null && factory.outputStoredObjects[0] != null) {
            for (int i = 0; i < factory.outputStoredObjects.length; i++) {
                if (factory.outputStoredObjects[i] == null) {
                    break;
                }
                Inventory.createElement(factory.outputStoredObjects[i]);
                factory.outputStoredObjects[i] = null;
            }
        }

        if (factory != null && factory.fuel == null && factory.breakingType != Factories.breaking.CRITICAL && factory.currentEnergy >= factory.needEnergy) {
            int iconY = (int) ((factory.y * blockSize) + blockSize);
            int iconX = (int) ((factory.x * blockSize) + blockSize);

            // todo починить отрисовку из других потоков
//            batch.draw(atlas.byPath("UI/GUI/interactionIcon.png"), iconX, iconY);
//            batch.draw(Window.defaultFont.getGlyph('E'),
//                    (factory.x * blockSize + 16) + blockSize,
//                    (factory.y * blockSize + 12) + blockSize);

            if (input.pressed(GLFW_KEY_E)){
                factory.currentProductionProgress++;
            }

            if (findInput(factory) && factory.currentProductionProgress >= factory.maxProductionProgress) {
                int outputCell = ArrayUtils.findFreeCell(factory.outputObjects);
                for (int i = 0; i < (outputCell == -1 ? factory.outputObjects.length : outputCell); i++) {
                    for (int j = 0; j < factory.outputStoredObjects.length; j++) {
                        int cell = ArrayUtils.findFreeCell(factory.outputStoredObjects);

                        if (cell != -1) {
                            factory.currentProductionProgress = 0;
                            factory.outputStoredObjects[cell] = factory.outputObjects[i];
                            deleteFirstFound(factory.inputObjects, factory.inputStoredObjects);
                            Sound.playSound(factory.sound, Sound.types.EFFECT, false);
                            break;
                        }
                    }
                }
            }
            return;
        }

        for (Point2i factories : factories) {
            factory = factoriesConst.get(StaticWorldObjects.getFileName(world.get(factories.x, factories.y)));

            if (factory != null && factory.fuel != null && factory.breakingType != Factories.breaking.CRITICAL && factory.currentEnergy >= factory.needEnergy) {
                factory.currentProductionProgress++;

                if (findInput(factory) && factory.currentProductionProgress >= factory.maxProductionProgress) {
                    int outputCell = ArrayUtils.findFreeCell(factory.outputObjects);

                    for (int i = 0; i < (outputCell == -1 ? factory.outputObjects.length : outputCell); i++) {
                        for (int j = 0; j < factory.outputStoredObjects.length; j++) {
                            int cell = ArrayUtils.findFreeCell(factory.outputStoredObjects);

                            if (cell != -1) {
                                factory.currentProductionProgress = 0;
                                deleteFirstFound(factory.storedFuel, factory.fuel);
                                deleteFirstFound(factory.inputObjects, factory.inputStoredObjects);
                                factory.outputStoredObjects[cell] = factory.outputObjects[i];
                                Sound.playSound(factory.sound, Sound.types.EFFECT, false);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean findInput(Factories factory) {
        int length = 0;
        Items[] input = factory.inputObjects;

        for (Items items : input) {
            for (int j = 0; j < factory.inputStoredObjects.length; j++) {
                if (items != null && factory.inputStoredObjects[j] != null && items.id == factory.inputStoredObjects[j].id) {
                    length++;
                    break;
                }
            }
        }

        return length == input.length;
    }

    private static void deleteFirstFound(Items[] items, Items[] target) {
        for (Items item : items) {
            if (item == null) {
                continue;
            }
            for (int j = 0; j < target.length; j++) {
                if (target[j] != null && item.id == target[j].id) {
                    target[j] = null;
                    break;
                }
            }
        }
    }

    private static Factories findFactoryUnderMouse() {
        Point2i blockUnderMouse = WorldUtils.getBlockUnderMousePoint();

        if (world.get(blockUnderMouse.x, blockUnderMouse.y) != -1) {
            Point2i root = Player.findRoot(blockUnderMouse.x, blockUnderMouse.y);

            if (root == null) {
                root = new Point2i(blockUnderMouse.x, blockUnderMouse.y);
            }
            if (factories.contains(root)) {
                return factoriesConst.get(StaticWorldObjects.getFileName(world.get(root.x, root.y)));
            }
        }
        return null;
    }
}
