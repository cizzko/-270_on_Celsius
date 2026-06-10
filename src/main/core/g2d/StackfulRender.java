package core.g2d;

import core.Global;
import core.g2d.UniformBuffer.Uniform;
import core.graphic.Camera2;
import core.math.Vector2f;
import core.pool.Pool;
import core.pool.Poolable;
import core.graphic.Color;
import core.util.Disposable;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import static core.g2d.Render.*;

/// Удобная несинхронизированная оболочка над [RenderList],
/// которая рассматривает параметры как состояние _фрейма_,
/// который в свою очередь можно модифицировать путём Copy-On-Write
/// через углубление по стеку
public final class StackfulRender {
    private StackfulRender() {}

    public static void pushRenderList() {
        queue.push(stateFrame.rlist);
    }

    public static void flush() {
        queue.flush();
    }

    public static void drawRepeated(Atlas.Region texture,
                                    float bx, float by,
                                    float bw, float bh) {  // TODO не должно быть привязки к множителю (blocksize) в методе
        float x1 = bx;
        float y1 = by;

        float x2 = x1 + bw;
        float y2 = y1 + bh;

        float u1 = BytePack.fromB16toFloat32(texture.u);
        float v1 = BytePack.fromB16toFloat32(texture.v);
        float u2 = BytePack.fromB16toFloat32(texture.u2);
        float v2 = BytePack.fromB16toFloat32(texture.v2);

        var ublockObj = queue.uniformBuffer().allocate(Shaders.repeat);
        ublockObj.push(Uniform.of("u_logical_ratio", Global.camera.projectionScale));
        // Здесь допустимо отсечение до float, поскольку рендерятся группы тайлов
        var camPos = Global.camera.position;
        ublockObj.push(Uniform.of("u_camera_pos", camPos.xf(), camPos.yf()));
        ublockObj.push(Uniform.of("u_reg_uv", u1, v1));
        ublockObj.push(Uniform.of("u_reg_size", u2 - u1, v2 - v1));

        int ublock = queue.uniformBuffer().push(ublockObj);

        draw(
                stateFrame.rlist,
                stateFrame.primitiveType,
                stateFrame.layer,
                stateFrame.blending,
                texture.id(),
                Shaders.repeat.id(),
                ublock,
                stateFrame.colorRgba8888,
                x1, y1,
                x2, y2,
                0, 0, bw, bh
        );
    }

    public static void drawPostEffect(Drawable screenTexture) {
        var rlist = stateFrame.rlist;

        short vertexCountPerQuad = queue.getVertexCountPerQuad(stateFrame.primitiveType);
        rlist.checkSpace(1, vertexCountPerQuad);

        var item = rlist.allocItem();
        item.vertexOffset = rlist.getVertexIndex();
        item.vertexCount = vertexCountPerQuad;

        // rlist.addRectangle(state.primitiveType, state.colorRgba8888,
        //         -1, 1,   // x1, y1 (левый верхний)
        //         -1, -1,    // x2, y2 (левый нижний)
        //         1, -1,     // x3, y3 (правый нижний)
        //         1, 1,      // x4, y4 (правый верхний)
        //         0, 0,       // u,  v  (левая нижняя точка текстуры)
        //         1, 1);

        short INDICES_PER_QUAD = 6;
        short VERTICES_PER_QUAD = 4;
        int quadIndex = item.vertexOffset / VERTICES_PER_QUAD;

        item.indexOffset = quadIndex * INDICES_PER_QUAD;
        item.indexCount = INDICES_PER_QUAD;
        item.sortKey = makeSortKey(stateFrame.primitiveType, stateFrame.layer, stateFrame.blending, screenTexture.id(), stateFrame.shader.id(), stateFrame.ublock, rlist.getItemIndex());

        item.validate();
        rlist.push(item);
    }

    public static final class StateFrame implements Poolable, Disposable {
        public static final int UBLOCK_UNSET = -1;

        public RenderList rlist;
        public @PrimitiveType byte primitiveType;
        public @Layer byte layer;
        public @Blending byte blending;

        public Shader shader;

