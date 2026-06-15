package core.g2d;

import core.math.Mat3;
import core.math.Vector2f;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import static core.g2d.BytePack.*;
import static it.unimi.dsi.fastutil.HashCommon.arraySize;
import static it.unimi.dsi.fastutil.HashCommon.maxFill;
import static java.lang.Float.*;

public final class UniformBuffer {
    public static final Logger log = LogManager.getLogger("UniformBuffer");

    private static final int MAX_ID = Byte.MAX_VALUE;

    UniformBuffer() {
        this(MAX_ID, MAX_ID, Hash.FAST_LOAD_FACTOR);
    }

    UniformBuffer(int maxSize, int expected, float f) {
        // if (f <= 0 || f >= 1) throw new IllegalArgumentException("Load factor must be greater than 0 and smaller than 1");
        // if (expected < 0) throw new IllegalArgumentException("The expected number of elements must be nonnegative");
        this.f = f;
        this.maxSize = maxSize;
        id2blocks = new Block[maxSize];
        pool = new Block[maxSize];
        n = arraySize(expected, f);
        mask = n - 1;
        maxFill = maxFill(n, f);
        key = new Block[n + 1];
    }

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

    public Block allocate(Shader shader) {
        if (poolSize > 0) {
            Block block = pool[--poolSize];
            pool[poolSize] = null;
            block.prepare(shader);
            return block;
        }
        return new Block(shader);
    }

    public int push(Block block) {
        if (block.id == Block.UNITIALIZED) {
            byte nextId = (byte) size;
            var existing = addOrGet(block, nextId);
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
        if (poolSize <  pool.length) {
            block.reset();
            pool[poolSize++] = block;
        }
    }

    public void clear() {
        if (size == 0) return;
        size = 0;
        Arrays.fill(key, null);
        for (int i = 0; i < id2blocks.length; i++) {
            Block block = id2blocks[i];
            if (block == null) break;
            id2blocks[i] = null;

            if (poolSize < pool.length) {
                block.reset();
                pool[poolSize++] = block;
            }
        }

        Arrays.fill(id2blocks, null);
    }

    private Block addOrGet(Block k, byte nextId) {
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

    public static final class Block {
        static final float[] tmpMat3 = new float[9];

        public static final byte UNITIALIZED = -1;

        private static final byte TYPE_INT   = 0;
        private static final byte TYPE_FLOAT = 1;
        private static final byte TYPE_VEC2F = 2;
        private static final byte TYPE_MAT3  = 3;
        private static final byte TYPE_TEX2D = 4;

        byte id = UNITIALIZED;
        private long hash;
        private boolean hashComputed;
        Shader shader;
        private int[] tape;
        private short ptr;

        public Block(Shader shader) {
            this.shader = shader;
            tape = new int[shader.tapeSize];
        }

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
            shader = null;
            ptr = 0;
        }

        void prepare(Shader shader) {
            // assert ptr == 0;
            // assert id == UNITIALIZED;
            // assert hash == 0;
            // assert hashComputed == false;
            this.shader = shader;
            int tapeSize = shader.tapeSize;
            if (tape.length < tapeSize) {
                tape = new int[tapeSize];
            }
        }

        private long computeHash() {
            long h = 5381L;
            h += (h << 5) + h + shader.id;

            int[] tape = this.tape;
            for (short i = 0, n = ptr; i < n; i++) {
                h += (h << 5) + h + tape[i];
            }
            return h;
        }

        public void pushInt(short loc, int value) {
            growIfNeeded(2);
            pushHeader(ptr++, TYPE_INT, loc);
            tape[ptr++] = value;
        }

        public void pushFloat(short loc, float value) {
            growIfNeeded(2);
            pushHeader(ptr++, TYPE_FLOAT, loc);
            tape[ptr++] = floatToRawIntBits(value);
        }

        public void pushVec2f(short loc, Vector2f vec) { pushVec2f(loc, vec.x, vec.y); }

        public void pushVec2f(short loc, float x, float y) {
            growIfNeeded(3);
            pushHeader(ptr++, TYPE_VEC2F, loc);
            tape[ptr++] = floatToRawIntBits(x);
            tape[ptr++] = floatToRawIntBits(y);
        }

        public void pushMat3(short loc, Mat3 mat) {
            growIfNeeded(10);
            pushHeader(ptr++, TYPE_MAT3, loc);
            for (float v : mat.val) {
                tape[ptr++] = floatToRawIntBits(v);
            }
        }

        public void pushTexture2d(short loc, short texId, short bindSlot) {
            growIfNeeded(2);
            pushHeader(ptr++, TYPE_TEX2D, loc);
            tape[ptr++] = packShortToInt(texId, bindSlot);
        }

        private void pushHeader(int idx, byte type, short loc) {
            // или если там реально uint32 для локаций, то можно декодировать в use()
            short realLoc = shader.relocate(loc);
            tape[idx] = ((type & 0xFF) << 16) | (realLoc & 0xFFFF);
        }

        private void growIfNeeded(int delta) {
            // TODO сделать проверки только в debug режиме. Запретить динамический ресайз
            // if (id != UNITIALIZED) throw new IllegalStateException("Block has already been completed");
            // Objects.checkFromIndexSize(idx, idx + delta, tapeArray.length);
            // short capacity = MathUtil.toShortExact(delta + idx);
            // if (tapeArray.length < capacity) {
            //     throw new IllegalArgumentException("Tape array overflow for shader: " + shader);
            //     capacity = MathUtil.toShortExact(
            //             Math.max(Math.min(tapeArray.length + (tapeArray.length >> 1),
            //                     it.unimi.dsi.fastutil.Arrays.MAX_ARRAY_SIZE), capacity));
            //     int[] copy = new int[capacity];
            //     System.arraycopy(tapeArray, 0, copy, 0, tapeArray.length);
            //     tapeArray = copy;
            // }
        }

        public void use(byte shaderId) {
            int[] tape = this.tape;
            int lo = 0;
            int hi = this.ptr;
            while (lo < hi) {
                int header = tape[lo++];
                byte type = (byte) (header >> 16);
                short loc = (short) header;

                switch (type) {
                    case TYPE_INT -> OpenGL.Uniform1i(shaderId, loc, tape[lo++]);
                    case TYPE_FLOAT -> OpenGL.Uniform1f(shaderId, loc, intBitsToFloat(tape[lo++]));
                    case TYPE_VEC2F -> {
                        float x = intBitsToFloat(tape[lo++]);
                        float y = intBitsToFloat(tape[lo++]);
                        OpenGL.Uniform2f(shaderId, loc, x, y);
                    }
                    case TYPE_MAT3 -> {
                        float[] res = tmpMat3;
                        for (int i = 0; i < res.length; i++) {
                            res[i] = Float.intBitsToFloat(tape[lo + i]);
                        }
                        OpenGL.UniformMatrix3fv(shaderId, loc, false, res);
                        lo += 9;
                    }
                    case TYPE_TEX2D -> {
                        int packed = tape[lo++];
                        short texId = unpackLeft(packed);
                        short bindSlot = unpackRight(packed);
                        OpenGL.bindTexture(shaderId, loc, texId, bindSlot);
                    }
                }
            }
        }

        public boolean equals(Block block) {
            return this == block ||
                   (shader == block.shader &&
                    Arrays.equals(tape, 0, ptr, block.tape, 0, block.ptr));
        }

        @Override
        public String toString() {
            return "Block{" +
                    "id=" + id +
                    ", hash=" + hash +
                    '}';
        }
    }
}
