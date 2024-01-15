package core.World.StaticWorldObjects.Structures;

import core.EventHandling.Logging.Config;
import core.UI.Sounds.Sound;
import core.Utils.ArrayUtils;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.Creatures.Player.Player;
import core.Utils.SimpleColor;
import core.World.StaticWorldObjects.StaticBlocksEvents;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.WorldGenerator;
import core.World.WorldUtils;
import core.g2d.Atlas;
import core.g2d.Fill;
import core.math.Point2i;

import java.util.*;

import static core.Global.*;
import static core.Utils.ArrayUtils.findEqualsObjects;
import static core.World.Creatures.Player.Player.playerSize;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

//there is no need to put them manually, they are automatically added to the array if the placed static block is in the factories folder
public class Factories implements StaticBlocksEvents {
    public float productionSpeed, needEnergy, currentHp, currentEnergy, maxHp, timeSinceBreakdown, x, y;
    public long lastProductionTime;
    public short id, maxStoredObjects;
    public String path, sound, name;
    public breaking breakingType;
    public Items[] outputObjects, outputStoredObjects, inputObjects, inputStoredObjects;
    private static boolean mouseGrabbedItem = false;
    private static final HashMap<String, Factories> factoriesConst = new HashMap<>();
    private static final HashSet<Point2i> factories = new HashSet<>();

    @Override
    public void placeStatic(int cellX, int cellY, short id) {
        if (id != 0 && id != -1 && StaticWorldObjects.getTexture(id) != null && StaticWorldObjects.getTexture(id).name().toLowerCase().contains("factory")) {
            setFactoryConst(StaticWorldObjects.getFileName(id));
            factories.add(new Point2i(cellX, cellY));
        }
    }

    @Override
    public void destroyStatic(int cellX, int cellY, short id) {
        if (id != 0 && id != -1 && StaticWorldObjects.getTexture(id) != null && StaticWorldObjects.getTexture(id).name().toLowerCase().contains("factory")) {
            factories.remove(new Point2i(cellX, cellY));
        }
    }

    public enum breaking {
        WEAK_SLOW, //slow working
        WEAK_OVERCONSUMPTION, //high consumption
        AVERAGE_STOP, //stop working
        AVERAGE_MISWORKING, //misworking
        CRITICAL //full stop working, need rebuild
    }

    public Factories() {}

    private Factories(float productionSpeed, float needEnergy, float maxHp, short maxStoredObjects, short id, String path, String sound, String name, Items[] outputObjects, Items[] inputObjects) {
        this.productionSpeed = productionSpeed;
        this.lastProductionTime = System.currentTimeMillis();
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
        this.outputStoredObjects = new Items[maxStoredObjects];
        this.inputStoredObjects = new Items[inputObjects.length];
        this.currentEnergy = 0;
        this.name = name;
    }