        public int ublock;
        public final Vector2f logicalRatio = new Vector2f();
        public final Vector2f cameraPosition = new Vector2f();

        public int colorRgba8888;
        public float xScale, yScale;

        StateFrame() {
            reset();
        }

        public void set(StateFrame old) {
            this.rlist = old.rlist;
            this.primitiveType = old.primitiveType;
            this.layer = old.layer;
            this.blending = old.blending;
            this.shader = old.shader;
            this.ublock = old.ublock;
            this.logicalRatio.set(old.logicalRatio);
            this.cameraPosition.set(old.cameraPosition);
            this.colorRgba8888 = old.colorRgba8888;
            this.xScale = old.xScale;
            this.yScale = old.yScale;
        }

        public void reset() {
            rlist = null;
            ublock = UBLOCK_UNSET;
            primitiveType = PRIMITIVE_TYPE_TRIANGLES;
            layer = LAYER_BACKGROUND;
            blending = BLENDING_NORMAL;
            shader = defaultShader;
            colorRgba8888 = Color.white;
            xScale = yScale = 1;
            logicalRatio.set(0, 0);
            cameraPosition.set(0, 0);
        }

        public void uniformBlock(UniformBuffer.Block ublock) {
            this.ublock = ublock.id;
        }

        int ublock() {
            if (ublock == StateFrame.UBLOCK_UNSET) {
                var block = queue.uniformBuffer().allocate(shader);
                block.push(Uniform.of("u_logical_ratio", logicalRatio));
                block.push(Uniform.of("u_camera_pos", cameraPosition));
                return queue.uniformBuffer().push(block);
            }
            return ublock;
        }

        public void close() {
            popState0();
        }
    }

    private static final int MAX_NESTING = 10;

    private static final Pool<StateFrame> statePool = new Pool<>(StateFrame::new, MAX_NESTING);
    private static final ObjectArrayList<StateFrame> stack = new ObjectArrayList<>(MAX_NESTING);
    private static StateFrame stateFrame;

    public static Shader defaultShader;

    static {
        pushState0();
    }

    public static StateFrame state() {
        return stateFrame;
    }

    private static void popState0() {
        stack.removeLast();
        statePool.free(stateFrame);
        stateFrame = stack.getLast();
    }

    public static StateFrame pushState() {
        return pushState0();
    }

    private static StateFrame pushState0() {
        var newState = statePool.obtain();
        stack.addLast(newState);
        if (stateFrame != null) {
            newState.set(stateFrame);
        }
        stateFrame = newState;
        return newState;
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
        draw(tex, x, y, tex.width() * stateFrame.xScale, tex.height() * stateFrame.yScale);
    }

    public static void draw(Drawable tex, int colorRgba8888, float x, float y) {
        float w = tex.width() * stateFrame.xScale;
        float h = tex.height() * stateFrame.yScale;

        draw(
                stateFrame.rlist,
                stateFrame.primitiveType,
                stateFrame.layer,
                stateFrame.blending,
                tex.id(),
                stateFrame.shader.id(),
                stateFrame.ublock(),
                colorRgba8888,
                x, y,
                x + w, y + h,
                tex.u(), tex.v(),
                tex.u2(), tex.v2()
        );
    }

    public static void draw(Drawable tex, Color color, float x, float y) { draw(tex, color.rgba8888(), x, y); }

    public static void draw(Drawable tex, Color color, float x, float y, float w, float h) {
        draw(tex, color.rgba8888(), x, y, w, h);
    }

    public static void draw(Drawable tex, int colorRgba8888, float x, float y, float w, float h) {
        draw(
                stateFrame.rlist,
                stateFrame.primitiveType,
                stateFrame.layer,
                stateFrame.blending,
                tex.id(),
                stateFrame.shader.id(),
                stateFrame.ublock(),
                colorRgba8888,
                x, y,
                x + w, y + h,
                tex.u(), tex.v(),
                tex.u2(), tex.v2()
        );
    }

    public static void draw(Drawable tex, float x, float y, float w, float h) {
        draw(tex, stateFrame.colorRgba8888, x, y, w, h);
    }

