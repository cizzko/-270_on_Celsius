package core.World.Textures;

import java.io.Serializable;

public class StaticWorldObjects implements Serializable {
    public boolean gas, liquid, solid, plasma, onCamera, mirrored;
    public String options, path;
    public float y, x, currentHp, totalHp;
    public Types type;

    public enum Types {
        GAS,
        GRASS,
        STONE,
        DIRT_STONE,
        DIRT,
        IRON_ORE
    }

    public StaticWorldObjects(String path, float x, float y, Types type) {
        this.onCamera = true;
        this.gas = false;
        this.liquid = false;
        this.solid = false;
        this.plasma = false;
        this.mirrored = false;
        this.path = path;
        this.x = x;
        this.y = y;
        this.totalHp = 100;
        this.currentHp = totalHp;
        this.type = type;
    }

    public void destroyObject() {
        this.path = null;
        this.solid = false;
        this.liquid = false;
        this.plasma = false;
        this.gas = true;
        this.currentHp = 0;
    }
}