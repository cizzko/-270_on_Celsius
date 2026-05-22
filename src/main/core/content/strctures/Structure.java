package core.content.strctures;

import core.World.StaticWorldObjects.StaticObjectsConst;
import core.content.ContentLoader;
import core.content.ContentResolver;
import core.content.ContentType;
import core.content.blocks.BlockUnresolved;

import java.util.ArrayList;

public class Structure implements ContentType {

    public final String id;

    public final ArrayList<Part> blocks = new ArrayList<>();

    public Structure(String id) {
        this.id = id;
    }

    public static class Part implements Comparable<Part> {
        public final int offsetX;
        public final int offsetY;
        StaticObjectsConst block;

        public Part(int offsetX, int offsetY, StaticObjectsConst block) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.block = block;
        }

        public StaticObjectsConst block() {
            return block;
        }

        public void resolve(ContentResolver res) {
            block = res.resolveBlock(block);
        }

        @Override
        public int compareTo(Structure.Part o) {
            int cmp = Integer.compare(offsetX, o.offsetX);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(offsetY, o.offsetY);
        }

        @Override
        public String toString() {
            return "Part{[" + offsetX + ", " + offsetY +  "], block=" + block + '}';
        }
    }

    @Override
    public final String id() { return id; }

    @Override
    public void load(ContentLoader cnt) {
        for (var node : cnt.node().path("Blocks")) {
            int offsetX = node.path("OffsetX").asInt(0);
            int offsetY = node.path("OffsetY").asInt(0);
            var block = new BlockUnresolved(node.required("Block").asText());
            blocks.add(new Part(offsetX, offsetY, block));
        }
        blocks.sort(null);
        blocks.trimToSize();
    }

    @Override
    public void resolve(ContentResolver res) {
        for (Part block : blocks) {
            block.resolve(res);
        }
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj || obj instanceof Structure s && id.equals(s.id);
    }
}
