package World;

public class WorldObjects {
    boolean gas, liquid, solid, plasma, sleeping, onCamera, destroyed;
    String options;
    int y;
    int x;

    public WorldObjects(boolean gas, boolean onCamera, boolean destroyed, boolean liquid, boolean solid, boolean plasma, boolean sleeping, String options, int x, int y) {
        this.gas = gas;
        this.onCamera = onCamera;
        this.destroyed = destroyed;
        this.liquid = liquid;
        this.solid = solid;
        this.plasma = plasma;
        this.sleeping = sleeping;
        this.options = options;
        this.x = x;
        this.y = y;

    }
}

