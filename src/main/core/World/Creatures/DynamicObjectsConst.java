package core.World.Creatures;

import core.EventHandling.Logging.Config;
import core.Global;
import core.g2d.Atlas;

import java.util.HashMap;

public class DynamicObjectsConst {
    private static final HashMap<Byte, DynamicObjectsConst> consts = new HashMap<>();
    public boolean isFlying, oneoffAnimation;
    public int framesCount, animSpeed;
    public float weight, maxHp;
    public Atlas.Region texture;

    private DynamicObjectsConst(boolean isFlying, boolean oneoffAnimation, int framesCount, int animSpeed, float weight, float maxHp, Atlas.Region texture) {
        this.isFlying = isFlying;
        this.oneoffAnimation = oneoffAnimation;
        this.framesCount = framesCount;
        this.animSpeed = animSpeed;
        this.weight = weight;
        this.maxHp = maxHp;
        this.texture = texture;
    }

    public static DynamicObjectsConst bindDynamic(String name, byte id) {
        DynamicObjectsConst cnst = consts.get(id);

        if (cnst == null) {
            var prop = Config.getProperties("World/CreaturesCharacteristics/" + name + ".properties");
            boolean isFlying = Boolean.parseBoolean(prop.getOrDefault("IsFlying", "false"));
            boolean oneoffAnimation = Boolean.parseBoolean(prop.getOrDefault("OneoffAnimation", "false"));
            int framesCount = Integer.parseInt(prop.getOrDefault("FramesCount", "0"));
            int animSpeed = Integer.parseInt(prop.getOrDefault("AnimationSpeed", "0"));
            float weight = Float.parseFloat(prop.getOrDefault("Weight", "0.001f"));
            float maxHp = Float.parseFloat(prop.getOrDefault("MaxHp", "100"));
            Atlas.Region texture = Global.atlas.byPath(prop.getOrDefault("Path", "World/textureNotFound.png"));

            consts.put(id, cnst = new DynamicObjectsConst(isFlying, oneoffAnimation, framesCount, animSpeed, weight, maxHp, texture));
        }
        return cnst;
    }

    public static DynamicObjectsConst getConst(byte id) {
        return consts.get(id);
    }
}
