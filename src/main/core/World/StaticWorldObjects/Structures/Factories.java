package core.World.StaticWorldObjects.Structures;

import core.EventHandling.Logging.Config;
import core.Global;
import core.UI.Sounds.Sound;
import core.Utils.ArrayUtils;
import core.World.Creatures.Player.Inventory.Inventory;
import core.World.Creatures.Player.Inventory.Items.Items;
import core.World.Creatures.Player.Player;
import core.Utils.SimpleColor;
import core.World.StaticWorldObjects.StaticBlocksEvents;
import core.World.StaticWorldObjects.StaticObjectsConst;
import core.World.StaticWorldObjects.StaticWorldObjects;
import core.World.Textures.TextureDrawing;
import core.World.WorldGenerator;
import java.awt.Point;
import java.util.*;
import static core.Utils.ArrayUtils.findEqualsObjects;
import static core.Window.*;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

public class Factories implements StaticBlocksEvents {
    public float productionSpeed, needEnergy, currentHp, currentEnergy, maxHp, timeSinceBreakdown, x, y;
    public long lastProductionTime;
    public short id, maxStoredObjects;
    public String path, sound, name;
    public breaking breakingType;
    public Items[] outputObjects, outputStoredObjects, inputObjects, inputStoredObjects;
    private static boolean mouseGrabbedItem = false;
    private static final HashMap<String, Factories> factoriesConst = new HashMap<>();
    private static final HashSet<Point> factories = new HashSet<>();

    @Override
    public void placeStatic(int cellX, int cellY, short id) {
        if (id != 0 && id != -1 && StaticWorldObjects.getPath(id) != null && StaticWorldObjects.getPath(id).toLowerCase().contains("factory")) {
            createFactory(StaticWorldObjects.getFileName(id), StaticObjectsConst.getConst(StaticWorldObjects.getId(id)).optionalTiles);
            factories.add(new Point(cellX, cellY));
        }
    }

    @Override
    public void destroyStatic(int cellX, int cellY, short id) {
        if (id != 0 && id != -1 && StaticWorldObjects.getPath(id) != null && StaticWorldObjects.getPath(id).toLowerCase().contains("factory")) {
            factories.remove(new Point(cellX, cellY));
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

    public static void createFactory(String name, short[][] tiles) {
        String originalName = name;
        name = assetsDir("World/ItemsCharacteristics/Factories/" + name + ".properties");

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
                    maxStoredObjects, (short) ((((byte) maxHp & 0xFF) << 8) | (id & 0xFF)), pathTo(path),
                    sound, factoryName, outputObjects, inputObjects));
            StaticObjectsConst.setConst(name, id, tiles);
        }
    }

    private static Items[] transformItems(String items) {
        String[] inItems = items.split(", ");
        Items[] outItems = new Items[inItems.length];

        for (int i = 0; i < inItems.length; i++) {
            outItems[i] = new Items(StaticWorldObjects.createStatic(inItems[i]), "");
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
        if (System.currentTimeMillis() - Global.input.getLastMouseMoveTimestamp() > 1000) {
            Factories factory = findFactoryUnderMouse();

            if (factory != null) {
                float xMouse = Global.input.mousePos().x;
                float yMouse = Global.input.mousePos().y;
                boolean input = factory.inputStoredObjects != null;
                boolean output = factory.outputStoredObjects != null;

                if (input && ArrayUtils.findFreeCell(factory.inputStoredObjects) != 0) {
                    TextureDrawing.drawRectangle((int) xMouse, (int) yMouse, ArrayUtils.findDistinctObjects(factory.inputStoredObjects) * 54 + 24, 64, new SimpleColor(40, 40, 40, 240));
                    drawObjects(xMouse, yMouse, factory.inputStoredObjects, assetsDir("UI/GUI/buildMenu/factoryIn.png"));
                }
                if (output && ArrayUtils.findFreeCell(factory.outputStoredObjects) != 0) {
                    xMouse += (ArrayUtils.findFreeCell(factory.inputStoredObjects) != 0 ? 78 : 0);

                    TextureDrawing.drawRectangle((int) xMouse, (int) yMouse, ArrayUtils.findDistinctObjects(factory.outputStoredObjects) * 54 + 24, 64, new SimpleColor(40, 40, 40, 240));
                    drawObjects(xMouse, yMouse, factory.outputStoredObjects, assetsDir("UI/GUI/buildMenu/factoryOut.png"));
                }
            }
        }

        if (Global.input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && Inventory.currentObjectType != null) {
            mouseGrabbedItem = true;
        }
        if (!Global.input.justClicked(GLFW_MOUSE_BUTTON_LEFT) && mouseGrabbedItem) {
            mouseGrabbedItem = false;
            Factories factoryUM = findFactoryUnderMouse();

            if (factoryUM != null) {
                int cell = ArrayUtils.findFreeCell(factoryUM.inputStoredObjects);
                Point current = Inventory.currentObject;

                if (cell != -1 && current != null) {
                    factoryUM.inputStoredObjects[cell] = Inventory.getCurrent();
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
        for (Point factories : factories) {
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
        Point blockUnderMouse = Player.getBlockUnderMousePoint();

        if (WorldGenerator.getObject(blockUnderMouse.x, blockUnderMouse.y) != -1) {
            Point root = Player.findRoot(blockUnderMouse.x, blockUnderMouse.y);

            if (root != null && factories.contains(root)) {
                return factoriesConst.get(StaticWorldObjects.getFileName(WorldGenerator.getObject(root.x, root.y)));
            }
        }
        return null;

//        for (Point factories : factories) {
//            int x = factories.x;
//            int y = factories.y;
//
//            Factories factory = factoriesConst.get(StaticWorldObjects.getFileName(WorldGenerator.getObject(x, y)));
//
//            if (factory != null && Player.getWorldMousePoint().x > (x * 16) && Player.getWorldMousePoint().y > (y * 16) && Player.getWorldMousePoint().x < (x * 16) + TextureLoader.getSize(factory.path).width && Player.getWorldMousePoint().y < (y * 16) + TextureLoader.getSize(factory.path).height()) {
//                return factory;
//            }
//        }
//        return null;
    }
}
