package core.World.StaticWorldObjects;

import core.EventHandling.Logging.Config;
import java.io.File;
import java.util.HashMap;
import static core.Window.defPath;

public class StaticObjectsConst {
    private static final HashMap<Byte, StaticObjectsConst> constants = new HashMap<>();
    public short[][] optionalTiles;
    public float maxHp, density, resistance;
    public int lightTransmission;
    public boolean hasMotherBlock;
    public String path, originalFileName, objectName;
    public Types type;

    public enum Types {
        GAS,
        LIQUID,
        SOLID,
        PLASMA
    }

    private StaticObjectsConst(boolean hasMotherBlock, float maxHp, float density, float resistance, int lightTransmission, String path, String objectName, String originalFileName, Types type) {
        this.hasMotherBlock = hasMotherBlock;
        this.maxHp = maxHp;
        this.density = density;
        this.path = path;
        this.objectName = objectName;
        this.originalFileName = originalFileName;
        this.type = type;
        this.lightTransmission = lightTransmission;
        this.resistance = resistance;
        this.optionalTiles = null;
    }

    private StaticObjectsConst(boolean hasMotherBlock, float maxHp, float density, float resistance, int lightTransmission, String path, String objectName, String originalFileName, short[][] optionalTiles, Types type) {
        this.hasMotherBlock = hasMotherBlock;
        this.maxHp = maxHp;
        this.density = density;
        this.path = path;
        this.objectName = objectName;
        this.originalFileName = originalFileName;
        this.type = type;
        this.lightTransmission = lightTransmission;
        this.optionalTiles = optionalTiles;
        this.resistance = resistance;
    }

    public static void setConst(String name, byte id, short[][] optionalTiles) {
        if (constants.get(id) == null) {
            String originalName = name;
            name = defPath + "\\src\\assets\\World\\ItemsCharacteristics\\" + name + ".properties";

            if (getStorageFolder(name).toLowerCase().contains("blocks")) {
                boolean hasMotherBlock = Config.getProperties(name).get("HasMotherBlock") != null && Config.getProperties(name).get("HasMotherBlock").equals("true");
                float density = Float.parseFloat((String) Config.getProperties(name).get("Density"));
                float resistance = Float.parseFloat((String) Config.getProperties(name).get("Resistance"));
                int lightTransmission = Integer.parseInt((String) Config.getProperties(name).get("LightTransmission"));
                int maxHp = Integer.parseInt((String) Config.getProperties(name).get("MaxHp"));
                String path = (String) Config.getProperties(name).get("Path");
                String enumType = (String) Config.getProperties(name).get("Type");
                String objectName = (String) Config.getProperties(name).get("Name");

                constants.put(id, new StaticObjectsConst(hasMotherBlock, maxHp, density, resistance, lightTransmission, path == null ? null : defPath + path, objectName, originalName, optionalTiles, Enum.valueOf(Types.class, enumType.toUpperCase())));
            } else {
                setConstStructures(name, id, optionalTiles);
            }
        }
    }

    public static void setConstStructures(String path, byte id, short[][] optionalTiles) {
        if (constants.get(id) == null) {
            boolean hasMotherBlock = Config.getProperties(path).get("HasMotherBlock") != null && Config.getProperties(path).get("HasMotherBlock").equals("true");
            float density = 1;
            float resistance = 0;
            int lightTransmission = 100;
            int maxHp = Integer.parseInt((String) Config.getProperties(path).get("MaxHp"));
            String texturePath = (String) Config.getProperties(path).get("Path");
            String objectName = (String) Config.getProperties(path).get("Name");
            String fileName = new File(path).getName();
            Types type = Types.SOLID;

            constants.put(id, new StaticObjectsConst(hasMotherBlock, maxHp, density, resistance, lightTransmission, defPath + texturePath, objectName, fileName.substring(0, fileName.indexOf(".")), optionalTiles, type));
        }
    }

    private static String getStorageFolder(String path) {
        return path.substring(path.lastIndexOf("\\", path.lastIndexOf("\\") - 1) + 1, path.lastIndexOf("\\"));
    }

    public static void setConst(String name, byte id) {
        setConst(name, id, null);
    }

    public static void setDestroyed() {
        constants.put((byte) 0, new StaticObjectsConst(false, 0, 0, 0, 100, null, "Destroyed", null, Types.GAS));
    }

    public static StaticObjectsConst getConst(byte id) {
        return constants.get(id);
    }

    public static boolean checkIsHere(byte id) {
        return constants.get(id) != null;
    }
}
