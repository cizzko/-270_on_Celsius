package core.World.Creatures;

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
            var player = Global.content.creatureById(name);

            consts.put(id, cnst = new DynamicObjectsConst(!player.hasGravity, true,
                    1, 1, player.weight, player.maxHp, player.texture));
        }
        return cnst;
    }

    public static DynamicObjectsConst getConst(byte id) {
        return consts.get(id);
    }
}
