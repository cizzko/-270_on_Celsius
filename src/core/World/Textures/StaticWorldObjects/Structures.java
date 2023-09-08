package core.World.Textures.StaticWorldObjects;

import java.io.Serializable;

public class Structures implements Serializable {
    public StaticWorldObjects[][] blocks;
    public int lowestSolidBlock;

    public Structures(int lowestSolidBlock, StaticWorldObjects[][] blocks) {
        this.lowestSolidBlock = lowestSolidBlock;
        this.blocks = blocks;
    }
}
