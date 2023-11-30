package core.World.StaticWorldObjects;

import core.EventHandling.Logging.Config;
import core.Window;
import java.util.HashMap;
import java.util.Properties;

import static core.Window.*;

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
            name = assetsDir("World/ItemsCharacteristics/" + name + ".properties");

            Properties props = Config.getProperties(name);
            boolean hasMotherBlock = Boolean.parseBoolean((String) props.getOrDefault("HasMotherBlock", "false"));
            float density = Float.parseFloat((String) props.getOrDefault("Density", "1"));
            float resistance = Float.parseFloat((String) props.getOrDefault("Resistance", "100"));
            int lightTransmission = Integer.parseInt((String) props.getOrDefault("LightTransmission", "100"));
            int maxHp = Integer.parseInt((String) props.getOrDefault("MaxHp", "100"));
            String path = Window.assetsDir((String) props.getOrDefault("Path", "\\World\\textureNotFound.png"));
            String enumType = (String) props.getOrDefault("Type", Types.SOLID.toString());
            String objectName = (String) props.getOrDefault("Name", "notFound");

            constants.put(id, new StaticObjectsConst(hasMotherBlock, maxHp, density, resistance, lightTransmission, path, objectName, originalName, optionalTiles, Types.valueOf(enumType.toUpperCase())));
        }
    }

    private static String getStorageFolder(String path) {
        return path.substring(path.lastIndexOf("/", path.lastIndexOf("/") - 1) + 1, path.lastIndexOf("/"));
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
