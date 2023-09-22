package core.World.Textures.StaticWorldObjects;

import java.io.Serializable;

public class Structures implements Serializable {
    public short[][] blocks;
    public int lowestSolidBlock;

    public Structures(int lowestSolidBlock, short[][] blocks) {
        this.lowestSolidBlock = lowestSolidBlock;
        this.blocks = blocks;
    }
}
