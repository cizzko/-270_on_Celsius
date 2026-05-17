package core.g2d;

import core.math.Mat3;
import core.pool.Pool;
import core.pool.Poolable;
import core.util.Color;
import org.intellij.lang.annotations.MagicConstant;

import java.util.ArrayDeque;
import java.util.Arrays;

import static core.g2d.Render.*;

public class StackfulRender {

    public static final class State implements Poolable {
        @MagicConstant(intValues = {LAYER_BACKGROUND, LAYER_BLOCKS, LAYER_ENTITIES, LAYER_GUI, LAYER_DEBUG})
        byte layer;
        @MagicConstant(intValues = {BLENDING_NORMAL})
        byte blending;

        Shader shader;
        int colorRgba8888;
        final Mat3 transform = new Mat3();
        float xScale, yScale;

        State() {
            reset();
        }

        void set(State old) {
            this.shader = old.shader;
            this.blending = old.blending;
            this.colorRgba8888 = old.colorRgba8888;
            this.transform.set(old.transform);
        }

        @Override
        public void reset() {
            shader = null;
            layer = LAYER_BACKGROUND;
            blending = BLENDING_NORMAL;
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
                state.layer,
                state.blending,
                tex.id(),
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
                state.layer,
                state.blending,
                tex.id(),
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
            @MagicConstant(intValues = {LAYER_BACKGROUND, LAYER_BLOCKS, LAYER_ENTITIES, LAYER_GUI, LAYER_DEBUG})
            byte layer,
            @MagicConstant(intValues = {BLENDING_NORMAL})
            byte blending,
            short texId,
            int rgba8888,
            float x, float y,
            float x2, float y2,
            float x3, float y3,
            float x4, float y4,
            float u, float v,
            float u2, float v2,
            Mat3 transform) {

        var rq = Render.queue();
        rq.advice(1, rq.getVertexCountPerQuad());

        var item = Render.allocItem();
        int vertexOffset = rq.addRectangle(rgba8888,
                x, y,
                x2, y2,
                x3, y3,
                x4, y4,
                u, v,
                u2, v2);
        item.vertexOffset = vertexOffset;
        item.vertexCount = rq.getVertexCountPerQuad();

        int INDICES_PER_QUAD = 6;
        int VERTICES_PER_QUAD = 4;
        int quadIndex = vertexOffset / VERTICES_PER_QUAD;

        item.indexOffset = quadIndex * INDICES_PER_QUAD;
        item.indexCount = 6;
        transform.to(item.matrix);
        item.sortKey = Render.makeSortKey(
                layer,
                blending,
                texId,
                ShaderCache.shadersById.firstByteKey(),
                rq.getItemIndex()); // TODO

        item.validate();
        rq.push(item);
    }

    public static void draw(
            @MagicConstant(intValues = {LAYER_BACKGROUND, LAYER_BLOCKS, LAYER_ENTITIES, LAYER_GUI, LAYER_DEBUG})
            byte layer,
            @MagicConstant(intValues = {BLENDING_NORMAL})
            byte blending,
            short texId,
            int rgba8888,
            float x, float y,
            float x2, float y2,
            float u, float v,
            float u2, float v2,
            Mat3 transform)
    {

        var rq = Render.queue();

        rq.advice(1, rq.getVertexCountPerQuad());

        var item = Render.allocItem();
        int vertexOffset = rq.addRectangle(rgba8888, x, y, x2, y2, u, v, u2, v2);
        item.vertexOffset = vertexOffset;
        item.vertexCount = rq.getVertexCountPerQuad();

        int INDICES_PER_QUAD = 6;
        int VERTICES_PER_QUAD = 4;
        int quadIndex = vertexOffset / VERTICES_PER_QUAD;

        item.indexOffset = quadIndex * INDICES_PER_QUAD;
        item.indexCount = 6;
        transform.to(item.matrix);
        item.sortKey = Render.makeSortKey(
                layer,
                blending,
                texId,
                ShaderCache.shadersById.firstByteKey(),
                rq.getItemIndex()); // TODO

        item.validate();
        rq.push(item);
    }

    public static void rect(Drawable tex,
                           int colorRgba8888,
                           float x, float y,
                           float x2, float y2,
                           float x3, float y3,
                           float x4, float y4) {
        draw(
                state.layer,
                state.blending,
                tex.id(),
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
}
