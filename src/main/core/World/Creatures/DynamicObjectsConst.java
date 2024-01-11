package core.World.Creatures;

import core.EventHandling.Logging.Config;
import core.Global;
import core.g2d.Atlas;

import java.util.HashMap;
import java.util.Properties;

import static core.Global.assets;

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
        var cnst = consts.get(id);
        if (cnst == null) {
            Properties prop = Config.getProperties(assets.assetsDir("/World/CreaturesCharacteristics/" + name + ".properties"));
            boolean isFlying = Boolean.parseBoolean((String) prop.getOrDefault("IsFlying", "false"));
            boolean oneoffAnimation = Boolean.parseBoolean((String) prop.getOrDefault("OneoffAnimation", "false"));
            int framesCount = Integer.parseInt((String) prop.getOrDefault("FramesCount", "0"));
            int animSpeed = Integer.parseInt((String) prop.getOrDefault("AnimationSpeed", "0"));
            float weight = Float.parseFloat((String) prop.getOrDefault("Weight", "0.001f"));
            float maxHp = Float.parseFloat((String) prop.getOrDefault("MaxHp", "100"));
            var texture = Global.atlas.byPath((String) prop.getOrDefault("Path", "/World/textureNotFound.png"));

            consts.put(id, cnst = new DynamicObjectsConst(isFlying, oneoffAnimation, framesCount, animSpeed, weight, maxHp, texture));
        }
        return cnst;
    }

    public static DynamicObjectsConst getConst(byte id) {
        return consts.get(id);
    }
}