    public static void draw(
            RenderList rlist,
            @PrimitiveType byte primitiveType,
            @Layer byte layer,
            @Blending byte blending,
            short texId,
            byte shader,
            int ublock,
            int rgba8888,
            float x, float y,
            float x2, float y2,
            float x3, float y3,
            float x4, float y4,
            float u, float v,
            float u2, float v2
    ) {


        short vertexCountPerQuad = queue.getVertexCountPerQuad(primitiveType);
        rlist.checkSpace(1, vertexCountPerQuad);

        var item = rlist.allocItem();

        item.vertexOffset = rlist.getVertexIndex();
        item.vertexCount = vertexCountPerQuad;

        rlist.addRectangle(primitiveType, rgba8888,
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
        item.indexCount = INDICES_PER_QUAD;

        item.sortKey = makeSortKey(primitiveType, layer, blending, texId, shader, ublock, rlist.getItemIndex());

        item.validate();
        rlist.push(item);
    }

    public static void draw(
            RenderList rlist,
            @PrimitiveType byte primitiveType,
            @Layer byte layer,
            @Blending byte blending,
            short texId,
            byte shaderId,
            int ublock,
            int rgba8888,
            float x, float y,
            float x2, float y2,
            float u, float v,
            float u2, float v2
    ) {
        short vertexCountPerQuad = queue.getVertexCountPerQuad(primitiveType);
        rlist.checkSpace(1, vertexCountPerQuad);

        var item = rlist.allocItem();
        item.vertexOffset = rlist.getVertexIndex();
        item.vertexCount = vertexCountPerQuad;
        rlist.addRectangle(primitiveType, rgba8888, x, y, x2, y2, u, v, u2, v2);

        short INDICES_PER_QUAD = 6;
        short VERTICES_PER_QUAD = 4;
        int quadIndex = item.vertexOffset / VERTICES_PER_QUAD;

        item.indexOffset = quadIndex * INDICES_PER_QUAD;
        item.indexCount = INDICES_PER_QUAD;
        item.sortKey = makeSortKey(primitiveType, layer, blending, texId, shaderId, ublock, rlist.getItemIndex());

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
                stateFrame.rlist,
                stateFrame.primitiveType,
                stateFrame.layer,
                stateFrame.blending,
                tex.id(),
                stateFrame.shader.id(),
                stateFrame.ublock(),
                colorRgba8888,
                x, y,
                x2, y2,
                x3, y3,
                x4, y4,
                tex.u(), tex.v(),
                tex.u2(), tex.v2()
        );
    }

    public static void z(@Layer byte z) {
        stateFrame.layer = z;
    }

    public static void blending(@Blending byte blending) {
        stateFrame.blending = blending;
    }

    public static void scale(float scale) {
        scale(scale, scale);
    }

    public static void scale(float xScale, float yScale) {
        stateFrame.xScale = xScale;
        stateFrame.yScale = yScale;
    }

    public static void color(Color color) { stateFrame.colorRgba8888 = color.rgba8888(); }
    public static void color(int rgba8888) { stateFrame.colorRgba8888 = rgba8888; }
    public static void resetColor() { stateFrame.colorRgba8888 = Color.white; }

    public static void camera(Camera2 camera) {
        stateFrame.logicalRatio.set(camera.projectionScale);
        var camPos = camera.position;
        stateFrame.cameraPosition.set(camPos.xf(), camPos.yf());
        resetUniformBlock();
    }

    public static void setUniformBlock(UniformBuffer.Block block) {
        stateFrame.ublock = block.id;
    }

    public static void resetUniformBlock() {
        stateFrame.ublock = UniformBuffer.Block.UNITIALIZED;
    }

    public static void shader(Shader shader) {
        stateFrame.shader = shader;
        resetUniformBlock(); // иначе упадём со странной ошибкой
    }

    public static void rlist(RenderList renderList) {
        stateFrame.rlist = renderList;
    }

    public static void primitiveType(@PrimitiveType byte primitiveType) {
        stateFrame.primitiveType = primitiveType;
    }
}
