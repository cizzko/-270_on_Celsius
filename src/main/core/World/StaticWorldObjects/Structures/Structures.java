package core.World.StaticWorldObjects.Structures;

import java.io.Serializable;

public class Structures implements Serializable {
    public String[][] blocks;
    public int lowestSolidBlock;

    public Structures(int lowestSolidBlock, String[][] blocks) {
        this.lowestSolidBlock = lowestSolidBlock;
        this.blocks = blocks;
    }
}
