package core.World.Textures.StaticWorldObjects;

import core.EventHandling.Logging.Logger;
import core.World.Creatures.Player.Inventory.Items.Details;
import core.World.Textures.ShadowMap;
import core.World.WorldGenerator;

import java.io.Serializable;
import java.util.HashMap;
import static core.Window.defPath;
import static core.Window.start;
import static core.World.Creatures.Player.Inventory.Inventory.createElementDetail;

public abstract class StaticWorldObjects implements Serializable {
    private static final String serVersion = "1.10";
    private static HashMap<String, Byte> ids = new HashMap<>();
    private static byte lastId = -127;

    public static short createStatic(String name) {
        byte id;

        if (ids.get(name) != null) {
            id = ids.get(name);
        } else {
            lastId++;
            if (lastId == -128) {
                Logger.log("Number of id's static objects exceeded, errors will occur");
            }

            ids.put(name, lastId);
            id = lastId;
        }
        StaticObjectsConst.setConst(name, id);

        return (short) ((((byte) getMaxHp(id) & 0xFF) << 8) | (id & 0xFF));
    }

    public static void destroyObject(int cellX, int cellY) {
        short id = WorldGenerator.getObject(cellX, cellY);

        if (id != 0) {
            WorldGenerator.setObject(cellX, cellY, (short) 0);

            if (StaticObjectsConst.getConst(getId(id)).optionalTiles != null) {
                new Thread(() -> {
                    short[][] tiles = StaticObjectsConst.getConst(getId(id)).optionalTiles;

                    for (int blockX = 0; blockX < tiles.length; blockX++) {
                        for (int blockY = 0; blockY < tiles[0].length; blockY++) {
                            if (getType(tiles[blockX][blockY]) != StaticObjectsConst.Types.GAS) {
                                WorldGenerator.setObject(cellX + blockX, cellY + blockY, (short) 0);
                                ShadowMap.update();
                            }
                        }
                    }
                }).start();
            } else {
                ShadowMap.update();
            }
            //TODO: костыль
            if (getName(id).toLowerCase().contains("trunk") && Math.random() * 100 < 30) {
                createElementDetail(new Details("Stick", defPath + "\\src\\assets\\World\\Items\\stick.png"), "");
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