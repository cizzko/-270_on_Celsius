package core.World.Textures.StaticWorldObjects;

import core.World.Textures.ShadowMap;
import java.io.Serializable;
import static core.Window.start;

public class StaticWorldObjects implements Serializable {
    public int id;
    public float y, x, currentHp;

    public StaticWorldObjects(String name, float x, float y) {
        this.x = x;
        this.y = y;
        if (name != null) {
            StaticObjectsConst.setConst(name, name.hashCode());
            this.id = name.hashCode();
            this.currentHp = StaticObjectsConst.getConst(id).maxHp;
        } else {
            id = 0;
            currentHp = 0;
        }
    }

    public void destroyObject() {
        if (id != 0) {
            this.currentHp = 0;
            this.id = 0;
            if (start) {
                ShadowMap.update();
            }
        }
    }
    public float getMaxHp() {
        return StaticObjectsConst.checkIsHere(id) ? StaticObjectsConst.getConst(id).maxHp : null;
    }

    public float getDensity() {
        return StaticObjectsConst.checkIsHere(id) ? StaticObjectsConst.getConst(id).density : null;
    }

    public String getPath() {
        return StaticObjectsConst.checkIsHere(id) ? StaticObjectsConst.getConst(id).path : null;
    }

    public String getName() {
        return StaticObjectsConst.checkIsHere(id) ? StaticObjectsConst.getConst(id).objectName : "";
    }

    public String getFileName() {
        return StaticObjectsConst.checkIsHere(id) ? StaticObjectsConst.getConst(id).originalFileName : null;
    }

    public StaticObjectsConst.Types getType() {
        return StaticObjectsConst.checkIsHere(id) ? StaticObjectsConst.getConst(id).type : null;
    }
}