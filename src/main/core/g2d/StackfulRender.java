package core.g2d;

import core.math.Mat3;
import core.pool.Pool;
import core.pool.Poolable;
import core.util.Color;
import core.util.Disposable;
import org.intellij.lang.annotations.MagicConstant;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;

import static core.g2d.Render.*;

public class StackfulRender {

    public static void pushRList() {
        Render.queue.push(state.rlist);
    }

    public static final class State implements Poolable {
        public RenderList rlist;

        @MagicConstant(intValues = {LAYER_BACKGROUND, LAYER_BLOCKS, LAYER_ENTITIES, LAYER_GUI, LAYER_DEBUG})
        public byte layer;
        @MagicConstant(intValues = {BLENDING_NORMAL})
        public byte blending;

        public Shader shader;
        public int colorRgba8888;
        public final Mat3 transform = new Mat3();
        public float xScale, yScale;

        State() {
            reset();
        }

        public void set(State old) {
            this.rlist = old.rlist;
            this.layer = old.layer;
            this.blending = old.blending;
            this.shader = old.shader;
            this.colorRgba8888 = old.colorRgba8888;
            this.transform.set(old.transform);
            this.xScale = old.xScale;
            this.yScale = old.yScale;
        }

        @Override
        public void reset() {
            rlist = null;
            layer = LAYER_BACKGROUND;
            blending = BLENDING_NORMAL;
            shader = defaultShader;
            colorRgba8888 = Color.white;
            xScale = yScale = 1;
            Arrays.fill(transform.val, 0);
        }

        @Override
        public String toString() {
            return "State{" +
                   "layer=" + layer +
                   ", blending=" + blending +
                   ", shader=" + shader +
                   ", colorRgba8888=" + colorRgba8888 +
                   ", transform=" + transform +
                   ", xScale=" + xScale +
                   ", yScale=" + yScale +
                   '}';
        }
    }

    private static final int MAX_NESTING = 10;

    private static final Pool<State> statePool = new Pool<>(State::new, MAX_NESTING);
    private static final ArrayDeque<State> stack = new ArrayDeque<>(MAX_NESTING);
    private static State state;

    public static Shader defaultShader;

    static {
        pushState0();
    }

    public static State state() {
        return state;
    }

    private static void popState0() {
        stack.removeLast();
        statePool.free(state);
        state = stack.getLast();
    }

    public static Disposable pushState() {
        pushState0();
        return StackfulRender::popState0;
    }

    private static void pushState0() {
        var newState = statePool.obtain();
        stack.addLast(newState);
        if (state != null) {
            newState.set(state);
        }
        state = newState;
    }

    public static void pushState(Runnable run) {
        pushState0();
        try {
            run.run();
        } finally {
            popState0();
        }
    }

    public static void draw(Drawable tex, float x, float y) {
        draw(tex, x, y, tex.width() * state.xScale, tex.height() * state.yScale);
    }

    public static void draw(Drawable tex, int colorRgba8888, float x, float y) {
        float w = tex.width() * state.xScale;
        float h = tex.height() * state.yScale;

        draw(
                state.rlist,
                state.layer,
                state.blending,
                tex.id(),
                state.shader.id(),
                colorRgba8888,
                x, y,
                x + w, y + h,
                tex.u(), tex.v(),
                tex.u2(), tex.v2(),
                state.transform
        );
    }

    public static void draw(Drawable tex, Color color, float x, float y) { draw(tex, color.rgba8888(), x, y); }

    public static void draw(Drawable tex, int colorRgba8888, float x, float y, float w, float h) {
        draw(
                state.rlist,
                state.layer,
                state.blending,
                tex.id(),
                state.shader.id(),
                colorRgba8888,
                x, y,
                x + w, y + h,
                tex.u(), tex.v(),
                tex.u2(), tex.v2(),
                state.transform
        );
    }

    public static void draw(Drawable tex, float x, float y, float w, float h) {
        draw(tex, state.colorRgba8888, x, y, w, h);
    }


