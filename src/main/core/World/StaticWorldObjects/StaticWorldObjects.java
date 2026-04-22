package core.World.StaticWorldObjects;

import core.Application;
import core.assets.AssetsManager;
import core.g2d.Atlas;

import java.io.Serializable;
import java.util.HashMap;

public abstract class StaticWorldObjects implements Serializable {
    public static final HashMap<String, Byte> ids = new HashMap<>();
    private static byte lastId = -128;

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
            lastId++;

            if (lastId == -128) {
                Application.log.warn("Number of id's static objects exceeded, errors will occur");
            }
            ids.put(name, lastId);
            return lastId;
        }
    }

    private static StaticObjectsConst get(short id) {
        return StaticObjectsConst.getConst(getId(id));
    }

    public static float getMaxHp(short id) {
        StaticObjectsConst c = get(id);
        return (c != null) ? c.maxHp : 0;
    }

    public static float getDensity(short id) {
        StaticObjectsConst c = get(id);
        return (c != null) ? c.density : 0;
    }

    public static float getResistance(short id) {
        StaticObjectsConst c = get(id);
        return (c != null) ? c.resistance : 0;
    }

    public static int getLightTransmission(short id) {
        StaticObjectsConst c = get(id);
        return (c != null) ? c.lightTransmission : 0;
    }

    public static Atlas.Region getTexture(short id) {
        StaticObjectsConst c = get(id);
        return (c != null) ? c.texture : null;
    }

    public static String getName(short id) {
        StaticObjectsConst c = get(id);
        return (c != null) ? c.objectName : "";
    }

    public static String getFileName(short id) {
        StaticObjectsConst c = get(id);
        return (c != null) ? c.originalFileName : null;
    }

    public static StaticObjectsConst.Types getType(short id) {
        StaticObjectsConst c = get(id);
        return (c != null) ? c.type : StaticObjectsConst.Types.SOLID;
    }

    public static Runnable getOnInteraction(short id) {
        StaticObjectsConst c = get(id);
        return (c != null) ? c.onInteraction : null;
    }

    public static boolean hasMotherBlock(short id) {
        StaticObjectsConst c = get(id);
        return (c != null) && c.hasMotherBlock;
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
