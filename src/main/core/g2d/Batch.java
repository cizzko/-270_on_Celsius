package core.g2d;

import core.Global;
import core.Utils.Disposable;
import core.Utils.SimpleColor;
import core.math.Mat3;
import core.pool.Pool;
import core.pool.Poolable;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static core.g2d.VertexAttribute.*;
import static org.lwjgl.opengl.GL46.*;

public class Batch<S extends Batch.State> implements Disposable {
    private static final int VERTEX_PER_SPRITE   = 4;
    private static final int VERTEX_PER_TRIANGLE = 6;
    private static final int MAX_NESTING = 16;

    private static final VertexFormat VERTEX_FORMAT = VertexFormat.of(List.of(
            create(2, Type.FLOAT, Interp.DIRECT_FLOAT),
            create(4, Type.UNSIGNED_BYTE, Interp.NORMALIZED),
            create(2, Type.FLOAT, Interp.DIRECT_FLOAT)
    ));

    protected final Mat3 matrix = new Mat3();

    protected Texture currentTexture;

    protected FloatBuffer vertices;
    protected Mesh mesh;
    protected Shader shader;
    protected int vertexCount;

    private boolean disposed;

    private final Disposable stackProcessor = this::popState0;

    protected static class State implements Poolable {
        protected Blending blending;
        protected int colorRgba;
        protected float xScale, yScale;

        protected State() {
            reset();
        }

        protected void set(State old) {
            this.blending = old.blending;
            this.colorRgba = old.colorRgba;
            this.xScale = old.xScale;
            this.yScale = old.yScale;
        }

        @Override
        public void reset() {
            blending = Blending.NORMAL;
            colorRgba = SimpleColor.WHITE.rgba;
            xScale = yScale = 1f;
        }
    }

    private final Pool<S> statePool;
    private final BiConsumer<S, S> extender;
    private final ArrayDeque<S> stack = new ArrayDeque<S>(MAX_NESTING);
    protected S state;

    private void popState0() {
        stack.removeLast();
        statePool.free(state);
        state = stack.getLast();
    }

    private void pushState0() {
        S newState = statePool.obtain();
        stack.addLast(newState);
        if (state != null)
            extender.accept(newState, state);
        state = newState;
    }

    // region Изменение параметров

    public final void pushState(Runnable run) {
        pushState0();
        try {
            run.run();
        } finally {
            popState0();
        }
    }

    public final Disposable pushState() {
        pushState0();
        return stackProcessor;
    }

    public final void blending(Blending blending) {
        if (blending == state.blending) {
            return;
        }
        flush();
        state.blending = blending;
    }

    public final void scale(float scale) {
        scale(scale, scale);
    }

    public final void scale(float xScale, float yScale) {
        state.xScale = xScale;
        state.yScale = yScale;
    }

    public final void color(SimpleColor color) {
        state.colorRgba = color.rgba;
    }

    // endregion