    public static void draw(
            RenderList rlist,
            @MagicConstant(intValues = {LAYER_BACKGROUND, LAYER_BLOCKS, LAYER_ENTITIES, LAYER_GUI, LAYER_DEBUG})
            byte layer,
            @MagicConstant(intValues = {BLENDING_NORMAL})
            byte blending,
            short texId,
            byte shader,
            int rgba8888,
            float x, float y,
            float x2, float y2,
            float x3, float y3,
            float x4, float y4,
            float u, float v,
            float u2, float v2,
            Mat3 transform) {


        int vertexCountPerQuad = queue.getVertexCountPerQuad();
        rlist.checkSpace(1, vertexCountPerQuad);

        var item = Render.allocItem();

        item.vertexOffset = rlist.getVertexIndex();
        item.vertexCount = vertexCountPerQuad;

        rlist.addRectangle(Render.PRIMITIVE_TYPE_TRIANGLES, rgba8888,
                x, y,
                x2, y2,
                x3, y3,
                x4, y4,
                u, v,
                u2, v2);

        final int INDICES_PER_QUAD = 6;
        final int VERTICES_PER_QUAD = 4;
        int quadIndex = item.vertexOffset / VERTICES_PER_QUAD;

        item.indexOffset = quadIndex * INDICES_PER_QUAD;
        item.indexCount = 6;

        transform.to(item.matrix);
        item.sortKey = Render.makeSortKey(layer, blending, texId, shader, rlist.getItemIndex());

        item.validate();
        rlist.push(item);
    }

    public static void draw(
            RenderList rlist,
            @MagicConstant(intValues = {LAYER_BACKGROUND, LAYER_BLOCKS, LAYER_ENTITIES, LAYER_GUI, LAYER_DEBUG})
            byte layer,
            @MagicConstant(intValues = {BLENDING_NORMAL})
            byte blending,
            short texId,
            byte shaderId,
            int rgba8888,
            float x, float y,
            float x2, float y2,
            float u, float v,
            float u2, float v2,
            Mat3 transform)
    {

        int vertexCountPerQuad = queue.getVertexCountPerQuad();
        rlist.checkSpace(1, vertexCountPerQuad);

        var item = Render.allocItem();
        item.vertexOffset = rlist.getVertexIndex();
        item.vertexCount = vertexCountPerQuad;
        rlist.addRectangle(PRIMITIVE_TYPE_TRIANGLES, rgba8888, x, y, x2, y2, u, v, u2, v2);

        int INDICES_PER_QUAD = 6;
        int VERTICES_PER_QUAD = 4;
        int quadIndex = item.vertexOffset / VERTICES_PER_QUAD;

        item.indexOffset = quadIndex * INDICES_PER_QUAD;
        item.indexCount = 6;
        transform.to(item.matrix);
        item.sortKey = Render.makeSortKey(layer, blending, texId, shaderId, rlist.getItemIndex());

        item.validate();
        rlist.push(item);
    }

    public static void rect(Drawable tex,
                           int colorRgba8888,
                           float x, float y,
                           float x2, float y2,
                           float x3, float y3,
                           float x4, float y4) {
        draw(
                state.rlist,
                state.layer,
                state.blending,
                tex.id(),
                state.shader.id(),
                colorRgba8888,
                x, y,
                x2, y2,
                x3, y3,
                x4, y4,
                tex.u(), tex.v(),
                tex.u2(), tex.v2(),
                state.transform
        );
    }

    public static void z(@MagicConstant(intValues = {LAYER_BACKGROUND, LAYER_BLOCKS, LAYER_ENTITIES, LAYER_GUI, LAYER_DEBUG}) byte z) {
        state.layer = z;
    }

    public static void blending(@MagicConstant(intValues = {BLENDING_NORMAL}) byte blending) {
        state.blending = blending;
    }

    public static void scale(float scale) {
        scale(scale, scale);
    }

    public static void scale(float xScale, float yScale) {
        state.xScale = xScale;
        state.yScale = yScale;
    }

    public static void color(Color color) { state.colorRgba8888 = color.rgba8888(); }

    public static void matrix(Mat3 matrix) {
        state.transform.set(matrix);
    }

    public static void shader(Shader shader) {
        state.shader = shader;
    }

    public static void rlist(RenderList renderList) {
        state.rlist = renderList;
    }
}
