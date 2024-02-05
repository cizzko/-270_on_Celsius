package core.World.StaticWorldObjects;

import core.EventHandling.Logging.Logger;
import core.assets.AssetsManager;
import core.g2d.Atlas;

import java.io.Serializable;
import java.util.HashMap;

public abstract class StaticWorldObjects implements Serializable {
    public static final HashMap<String, Byte> ids = new HashMap<>();

    public static short createStatic(String name) {
        name = AssetsManager.normalizePath(name);

        byte id = generateId(name);
        StaticObjectsConst.setConst(name, id);

        return (short) ((((byte) getMaxHp(id) & 0xFF) << 8) | (id & 0xFF));
    }

    public static boolean idsContains(String name) {
        return ids.containsKey(name);
    }

    public static byte generateId(String name) {
        if (name == null) {
            return 0;
        }
        byte id = ids.getOrDefault(name, (byte) 0);
        if (id != 0) {
            return id;
        } else {
            for (byte i = -127; i < 127; i++) {
                if (i != -1 && i != 0 && !ids.containsValue(i)) {
                    ids.put(name, i);
                    return i;
                }
                if (i == 126) {
                    Logger.log("Number of id's static objects exceeded, errors will occur");
                }
            }
        }
        return 0;
    }

    public static float getMaxHp(short id) {
        return StaticObjectsConst.checkIsHere(getId(id)) ? StaticObjectsConst.getConst(getId(id)).maxHp : 0;
    }

    public static float getDensity(short id) {
        return StaticObjectsConst.checkIsHere(getId(id)) ? StaticObjectsConst.getConst(getId(id)).density : 0;
    }

    public static Atlas.Region getTexture(short id) {
        return StaticObjectsConst.checkIsHere(getId(id)) ? StaticObjectsConst.getConst(getId(id)).texture : null;
    }

    public static String getName(short id) {
        return StaticObjectsConst.checkIsHere(getId(id)) ? StaticObjectsConst.getConst(getId(id)).objectName : "";
    }

    public static String getFileName(short id) {
        return StaticObjectsConst.checkIsHere(getId(id)) ? StaticObjectsConst.getConst(getId(id)).originalFileName : null;
    }

    public static StaticObjectsConst.Types getType(short id) {
        return StaticObjectsConst.checkIsHere(getId(id)) ? StaticObjectsConst.getConst(getId(id)).type : null;
    }

    public static float getResistance(short id) {
        return StaticObjectsConst.checkIsHere(getId(id)) ? StaticObjectsConst.getConst(getId(id)).resistance : 0;
    }

    public static int getLightTransmission(short id) {
        return StaticObjectsConst.checkIsHere(getId(id)) ? StaticObjectsConst.getConst(getId(id)).lightTransmission : 0;
    }

    public static Runnable getOnInteraction(short id) {
        return StaticObjectsConst.checkIsHere(getId(id)) ? StaticObjectsConst.getConst(getId(id)).onInteraction : null;
    }

    public static byte getId(short id) {
        return (byte) (id & 0xFF);
    }

    public static byte getHp(short id) {
        return (byte) ((id >> 8) & 0xFF);
    }

    public static short incrementHp(short id, int count) {
        return (short) (((getHp(id) + count & 0xFF) << 8) | (id & 0xFF));
    }

    public static short decrementHp(short id, int count) {
        return (short) (((getHp(id) - count & 0xFF) << 8) | (id & 0xFF));
    }
}