    public Batch(int bufferSize, Supplier<? extends S> constr, BiConsumer<S, S> extender) {
        this.statePool = new Pool<>(constr, MAX_NESTING);
        this.extender = extender;
        try {
            shader = Shader.load(Global.assets.assetsDir("Shaders/default"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int vertexCount = bufferSize * VERTEX_PER_SPRITE;
        vertices = BufferUtils.createFloatBuffer(vertexCount);
        mesh = new Mesh(GL_STATIC_DRAW);

        mesh.bindVao();
        mesh.bindVbo();

        VERTEX_FORMAT.enableAttributes();

        int indexesCount = bufferSize * VERTEX_PER_TRIANGLE;
        IntBuffer indexes = BufferUtils.createIntBuffer(indexesCount);

        int j = 0;
        for (int i = 0; i < bufferSize; i++) {
            indexes.put(j);
            indexes.put(j + 1);
            indexes.put(j + 2);

            indexes.put(j + 2);
            indexes.put(j + 3);
            indexes.put(j);

            j += 4;
        }

        indexes.flip();
        mesh.updateIndexes(indexes);
        mesh.useIndexes(true);

        pushState();
    }

    public final void matrix(Mat3 matrix) {
        flush();

        this.matrix.set(matrix);
    }

    protected final void prepareTexture(Texture tex) {
        if (currentTexture != tex || vertices.remaining() < VERTEX_FORMAT.vertexByteSize()*VERTEX_PER_SPRITE) {
            flush();
            currentTexture = tex;
        }
    }

    public void flush() {
        if (vertexCount == 0) {
            return;
        }

        state.blending.apply();

        shader.use();
        shader.setUniform("u_texture", currentTexture);
        shader.setUniformTransforming("u_proj", matrix);

        vertices.flip();
        mesh.draw(GL_TRIANGLES, vertices, vertexCount * VERTEX_PER_TRIANGLE);

        vertices.clear();
        vertexCount = 0;
    }

    // drawing

    public final void draw(Drawable drawable, SimpleColor color) {
        draw(drawable, color, 0, 0);
    }

    public final void draw(Drawable drawable) {
        draw(drawable, state.colorRgba, 0, 0);
    }

    public final void draw(Drawable drawable, SimpleColor color, float x, float y) {
        draw(drawable, color.rgba, x, y, drawable.width() * state.xScale, drawable.height() * state.yScale);
    }

    // Хмхмх. Лучше отказаться в этом классе от обёртки SimpleColor
    public final void draw(Drawable drawable, int colorRgba, float x, float y) {
        draw(drawable, colorRgba, x, y, drawable.width() * state.xScale, drawable.height() * state.yScale);
    }

    public final void draw(Drawable drawable, float x, float y) {
        draw(drawable, state.colorRgba, x, y);
    }

    public final void draw(Drawable drawable, int colorRgba, float x, float y, float width, float height) {
        float x2 = x + width;
        float y2 = y + height;
        drawTexture(drawable, colorRgba, x, y, x, y2, x2, y2, x2, y); // index!!!
    }

    public final void draw(Drawable drawable, float x, float y, float width, float height) {
        draw(drawable, state.colorRgba, x, y, width, height);
    }

    public final void rect(Drawable drawable,
                           int colorRgba,
                           float x, float y,
                           float x2, float y2,
                           float x3, float y3,
                           float x4, float y4) {
        drawTexture(drawable, colorRgba, x, y, x2, y2, x3, y3, x4, y4);
    }

    protected void drawTexture(Drawable drawable,
                               int color,
                               float x, float y,
                               float x2, float y2,
                               float x3, float y3,
                               float x4, float y4) {
        prepareTexture(textureOf(drawable));

        rectInternal(x, y, x2, y2, x3, y3, x4, y4,
                drawable.u(), drawable.v(),
                drawable.u2(), drawable.v2(),
                SimpleColor.toGLBits(color));
    }

    protected final Texture textureOf(Drawable drawable) {
        return switch (drawable) {
            case Font.Glyph gl -> gl.font().getTexture();
            case Atlas.Region region -> region.atlas().getTexture();
            case Texture tex -> tex;
        };
    }

    protected final void vertex(float x, float y, float u, float v, float color) {
        vertices.put(x);
        vertices.put(y);
        vertices.put(color);
        vertices.put(u);
        vertices.put(v);

        vertexCount++;
    }

    protected final void rectInternal(float x1, float y1,
                                      float x2, float y2,
                                      float x3, float y3,
                                      float x4, float y4,
                                      float u1, float v1,
                                      float u2, float v2,
                                      float c) {

        vertex(x1, y1, u1, v2, c);
        vertex(x2, y2, u1, v1, c);
        vertex(x3, y3, u2, v1, c);
        vertex(x4, y4, u2, v2, c);
    }

    @Override
    public final boolean isDisposed() {
        return disposed;
    }

    @Override
    public final void close() {
        if (disposed) return;
        mesh.close();
        shader.close();
        MemoryUtil.memFree(vertices);
        disposed = true;
    }
}
