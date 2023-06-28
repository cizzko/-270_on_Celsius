package core.World.Textures;

import java.io.Serializable;

//статичные объекты, ставятся в строго заданных координатах и не могут произвольно быть пермещены
public class StaticWorldObjects implements Serializable {
    public boolean gas, liquid, solid, plasma, onCamera;
    public String options, path;
    public float y, x;

    public StaticWorldObjects(String options, String path, float x, float y) {
        this.onCamera = true;          //находится ли в фокусе, для оптимизации
        this.gas = false;              //является ли газом
        this.liquid = false;           //является ли жидкостью
        this.solid = false;            //является ли твердым
        this.plasma = false;           //является ли плазмой
        this.options = options;        //описание и прочее
        this.path = path;              //путь до текстуры
        this.x = x;                    //мировая координата x
        this.y = y;                    //мировая координата у
    }

    public void destroyObject() {
        this.path = null;
        this.solid = false;
        this.liquid = false;
        this.plasma = false;
        this.gas = true;
    }
}