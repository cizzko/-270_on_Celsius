package core.content.strctures;

import com.fasterxml.jackson.databind.JsonNode;
import core.content.blocks.Block;
import core.content.ContentLoader;
import core.content.ContentResolver;
import core.content.ContentType;
import core.content.Loadable;
import core.content.blocks.BlockUnresolved;

import java.util.ArrayList;

import static core.math.MathUtil.toShortExact;
import static core.util.TypeUtil.canonicalNameOrParent;

public class Structure implements ContentType, Loadable {

    public final String key;

    public short id;

    public final ArrayList<Part> blocks = new ArrayList<>();

    public Structure(String key) {
        this.key = key;
    }

    public static class Part implements Comparable<Part> {
        public final short offsetX;
        public final short offsetY;
        Block block;

        public Part(short offsetX, short offsetY, Block block) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.block = block;
        }

        public Block block() {
            return block;
        }

        public void resolve(ContentResolver res) {
            block = res.resolveBlock(block);
        }

        @Override
        public int compareTo(Structure.Part o) {
            int cmp = Short.compare(offsetX, o.offsetX);
            if (cmp != 0) {
                return cmp;
            }
            return Short.compare(offsetY, o.offsetY);
        }

        @Override
        public String toString() {
            return "Part{[" + offsetX + ", " + offsetY +  "], block=" + block + '}';
        }
    }

    public final String key() { return key; }
    public final short id() { return id; }

    public final void setId(short id) { this.id = id; }

    @Override
    public void load(ContentLoader cnt) {
        JsonNode blocksNode = cnt.node().path("Blocks");
        blocks.ensureCapacity(blocksNode.size());
        for (var node : blocksNode) {
            short offsetX = toShortExact(node.path("OffsetX").asInt(0));
            short offsetY = toShortExact(node.path("OffsetY").asInt(0));
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
    public final int hashCode() {
        return key.hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj || obj instanceof Structure s && key.equals(s.key);
    }

    @Override
    public final String toString() {
        return canonicalNameOrParent(getClass()) + "['" + key + "']";
    }
}
