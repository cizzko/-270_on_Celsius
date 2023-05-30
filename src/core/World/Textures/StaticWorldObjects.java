package core.World.Textures;

//статичные объекты, ставятся в строго заданных координатах и не могут произвольно быть пермещены
public class StaticWorldObjects {
    public boolean gas, liquid, solid, plasma, notForDrawing, onCamera;
    public String options, path;
    public float y, x;

    public StaticWorldObjects(String options, String path, float x, float y) {
        this.onCamera = true;          //находится ли в фокусе, для оптимизации
        this.gas = false;              //является ли газом
        this.liquid = false;           //является ли жидкостью
        this.solid = false;            //является ли твердым
        this.plasma = false;           //является ли плазмой
        this.notForDrawing = false;         //для прозрачных газов типа воздуха
        this.options = options;        //описание и прочее
        this.path = path;              //путь до текстуры
        this.x = x;                    //мировая координата x
        this.y = y;                    //мировая координата у
    }

    public void destroyObject() {
        this.solid = false;
        this.liquid = false;
        this.plasma = false;
        this.gas = true;
    }
}