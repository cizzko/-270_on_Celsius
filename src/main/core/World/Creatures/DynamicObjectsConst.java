package core.World.Creatures;

import core.EventHandling.Logging.Config;
import core.Window;
import java.util.HashMap;
import java.util.Properties;

public class DynamicObjectsConst {
    private static final HashMap<Byte, DynamicObjectsConst> consts = new HashMap<>();
    public boolean isFlying, oneoffAnimation;
    public int framesCount, animSpeed;
    public float weight, maxHp;
    public String path;

    private DynamicObjectsConst(boolean isFlying, boolean oneoffAnimation, int framesCount, int animSpeed, float weight, float maxHp, String path) {
        this.isFlying = isFlying;
        this.oneoffAnimation = oneoffAnimation;
        this.framesCount = framesCount;
        this.animSpeed = animSpeed;
        this.weight = weight;
        this.maxHp = maxHp;
        this.path = path;
    }

    public static void bindDynamic(String name, byte id) {
        if (consts.get(id) == null) {
            Properties prop = Config.getProperties(Window.assetsDir("\\World\\CreaturesCharacteristics\\" + name + ".properties"));
            boolean isFlying = Boolean.parseBoolean((String) prop.getOrDefault("IsFlying", "false"));
            boolean oneoffAnimation = Boolean.parseBoolean((String) prop.getOrDefault("OneoffAnimation", "false"));
            int framesCount = Integer.parseInt((String) prop.getOrDefault("FramesCount", "0"));
            int animSpeed = Integer.parseInt((String) prop.getOrDefault("AnimationSpeed", "0"));
            float weight = Float.parseFloat((String) prop.getOrDefault("Weight", "0.001f"));
            float maxHp = Float.parseFloat((String) prop.getOrDefault("MaxHp", "100"));
            String path = Window.assetsDir((String) prop.getOrDefault("Path", "\\World\\textureNotFound.png"));

            consts.put(id, new DynamicObjectsConst(isFlying, oneoffAnimation, framesCount, animSpeed, weight, maxHp, path));
        }
    }

    public static DynamicObjectsConst getConst(byte id) {
        return consts.get(id);
    }
}
