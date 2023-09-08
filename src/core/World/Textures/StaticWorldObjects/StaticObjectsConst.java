package core.World.Textures.StaticWorldObjects;

import core.EventHandling.Logging.Config;
import java.util.HashMap;
import static core.Window.defPath;

public class StaticObjectsConst {
    private static final HashMap<Integer, StaticObjectsConst> constants = new HashMap<>();
    public float maxHp, density, lightTransmission, resistance;
    public String path, originalFileName, objectName;
    public Types type;

    public enum Types {
        GAS,
        LIQUID,
        SOLID,
        PLASMA
    }

    private StaticObjectsConst(float maxHp, float density, float resistance, float lightTransmission, String path, String objectName, String originalFileName, Types type) {
        this.maxHp = maxHp;
        this.density = density;
        this.path = path;
        this.objectName = objectName;
        this.originalFileName = originalFileName;
        this.type = type;
        this.lightTransmission = lightTransmission;
        this.resistance = resistance;
    }

    public static void setConst(String name, int id) {
        if (constants.get(id) == null) {
            String originalName = name;
            name = (defPath + "\\src\\assets\\World\\ItemsCharacteristics\\Blocks\\" + name + ".properties");

            float maxHp = Float.parseFloat((String) Config.getProperties(name).get("MaxHp"));
            float density = Float.parseFloat((String) Config.getProperties(name).get("Density"));
            float resistance = Float.parseFloat((String) Config.getProperties(name).get("Resistance"));
            float lightTransmission = Float.parseFloat((String) Config.getProperties(name).get("LightTransmission"));
            String path = (String) Config.getProperties(name).get("Path");
            String enumType = (String) Config.getProperties(name).get("Type");
            String objectName = (String) Config.getProperties(name).get("Name");

            if (path == null || path.equals("null")) {
                constants.put(id, new StaticObjectsConst(maxHp, density, resistance, lightTransmission, path, objectName, originalName, Enum.valueOf(Types.class, enumType.toUpperCase())));
            } else {
                constants.put(id, new StaticObjectsConst(maxHp, density, resistance, lightTransmission, defPath + path, objectName, originalName, Enum.valueOf(Types.class, enumType.toUpperCase())));
            }
        }
    }

    public static void setDestroyed() {
        constants.put(0, new StaticObjectsConst(0, 0, 0, 100, null, "Destroyed", null, Types.GAS));
    }

    public static StaticObjectsConst getConst(int id) {
        return constants.get(id);
    }

    public static boolean checkIsHere(int id) {
        return constants.get(id) != null;
    }
}
