package core.World.StaticWorldObjects;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import core.EventHandling.Logging.Config;
import core.Global;
import core.World.StaticWorldObjects.Structures.Structures;
import core.g2d.Atlas;

import java.util.concurrent.ConcurrentHashMap;

public class StaticObjectsConst implements Cloneable {
    private static final ConcurrentHashMap<Byte, StaticObjectsConst> constants = new ConcurrentHashMap<>();
    public short[][] optionalTiles;

    //гсон сам заменяет параметр если находит его
    //а если нет, то применяется дефолтный
    @SerializedName("Max-hp")
    public float maxHp = 100f;

    @SerializedName("Density")
    public float density = 1f;

    @SerializedName("Resistance")
    public float resistance = 100f;

    @SerializedName("Light-transmission")
    public int lightTransmission = 100;

    @SerializedName("Has-mother-block")
    public boolean hasMotherBlock = false;

    @SerializedName("Type")
    public Types type = Types.SOLID;

    @SerializedName("Name")
    public String objectName = "notFound";

    @SerializedName("Texture")
    private String texturePath;

    public String originalFileName;
    public Atlas.Region texture;
    public Runnable onInteraction;

    public enum Types {
        GAS,
        LIQUID,
        SOLID,
        PLASMA
    }

    @Override
    public StaticObjectsConst clone() {
        try {
            return (StaticObjectsConst) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    //да тут можно красивый билдер сделать но зачем оно же приват
    private StaticObjectsConst(boolean hasMotherBlock, float maxHp, float density, float resistance, int lightTransmission, Atlas.Region texture, String objectName, String originalFileName, short[][] optionalTiles, Types type) {
        this.hasMotherBlock = hasMotherBlock;
        this.maxHp = maxHp;
        this.density = density;
        this.texture = texture;
        this.objectName = objectName;
        this.originalFileName = originalFileName;
        this.type = type;
        this.lightTransmission = lightTransmission;
        this.optionalTiles = optionalTiles;
        this.resistance = resistance;
    }

    public static void setConst(StaticObjectsConst staticConst, byte id) {
        constants.put(id, staticConst);
    }

    public static void setConst(String name, byte id, short[][] optionalTiles) {
        if (!constants.containsKey(id)) {
            StaticObjectsConst staticConst = createConst("World/ItemsCharacteristics/" + name + ".json", id);
            staticConst.optionalTiles = optionalTiles;
            staticConst.originalFileName = name;

            constants.put(id, staticConst);

            Structures.bindStructure(name, id);
        }
    }

    public static StaticObjectsConst createConst(String path, byte id) {
        if (!constants.containsKey(id)) {
            JsonObject asJson = Global.assets.jsonReader(path);
            StaticObjectsConst obj = new Gson().fromJson(asJson, StaticObjectsConst.class);
            obj.originalFileName = path;

            if (asJson.has("Texture")) {
                obj.texture = Global.atlas.byPath(asJson.get("Texture").getAsString());
            }

            constants.put(id, obj);
            return obj;
        }
        return constants.get(id);
    }

    public static void setConst(String name, byte id) {
        setConst(name, id, null);
    }

    public static void setDestroyed() {
        constants.put((byte) 0, new StaticObjectsConst(false, 0, 0, 0, 100, null, "Destroyed", null, null, Types.GAS));
    }

    public static StaticObjectsConst getConst(byte id) {
        return constants.get(id);
    }
}
