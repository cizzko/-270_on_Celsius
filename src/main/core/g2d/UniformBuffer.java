package core.g2d;

import core.math.Mat3;
import core.math.Vector2f;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.Arrays;

import static it.unimi.dsi.fastutil.HashCommon.arraySize;
import static it.unimi.dsi.fastutil.HashCommon.maxFill;

public final class UniformBuffer {
    private static final int MAX_ID = Byte.MAX_VALUE;

    final BlockHashSet blockSet = new BlockHashSet(MAX_ID, MAX_ID, ObjectOpenHashSet.FAST_LOAD_FACTOR);

    public Block allocate(Shader shader) {
        int capacity = shader.uniforms.size();
        if (blockSet.poolSize > 0) {
            Block block = blockSet.pool[--blockSet.poolSize];
            blockSet.pool[blockSet.poolSize] = null;
            block.prepare(capacity);
            return block;
        }
        return new Block(capacity);
    }

    public int push(Block block) {
        if (block.id == Block.UNITIALIZED) {
            byte nextId = (byte) blockSet.size;
            var existing = blockSet.addOrGet(block, nextId);
            if (existing == null) {
                throw new IllegalStateException("Buffer is full");
            }
            if (existing != block) {
                returnInFramePool(block);
                return existing.id;
            }
        }
        return block.id;
    }

    private void returnInFramePool(Block block) {
        if (blockSet.poolSize < blockSet. pool.length) {
            block.reset();
            blockSet.pool[blockSet.poolSize++] = block;
        }
    }

    public void clear() {
        blockSet.clear();
        for (int i = 0; i < blockSet.id2blocks.length; i++) {
            Block block = blockSet.id2blocks[i];
            if (block == null) break;
            blockSet.id2blocks[i] = null;

            if (blockSet.poolSize < blockSet.pool.length) {
                block.reset();
                blockSet.pool[blockSet.poolSize++] = block;
            }
        }

        Arrays.fill(blockSet.id2blocks, null);
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
                h = (h << 5) + h + name.hashCode();
                h = (h << 5) + h + Float.hashCode(value);
                return h;
            }
        }

        record OfInt(String name, int value) implements Uniform {
            @Override
            public void setTo(Shader shader) { shader.setUniformInt(name, value); }

            @Override
            public long hash() {
                long h = 5381L;
                h = (h << 5) + h + name.hashCode();
                h = (h << 5) + h + value;
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
                h = (h << 5) + h + name.hashCode();
                h = (h << 5) + h + Float.hashCode(m00);
                h = (h << 5) + h + Float.hashCode(m01);
                h = (h << 5) + h + Float.hashCode(m02);
                h = (h << 5) + h + Float.hashCode(m10);
                h = (h << 5) + h + Float.hashCode(m11);
                h = (h << 5) + h + Float.hashCode(m12);
                h = (h << 5) + h + Float.hashCode(m20);
                h = (h << 5) + h + Float.hashCode(m21);
                h = (h << 5) + h + Float.hashCode(m22);
                return h;
            }
        }

        record OfVec2f(String name, float x, float y) implements Uniform {
            @Override
            public void setTo(Shader shader) { shader.setUniformVec2f(name, x, y); }

            @Override
            public long hash() {
                long h = 5381L;
                h = (h << 5) + h + name.hashCode();
                h = (h << 5) + h + Float.hashCode(x);
                h = (h << 5) + h + Float.hashCode(y);
                return h;
            }
        }

        record OfTexture2d(String name, short texId, int bindSlot) implements Uniform {
            @Override
            public void setTo(Shader shader) { shader.setUniformTexture2d(name, texId, bindSlot); }
            @Override
            public long hash() {
                long h = 5381L;
                h = (h << 5) + h + name.hashCode();
                h = (h << 5) + h + texId;
                h = (h << 5) + h + bindSlot;
                return h;
            }
        }
    }

    public static final class Block {
        static final byte UNITIALIZED = -1;

        public byte id = UNITIALIZED;
        public boolean hashComputed;

        private final ObjectArrayList<Uniform> children;

        public Block(int uniformCount) {
            children = new ObjectArrayList<>(uniformCount);
        }

        private long hash;
        public long hash() {
            long h = hash;
            if (!hashComputed) {
                hashComputed = true;
                hash = h = computeHash();
            }
            return h;
        }

        void reset() {
            id = UNITIALIZED;
            hash = 0;
            hashComputed = false;
            children.clear();
        }

        void prepare(int capacity) {
            id = UNITIALIZED;
            hash = 0;
            hashComputed = false;
            children.ensureCapacity(capacity);
        }

        private long computeHash() {
            long h = 5381L;
            Object[] elements = children.elements();
            for (int i = 0, n = children.size(); i < n; i++) {
                var child = (Uniform) elements[i];
                h = (h << 5) + h + child.hash();
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

        public boolean equals(Block block) {
            return block == this || children.equals(block.children);
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

    static final class BlockHashSet {
        private final Block[] pool;
        final Block[] id2blocks;

        private int poolSize = 0;
        private Block[] key;
        private int mask;
        private int n;
        private int maxFill;
        private int size;

        private final int maxSize;
        private final float f;

        public BlockHashSet(int expected, int maxSize, float f) {
            if (f <= 0 || f >= 1) throw new IllegalArgumentException("Load factor must be greater than 0 and smaller than 1");
            if (expected < 0) throw new IllegalArgumentException("The expected number of elements must be nonnegative");
            this.f = f;
            this.maxSize = maxSize;
            id2blocks = new Block[maxSize];
            pool = new Block[maxSize];
            n = arraySize(expected, f);
            mask = n - 1;
            maxFill = maxFill(n, f);
            key = new Block[n + 1];
        }

        public void clear() {
            if (size == 0) return;
            size = 0;
            Arrays.fill(key, null);
        }

        public Block addOrGet(Block k, byte nextId) {
            var key = this.key;

            long hash = k.hash();
            int pos = (int)(HashCommon.mix(hash) & mask);
            var curr = key[pos];
            if (curr != null) {
                do {
                    if (hash == curr.hash && curr.equals(k)) return curr;
                } while ((curr = key[pos = pos + 1 & mask]) != null);
            }
            if (size == maxSize) {
                return null;
            }

            k.id = nextId;
            key[pos] = k;
            id2blocks[nextId] = k;
            if (size++ >= maxFill) rehash(arraySize(size + 1, f));
            return k;
        }
        private void rehash(final int newN) {
            final var key = this.key;
            final int mask = newN - 1;
            final var newKey = new Block[newN + 1];

            int i = n, pos;
            for (int j = size; j-- != 0;) {
                while (key[--i] == null);
                if (newKey[pos = (int) (HashCommon.mix(key[i].hash()) & mask)] != null)
                    while (newKey[pos = pos + 1 & mask] != null);
                newKey[pos] = key[i];
            }
            n = newN;
            this.mask = mask;
            maxFill = maxFill(n, f);
            this.key = newKey;
        }
    }
}
