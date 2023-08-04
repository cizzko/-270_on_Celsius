package core.World.Textures;

import java.io.Serializable;
import static core.Window.start;

public class StaticWorldObjects implements Serializable {
    public boolean gas, liquid, solid, plasma, onCamera, mirrored;
    public int id;
    public String path;
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
        this.id = getId(type);
    }

    public void destroyObject() {
        if (id != 0) {
            this.path = null;
            this.solid = false;
            this.liquid = false;
            this.plasma = false;
            this.gas = true;
            this.currentHp = 0;
            this.id = 0;
            if (start) {
                ShadowMap.update();
            }
        }
    }

    private static int getId(Types type) {
        switch (type) {
            case GAS        -> { return 0; }
            case DIRT       -> { return 1; }
            case GRASS      -> { return 2; }
            case STONE      -> { return 3; }
            case IRON_ORE   -> { return 4; }
            case DIRT_STONE -> { return 5; }
        }
        return 0;
    }
}