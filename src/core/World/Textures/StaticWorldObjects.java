package core.World.Textures;

//объекты мира и возможные переменные у них
public class StaticWorldObjects {
    public boolean gas, liquid, solid, plasma, sleeping, onCamera;
    public String options, path;
    public float y, x;

    public StaticWorldObjects(String options, String path, float x, float y) {
        this.onCamera = true;          //находится ли в фокусе, для оптимизации
        this.gas = false;              //является ли газом
        this.liquid = false;           //является ли жидкостью
        this.solid = false;            //является ли твердым
        this.plasma = false;           //является ли плазмой
        this.sleeping = false;         //спит (находится вне фокуса и не рисуется/неактивен)
        this.options = options;        //описание и прочее
        this.path = path;              //путь до текстуры
        this.x = x;                    //мировая координата x
        this.y = y;                    //мировая координата у
    }
}