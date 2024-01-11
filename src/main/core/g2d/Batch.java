package core.g2d;

import core.Global;
import core.Utils.Disposable;
import core.Utils.SimpleColor;
import core.math.Mat3;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

import static core.g2d.VertexAttribute.*;
import static org.lwjgl.opengl.GL46.*;

public class Batch implements Disposable {
    private static final int VERTEX_PER_SPRITE   = 4;
    private static final int VERTEX_PER_TRIANGLE = 6;

    private static final VertexFormat VERTEX_FORMAT = VertexFormat.of(List.of(
            create(2, Type.FLOAT, Interp.NORMAL),
            create(4, Type.UNSIGNED_BYTE, Interp.COLOR),
            create(2, Type.FLOAT, Interp.NORMAL)
    ));

    // TODO
    //  Мне не нравится текущий вариант работы с color(), scale(), blending(),
    //  т.е. возвращение старого значения, чтобы потом его можно было восстановить
    //  Вижу следующие решения:
    //  1) void withColor(Color color, Runnable action)
    //  Но это накладно и возникнут пробелемы с кодом из-за effectively final
    //  2) void color(Color newColor) - Записывает старый цвет в дополнительное поле класса
    //     void resetColor()          - Восстанавливает цвет из доп. поля.
    //   Мне этот вариант кажется более эффективным, но с ним надо быть осторожным

    protected Blending blending = Blending.NORMAL;
    protected SimpleColor color = SimpleColor.WHITE;
    protected float colorBits = toBits(color);

    private static float toBits(SimpleColor color) {
        return Float.intBitsToFloat(color.getValueABGR() & 0xfeffffff);
    }

    protected final Mat3 matrix = new Mat3();

    protected float scale;
    protected Texture currentTexture;

    protected FloatBuffer vertices;
    protected Mesh mesh;
    protected Shader shader;
    protected int vertexCount;

    private boolean disposed;

    public final Blending blending(Blending blending) {
        Blending old = this.blending;
        this.blending = blending;
        return old;
    }

    // TODO не проверял, но кажется, что правильно сделал
    public final float scale(float scale) {
        float oldScale = this.scale;
        this.scale = scale;

        matrix(matrix.scale(scale));
        return oldScale;
    }

    public final SimpleColor color(SimpleColor color) {
        SimpleColor oldColor = this.color;
        this.color = color;
        this.colorBits = toBits(color);

        return oldColor;
    }

    public Batch(int bufferSize) {
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

        glBindVertexArray(0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public final void matrix(Mat3 matrix) {
        flush();

        this.matrix.set(matrix);
    }

    protected final void prepareTexture(Texture tex) {
        if (currentTexture != tex || vertices.remaining() < VERTEX_PER_SPRITE) {
            flush();
            currentTexture = tex;
        }
    }

    public void flush() {
        if (vertexCount == 0) {
            return;
        }

        blending.apply();

        shader.use();
        shader.setUniform("u_texture", currentTexture);
        shader.setUniformTransforming("u_proj", matrix);

        vertices.flip();
        mesh.bindVao();
        mesh.render(GL_TRIANGLES, vertices, vertexCount * VERTEX_PER_TRIANGLE);

        vertices.clear();
        vertexCount = 0;
    }

    // Отрисовка

    public final void draw(Drawable drawable) {
        draw(drawable, 0, 0);
    }

    public final void draw(Drawable drawable, float x, float y) {
        draw(drawable, x, y, drawable.width(), drawable.height());
    }

    public final void draw(Drawable drawable, float x, float y, float width, float height) {
        drawTexture(drawable, x, y, width, height);
    }

    protected void drawTexture(Drawable drawable, float x, float y, float w, float h) {
        prepareTexture(textureOf(drawable));

        rect(x, y,
                x + w, y + h,
                drawable.u(), drawable.v(),
                drawable.u2(), drawable.v2(),
                colorBits);
    }

    protected final Texture textureOf(Drawable drawable) {
        return switch (drawable) {
            case Font.Glyph gl -> gl.font().getTexture();
            case Atlas.Region region -> region.atlas().getTexture();
            case Texture tex -> tex;
        };
    }

    protected final void vertex(float x, float y,
                                float u, float v,
                                float color) {
        vertices.put(x);
        vertices.put(y);
        vertices.put(color);
        vertices.put(u);
        vertices.put(v);

        vertexCount++;
    }

    protected final void rect(float x1, float y1,
                              float x2, float y2,
                              float u1, float v1,
                              float u2, float v2,
                              float c) {

        vertex(x1, y1, u1, v2, c);
        vertex(x1, y2, u1, v1, c);
        vertex(x2, y2, u2, v1, c);
        vertex(x2, y1, u2, v2, c);
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
