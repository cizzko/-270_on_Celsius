package core.g2d;

import core.math.Mat3;
import core.pool.Pool;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Arrays;

public final class UniformBuffer {
    private static final int MAX_ID = Byte.MAX_VALUE;

    private final Pool<Block> pool = new Pool<>(Block::new, 16);

    final Long2ObjectOpenHashMap<Block> hash2blocks = new Long2ObjectOpenHashMap<>();
    final Byte2ObjectOpenHashMap<Block> id2blocks = new Byte2ObjectOpenHashMap<>(MAX_ID);

    public Block allocate() {
        return pool.create();
    }

    public int push(Block block) {
        if (block.id == Block.UNITIALIZED) {
            int id = hash2blocks.size();
            if (id == MAX_ID)
                throw new IllegalStateException("Buffer is full");

            var existing = hash2blocks.putIfAbsent(block.hash(), block);
            if (block.equals(existing)) {
                block.id = existing.id;
                return block.id;
            }
            block.id = (byte) id;
            block.children.trimToSize();
            id2blocks.put(block.id, block);
            return block.id;
        }
        return block.id;
    }

    public void free(Block block) {
        // todo хз когда
    }

    public void clear() {
        hash2blocks.clear();
        id2blocks.clear();
    }

    public void debug() {
        for (int i = 0; i < id2blocks.size(); i++) {
            System.out.println("block[" + i + "]: " + id2blocks.get((byte)i));
        }
    }

    public sealed interface Uniform {
        String name();

        void setTo(Shader shader);

        long hash();

        static OfInt of(String name, int value) { return new OfInt(name, value); }
        static OfMat3 of(String name, Mat3 value) { return new OfMat3(name, new Mat3(value)); }
        static OfVec2f of(String name, float x, float y) { return new OfVec2f(name, x, y); }
        static OfFloat of(String name, float value) { return new OfFloat(name, value); }
        static OfTexture2d of(String name, short texId, int bindSlot) { return new OfTexture2d(name, texId, bindSlot); }
        static OfTexture2d of(String name, Drawable tex, int bindSlot) { return new OfTexture2d(name, tex.id(), bindSlot); }

        record OfFloat(String name, float value) implements Uniform {
            @Override
            public void setTo(Shader shader) { shader.setUniformFloat(name, value); }

            @Override
            public long hash() {
                long h = 5381L;
                h += (h << 5L) + name.hashCode();
                h += (h << 5L) + Float.hashCode(value);
                return h;
            }
        }

        record OfInt(String name, int value) implements Uniform {
            @Override
            public void setTo(Shader shader) { shader.setUniformInt(name, value); }

            @Override
            public long hash() {
                long h = 5381L;
                h += (h << 5L) + name.hashCode();
                h += (h << 5L) + value;
                return h;
            }
        }

        record OfMat3(String name, Mat3 value) implements Uniform {
            @Override
            public void setTo(Shader shader) { shader.setUniformTransforming(name, value); }

            @Override
            public long hash() {
                long h = 5381L;
                h += (h << 5L) + name.hashCode();
                var fl = value.val;
                for (float f : fl) {
                    h += (h << 5L) + Float.hashCode(f);
                }
                return h;
            }
        }

        record OfVec2f(String name, float x, float y) implements Uniform {
            @Override
            public void setTo(Shader shader) { shader.setUniform(name, x, y); }

            @Override
            public long hash() {
                long h = 5381L;
                h += (h << 5L) + name.hashCode();
                h += (h << 5L) + Float.hashCode(x);
                h += (h << 5L) + Float.hashCode(y);
                return h;
            }
        }

        record OfTexture2d(String name, short texId, int bindSlot) implements Uniform {
            @Override
            public void setTo(Shader shader) { shader.setUniformTexture2d(name, texId, bindSlot); }
            @Override
            public long hash() {
                long h = 5381L;
                h += (h << 5L) + name.hashCode();
                h += (h << 5L) + texId;
                h += (h << 5L) + bindSlot;
                return h;
            }
        }
    }

    public static final class Block {
        static final byte UNITIALIZED = -1;

        public byte id = UNITIALIZED;

        private final ArrayList<Uniform> children = new ArrayList<>();

        private long hash;
        public long hash() {
            long h = hash;
            if (h == 0) {
                hash = h = computeHash();
            }
            return h;
        }

        private long computeHash() {
            long h = 5381L;
            for (Uniform child : children) {
                h += (h << 5L) + child.hash();
            }
            return h;
        }

        public void push(Uniform uniform) {
            if (id != UNITIALIZED)
                throw new IllegalStateException("Block has already been completed");
            this.children.add(uniform);
        }

        public void setTo(Shader shader) {
            // System.out.println();
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < children.size(); i++) {
                var child = children.get(i);
                // System.out.println(child);
                child.setTo(shader);
            }
        }

        void debug() {
            System.out.println("Id: " + id + " Hash: " + hash() + " Children: " + children);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Block block)) return false;
            return children.equals(block.children);
        }

        @Override
        public int hashCode() { return Long.hashCode(hash()); }

        @Override
        public String toString() {
            return "Block{" +
                   "id=" + id +
                   ", children=" + children +
                   ", hash=" + hash +
                   '}';
        }
    }
}