    public static void setFactoryConst(String name) {
        String originalName = name;
        name = assets.assetsDir("World/ItemsCharacteristics/BuildMenu/Factories/" + name + ".properties");

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

            factoriesConst.put(originalName, new Factories(productionSpeed, needEnergy, maxHp,
                    maxStoredObjects, (short) ((((byte) maxHp & 0xFF) << 8) | (id & 0xFF)), assets.pathTo(path),
                    sound, factoryName, outputObjects, inputObjects));
            Structures.bindStructure("\\BuildMenu\\Factories\\" + name);
        }
    }

    public static Factories getFactoryConst(String name) {
        return factoriesConst.getOrDefault(name, null);
    }

    private static Items[] transformItems(String items) {
        String[] inItems = items.split(", ");
        Items[] outItems = new Items[inItems.length];

        for (int i = 0; i < inItems.length; i++) {
            outItems[i] = Items.createPlaceable(StaticWorldObjects.createStatic(inItems[i]));
        }
        return outItems;
    }

    public void breakFactory(breaking breakingType) {
        this.breakingType = breakingType;

        switch (breakingType) {
            case WEAK_SLOW -> productionSpeed *= (Math.random() * 4) + 1;
            case AVERAGE_STOP -> productionSpeed = Float.MAX_VALUE;
            case AVERAGE_MISWORKING -> System.arraycopy(outputObjects, 0, outputObjects, 0, outputObjects.length - 1);
            case WEAK_OVERCONSUMPTION -> needEnergy *= (Math.random() * 4) + 1;
        }
    }

    public void removeBreakEffect(breaking breakingType) {
        this.breakingType = (breakingType == breaking.CRITICAL ? breaking.CRITICAL : null);

        switch (breakingType) {
            case WEAK_SLOW, AVERAGE_STOP -> productionSpeed = Integer.parseInt((String) Config.getProperties(path).get("ProductionSpeed"));
            case AVERAGE_MISWORKING -> outputObjects = transformItems((String) Config.getProperties(path).get("OutputObjects"));
            case WEAK_OVERCONSUMPTION -> needEnergy = Integer.parseInt((String) Config.getProperties(path).get("NeedEnergy"));
        }
    }

    public static void update() {
        if (System.currentTimeMillis() - input.getLastMouseMoveTimestamp() > 1000) {
            Factories factory = findFactoryUnderMouse();

            if (factory != null) {
                float xMouse = input.mousePos().x;
                float yMouse = input.mousePos().y;
                boolean input = factory.inputStoredObjects != null;
                boolean output = factory.outputStoredObjects != null;

                if (input && ArrayUtils.findFreeCell(factory.inputStoredObjects) != 0) {
                    int width1 = ArrayUtils.findDistinctObjects(factory.inputStoredObjects) * 54 + playerSize;
                    SimpleColor color = SimpleColor.fromRGBA(40, 40, 40, 240);
                    Fill.rect(xMouse, yMouse, width1, 64, color);
                    drawObjects(xMouse, yMouse, factory.inputStoredObjects, atlas.byPath("UI/GUI/buildMenu/factoryIn.png"));
                }
                if (output && ArrayUtils.findFreeCell(factory.outputStoredObjects) != 0) {
                    xMouse += (ArrayUtils.findFreeCell(factory.inputStoredObjects) != 0 ? 78 : 0);

                    int width1 = ArrayUtils.findDistinctObjects(factory.outputStoredObjects) * 54 + playerSize;
                    SimpleColor color = SimpleColor.fromRGBA(40, 40, 40, 240);
                    Fill.rect(xMouse, yMouse, width1, 64, color);
                    drawObjects(xMouse, yMouse, factory.outputStoredObjects, atlas.byPath("UI/GUI/buildMenu/factoryOut.png"));
                }
            }
        }

        if (input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && Inventory.currentObjectType != null) {
            mouseGrabbedItem = true;
        }
        if (!input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && mouseGrabbedItem) {
            mouseGrabbedItem = false;
            Factories factoryUM = findFactoryUnderMouse();

            if (factoryUM != null) {
                int cell = ArrayUtils.findFreeCell(factoryUM.inputStoredObjects);
                Point2i current = Inventory.currentObject;

                if (cell != -1 && current != null) {
                    factoryUM.inputStoredObjects[cell] = Inventory.getCurrent();
                    Inventory.decrementItem(current.x, current.y);
                }
            }
        }
    }

    public static void drawObjects(float x, float y, Items[] items, Atlas.Region iconRegion) {
        if (items != null && ArrayUtils.findFreeCell(items) != 0) {
            batch.draw(iconRegion, x, y + 16);

            for (int i = 0; i < ArrayUtils.findDistinctObjects(items); i++) {
                if (items[i] != null) {
                    Inventory.drawInventoryItem((x + (i * 54)) + playerSize, y + 10, findEqualsObjects(items, items[i]), items[i].texture);
                }
            }
        }
    }

    public static void updateFactoriesOutput() {
        for (Point2i factories : factories) {
            Factories factory = factoriesConst.get(StaticWorldObjects.getFileName(WorldGenerator.getObject(factories.x, factories.y)));

            if (factory != null && factory.breakingType != Factories.breaking.CRITICAL && factory.currentEnergy == factory.needEnergy  && System.currentTimeMillis() - factory.lastProductionTime >= factory.productionSpeed && findInput(factory)) {
                int outputCell = ArrayUtils.findFreeCell(factory.outputObjects);

                for (int i = 0; i < (outputCell == -1 ? factory.outputObjects.length : outputCell); i++) {
                    for (int j = 0; j < factory.outputStoredObjects.length; j++) {
                        int cell = ArrayUtils.findFreeCell(factory.outputStoredObjects);

                        if (cell != -1) {
                            factory.inputStoredObjects = deleteAnyMatchInput(factory);
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

    private static Items[] deleteAnyMatchInput(Factories factory) {
        Items[] input = factory.inputStoredObjects;

        for (int i = 0; i < factory.inputObjects.length; i++) {
            for (int j = 0; j < input.length; j++) {
                if (input[j] != null && factory.inputObjects[i] != null && input[j].id == factory.inputObjects[i].id) {
                    input[j] = null;
                    break;
                }
            }
        }
        return input;
    }

    private static Factories findFactoryUnderMouse() {
        Point2i blockUnderMouse = WorldUtils.getBlockUnderMousePoint();

        if (WorldGenerator.getObject(blockUnderMouse.x, blockUnderMouse.y) != -1) {
            Point2i root = Player.findRoot(blockUnderMouse.x, blockUnderMouse.y);

            if (root != null && factories.contains(root)) {
                return factoriesConst.get(StaticWorldObjects.getFileName(WorldGenerator.getObject(root.x, root.y)));
            }
        }
        return null;
    }
}
