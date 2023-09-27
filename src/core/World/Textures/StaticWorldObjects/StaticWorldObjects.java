package core.World.Textures.StaticWorldObjects;

import core.World.Creatures.Player.Inventory.Items.Details;
import core.World.Textures.ShadowMap;
import core.World.WorldGenerator;

import java.io.Serializable;
import static core.Window.defPath;
import static core.Window.start;
import static core.World.Creatures.Player.Inventory.Inventory.createElementDetail;

public abstract class StaticWorldObjects implements Serializable {
    public static short createStatic(String name) {
        byte id = (byte) name.hashCode();
        StaticObjectsConst.setConst(name, id);

        return (short) ((((byte) getMaxHp(id) & 0xFF) << 8) | (id & 0xFF));
    }

    public static void destroyObject(int cellX, int cellY) {
        short id = WorldGenerator.getObject(cellX, cellY);

        if (id != 0) {
            WorldGenerator.setObject(cellX, cellY, (short) 0);
            //TODO: костыль
            if (getName(id).toLowerCase().contains("trunk") && Math.random() * 100 < 30) {
                createElementDetail(new Details("Stick", defPath + "\\src\\assets\\World\\Items\\stick.png"), "");
            }
            if (start) {
                ShadowMap.update();
            }
        }
    }

    public static float getMaxHp(short id) {
        return StaticObjectsConst.checkIsHere(getId(id)) ? StaticObjectsConst.getConst(getId(id)).maxHp : 0;
    }

    public static float getDensity(short id) {
        return StaticObjectsConst.checkIsHere(getId(id)) ? StaticObjectsConst.getConst(getId(id)).density : 0;
    }

    public static String getPath(short id) {
        return StaticObjectsConst.checkIsHere(getId(id)) ? StaticObjectsConst.getConst(getId(id)).path : null;
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