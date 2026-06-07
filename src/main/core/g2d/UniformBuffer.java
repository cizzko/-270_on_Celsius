package core.g2d;

import core.math.Mat3;
import core.math.Vector2f;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.Arrays;

public final class UniformBuffer {
    private static final int MAX_ID = Byte.MAX_VALUE;

    final Long2ObjectOpenHashMap<Block> hash2blocks = new Long2ObjectOpenHashMap<>(MAX_ID);
    final Block[] id2blocks = new Block[MAX_ID];

    public Block allocate(Shader shader) {
        return new Block(shader.uniforms.size());
    }

    public int push(Block block) {
        if (block.id == Block.UNITIALIZED) {
            int id = hash2blocks.size();
            if (id == MAX_ID) {
                throw new IllegalStateException("Buffer is full");
            }

            var existing = hash2blocks.putIfAbsent(block.hash(), block);
            if (block.equals(existing)) {
                block.id = existing.id;
                return block.id;
            }
            block.id = (byte) id;
            id2blocks[block.id] = block;
            return block.id;
        }
        return block.id;
    }

    public void clear() {
        hash2blocks.clear();
        Arrays.fill(id2blocks, null);
    }

    public sealed interface Uniform {
        String name();

        void setTo(Shader shader);

        long hash();

        static OfInt of(String name, int value) { return new OfInt(name, value); }
        static OfMat3 of(String name, Mat3 value) { return new OfMat3(name, value); }
        static OfVec2f of(String name, Vector2f value) { return new OfVec2f(name, value.x, value.y); }
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

        record OfMat3(String name, // Потому что жава
                      float m00, float m01, float m02,
                      float m10, float m11, float m12,
                      float m20, float m21, float m22) implements Uniform {
            OfMat3(String name, Mat3 mat) {
                this(name,
                        mat.val[Mat3.M00], mat.val[Mat3.M01], mat.val[Mat3.M02],
                        mat.val[Mat3.M10], mat.val[Mat3.M11], mat.val[Mat3.M12],
                        mat.val[Mat3.M20], mat.val[Mat3.M21], mat.val[Mat3.M22]);
            }

            @Override
            public void setTo(Shader shader) {
                shader.setUniformMat3(name,
                        m00, m01, m02,
                        m10, m11, m12,
                        m20, m21, m22);
            }

            @Override
            public long hash() {
                long h = 5381L;
                h += (h << 5L) + name.hashCode();
                h += (h << 5L) + Float.hashCode(m00);
                h += (h << 5L) + Float.hashCode(m01);
                h += (h << 5L) + Float.hashCode(m02);
                h += (h << 5L) + Float.hashCode(m10);
                h += (h << 5L) + Float.hashCode(m11);
                h += (h << 5L) + Float.hashCode(m12);
                h += (h << 5L) + Float.hashCode(m20);
                h += (h << 5L) + Float.hashCode(m21);
                h += (h << 5L) + Float.hashCode(m22);
                return h;
            }
        }

        record OfVec2f(String name, float x, float y) implements Uniform {
            @Override
            public void setTo(Shader shader) { shader.setUniformVec2f(name, x, y); }

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

        private final ObjectArrayList<Uniform> children;

        public Block(int uniformCount) {
            children = new ObjectArrayList<>(uniformCount);
        }

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
            Object[] elements = children.elements();
            for (int i = 0, n = children.size(); i < n; i++) {
                var child = (Uniform) elements[i];
                h += (h << 5L) + child.hash();
            }
            return h;
        }

        public void push(Uniform uniform) {
            if (id != UNITIALIZED) {
                throw new IllegalStateException("Block has already been completed");
            }
            this.children.add(uniform);
        }

        public void setTo(Shader shader) {
            Object[] elements = children.elements();
            for (int i = 0, n = children.size(); i < n; i++) {
                var child = (Uniform) elements[i];
                child.setTo(shader);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Block block)) {
                return false;
            }
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
