package core.graphic.effects;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;

import core.World.TemperatureMap;
import core.content.blocks.Block;
import core.g2d.Fbo;
import core.g2d.Mesh;
import core.g2d.Shaders;
import core.math.Rectangle;
import core.util.Disposable;

import static core.Global.camera;
import static core.Global.input;
import static core.Global.player;
import static core.Global.world;
import static org.lwjgl.opengl.GL46.*;

public final class HeatHazePost implements Disposable {

    public static float RATE_LOW = 0.01f;
    public static float RATE_HIGH = 8f;
    public static final float HEMPM_FLOOR_TEMP = 300f;
    public static final float VIS_CUT = 0.05f;
    public static boolean enabled = true;
    public static boolean maskOverlay = false;

    private static final int MAX_MASK_CELLS = 1024;
    private static final int MAX_BLOCK_CELLS = 200_000;
    private static final int MASK_DRIFT_PAD = 1;
    private static final float HOLD_TAU_SECONDS = 0.6f;

    private final Rectangle region = new Rectangle();

    private Fbo fbo;
    private int fullscreenVao, fullscreenVbo;
    private int quadFbW, quadFbH;
    private int blockVao, blockVbo;
    private float[] blockVertices;
    private int blockVertCount;
    private FloatBuffer blockUploadBuf;
    private int rateTex = -1;
    private int regionW, regionH;
    private int texCapW, texCapH;
    private int minX, minY;
    private float[] rateData;
    private float[] holdData;
    private ByteBuffer rateBytes;

    private float elapsed;

    private float boundsX, boundsY, boundsW, boundsH;
    private int maskLoX, maskLoY;

    private volatile boolean active;

    public boolean active() {
        if (!enabled) {
            return false;
        }
        if (player == null || world == null) {
            return false;
        }
        return active;
    }

    public void reset() {
        active = false;
        boundsX = 0f;
        boundsY = 0f;
        boundsW = 0f;
        boundsH = 0f;
    }

    public void bind() {
        int vw = input.viewportWidth();
        int vh = input.viewportHeight();
        if (vw <= 0 || vh <= 0) {
            return;
        }
        if (fbo == null) {
            fbo = new Fbo(vw, vh);
        } else if (fbo.width() != vw || fbo.height() != vh) {
            fbo.resize(vw, vh);
        }
        fbo.bind();
    }

    public void unbind() {
        if (fbo == null) {
            return;
        }
        fbo.unbind();
        glViewport(input.viewportX(), input.viewportY(), input.viewportWidth(), input.viewportHeight());
    }

    public void draw() {
        if (fbo == null || rateTex == -1 || input.fbWidth() <= 0 || input.fbHeight() <= 0) {
            return;
        }

        updateFullscreenQuad();

        buildBlockQuads();
        if (blockVertCount > 0) {
            ensureBlockBuffers();
            uploadBlockQuads();
        }

        boolean blendWasEnabled = glIsEnabled(GL_BLEND);
        glDisable(GL_BLEND);

        var shader = Shaders.heatHaze;
        shader.use();
        shader.setUniformTexture2d("u_scene", (short) fbo.colorTex(), 0);
        shader.setUniformTexture2d("u_tempRate", (short) rateTex, 1);
        shader.setUniformVec2f("u_camera_origin", boundsX, boundsY);
        shader.setUniformVec2f("u_camera_size", boundsW, boundsH);
        shader.setUniformVec2f("u_mask_origin", (float) maskLoX, (float) maskLoY);
        shader.setUniformVec2f("u_mask_cells", (float) texCapW, (float) texCapH);
        shader.setUniformVec2f("u_region_cells", (float) regionW, (float) regionH);
        shader.setUniformFloat("u_time", elapsed);
        shader.setUniformFloat("u_visCut", VIS_CUT);
        shader.setUniformFloat("u_zoom", camera.zoom());

        glBindVertexArray(fullscreenVao);
        try {
            glViewport(0, 0, input.fbWidth(), input.fbHeight());
            shader.setUniformFloat("u_maskOverlay", 0f);
            shader.setUniformFloat("u_flat", 1f);
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
            if (blockVertCount > 0) {
                glBindVertexArray(blockVao);
                shader.setUniformFloat("u_flat", 0f);
                glDrawArrays(GL_TRIANGLES, 0, blockVertCount);
            }

            if (maskOverlay && blockVertCount > 0) {
                boolean oBlend = glIsEnabled(GL_BLEND);
                glEnable(GL_BLEND);
                glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA,
                                    GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
                shader.setUniformFloat("u_maskOverlay", 1f);
                shader.setUniformFloat("u_flat", 0f);
                glBindVertexArray(blockVao);
                glDrawArrays(GL_TRIANGLES, 0, blockVertCount);
                if (!oBlend) {
                    glDisable(GL_BLEND);
                }
            }
        } finally {
            glViewport(input.viewportX(), input.viewportY(), input.viewportWidth(), input.viewportHeight());
        }
        glBindVertexArray(0);

        Mesh.resetBindState();

        if (blendWasEnabled) {
            glEnable(GL_BLEND);
        }
    }

    private void ensureBlockBuffers() {
        if (blockVao != 0) {
            return;
        }
        blockVao = glGenVertexArrays();
        blockVbo = glGenBuffers();
        glBindVertexArray(blockVao);
        glBindBuffer(GL_ARRAY_BUFFER, blockVbo);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 5 * Float.BYTES, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 2L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, 5 * Float.BYTES, 4L * Float.BYTES);
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void buildBlockQuads() {
        blockVertCount = 0;
        if (regionW <= 0 || regionH <= 0 || rateData == null) {
            return;
        }
        float camX = camera.position.xf();
        float camY = camera.position.yf();
        float sx = camera.projectionScale.x;
        float sy = camera.projectionScale.y;
        float wx = boundsX, wy = boundsY, ww = boundsW, wh = boundsH;
        if (ww <= 0f || wh <= 0f) {
            return;
        }

        int cells = regionW * regionH;
        int stride = cells > MAX_BLOCK_CELLS ? Math.max(1, (int) Math.ceil(Math.sqrt((double) cells / MAX_BLOCK_CELLS))) : 1;
        int capacity = ((int) Math.min(cells, (long) MAX_BLOCK_CELLS)
                + (stride > 1 ? regionW + regionH : 0)) * 30;
        if (blockVertices == null || blockVertices.length < capacity) {
            blockVertices = new float[capacity];
        }
        float[] v = blockVertices;
        int p = 0;
        for (int j = 0; j < regionH; j += stride) {
            int by = minY + j;
            float vy0 = (by - wy) / wh;
            float vy1 = (by + 1 - wy) / wh;
            float ny0 = (by - camY) * sy;
            float ny1 = (by + 1 - camY) * sy;
            for (int i = 0; i < regionW; i += stride) {
                float intensity = rateData[j * regionW + i];
                if (intensity <= VIS_CUT) {
                    continue;
                }
                if (p + 5 > v.length) {
                    break;
                }
                int bx = minX + i;
                float u0 = (bx - wx) / ww;
                float u1 = (bx + 1 - wx) / ww;
                float nx0 = (bx - camX) * sx;
                float nx1 = (bx + 1 - camX) * sx;
                v[p++] = nx0; v[p++] = ny0; v[p++] = u0; v[p++] = vy0; v[p++] = intensity;
                v[p++] = nx1; v[p++] = ny0; v[p++] = u1; v[p++] = vy0; v[p++] = intensity;
                v[p++] = nx0; v[p++] = ny1; v[p++] = u0; v[p++] = vy1; v[p++] = intensity;
                v[p++] = nx1; v[p++] = ny0; v[p++] = u1; v[p++] = vy0; v[p++] = intensity;
                v[p++] = nx1; v[p++] = ny1; v[p++] = u1; v[p++] = vy1; v[p++] = intensity;
                v[p++] = nx0; v[p++] = ny1; v[p++] = u0; v[p++] = vy1; v[p++] = intensity;
            }
        }
        blockVertCount = p / 5;
    }

    private void uploadBlockQuads() {
        if (blockVertCount == 0 || blockVertices == null) {
            return;
        }
        int floats = blockVertCount * 5;
        if (blockUploadBuf == null || blockUploadBuf.capacity() < floats) {
            blockUploadBuf = BufferUtils.createFloatBuffer(floats);
        }
        var buf = blockUploadBuf;
        buf.clear().put(blockVertices, 0, floats).flip();

        glBindBuffer(GL_ARRAY_BUFFER, blockVbo);
        glBufferData(GL_ARRAY_BUFFER, buf, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void updateFullscreenQuad() {
        int fbW = input.fbWidth();
        int fbH = input.fbHeight();
        int vx = input.viewportX();
        int vy = input.viewportY();
        int vw = input.viewportWidth();
        int vh = input.viewportHeight();
        if (fullscreenVao != 0 && quadFbW == fbW && quadFbH == fbH) {
            return;
        }

        if (fullscreenVao == 0) {
            fullscreenVao = glGenVertexArrays();
            fullscreenVbo = glGenBuffers();
            glBindVertexArray(fullscreenVao);
            glBindBuffer(GL_ARRAY_BUFFER, fullscreenVbo);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0L);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2L * Float.BYTES);
            glBindVertexArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        }
        quadFbW = fbW;
        quadFbH = fbH;

        float ndcX0 = (vx / (float) fbW) * 2f - 1f;
        float ndcY0 = (vy / (float) fbH) * 2f - 1f;
        float ndcX1 = ((vx + vw) / (float) fbW) * 2f - 1f;
        float ndcY1 = ((vy + vh) / (float) fbH) * 2f - 1f;
        float[] verts = {
                ndcX0, ndcY0, 0f, 0f,
                ndcX1, ndcY0, 1f, 0f,
                ndcX0, ndcY1, 0f, 1f,
                ndcX1, ndcY1, 1f, 1f,
        };

        glBindBuffer(GL_ARRAY_BUFFER, fullscreenVbo);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var buf = stack.mallocFloat(verts.length);
            buf.put(verts).flip();
            glBufferData(GL_ARRAY_BUFFER, buf, GL_DYNAMIC_DRAW);
        }
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void updateCpu(float dt) {
        if (!enabled) {
            return;
        }
        elapsed += dt / core.Time.ONE_SECOND;
        active = false;
        if (player == null || world == null) {
            return;
        }

        camera.boundsTo(region);

        int loX = (int) Math.floor(region.x);
        int loY = (int) Math.floor(region.y);
        int needW = (int) Math.ceil(region.x + region.width) - loX;
        int needH = (int) Math.ceil(region.y + region.height) - loY;
        if (needW <= 0 || needH <= 0) {
            return;
        }

        int w = Math.min(needW, MAX_MASK_CELLS);
        int h = Math.min(needH, MAX_MASK_CELLS);
        regionW = w;
        regionH = h;

        boundsX = region.x;
        boundsY = region.y;
        boundsW = region.width;
        boundsH = region.height;
        maskLoX = loX;
        maskLoY = loY;

        if (rateData == null || rateData.length < w * h) {
            rateData = new float[w * h];
        }
        if (holdData == null || holdData.length < w * h) {
            holdData = new float[w * h];
        }

        float dtSeconds = dt / core.Time.ONE_SECOND;
        float holdDecay = (float) Math.exp(-dtSeconds / HOLD_TAU_SECONDS);

        boolean any = false;
        int idx = 0;

        for (int j = 0; j < h; j++) {
            int by = loY + j;
            for (int i = 0; i < w; i++) {
                int bx = loX + i;
                float intensity = 0f;
                if (world.inBounds(bx, by)) {
                    Block.Type cellType = world.getBlockType(bx, by);
                    if (cellType == Block.Type.GAS || cellType == Block.Type.WALKABLE) {
                        float rate = Math.abs(TemperatureMap.getTempChangeRate(bx, by));
                        float byRate = rate <= RATE_LOW
                                ? 0f
                                : Math.min((rate - RATE_LOW) / (RATE_HIGH - RATE_LOW), 1f);
                        float cellTemp = TemperatureMap.getTempSnapshotCell(bx, by);
                        float byTemp = cellTemp <= HEMPM_FLOOR_TEMP
                                ? 0f
                                : Math.min((cellTemp - HEMPM_FLOOR_TEMP) / 300f, 1f);
                        intensity = Math.max(byRate, byTemp);
                    }
                }
                float held = holdData[idx] * holdDecay;
                if (intensity > held) {
                    holdData[idx] = intensity;
                } else {
                    holdData[idx] = held;
                }
                rateData[idx] = holdData[idx];
                if (rateData[idx] > VIS_CUT) {
                    any = true;
                }
                idx++;
            }
        }
        active = any;

        if (!active) {
            return;
        }

        minX = loX;
        minY = loY;
    }

    public void updateGpu() {
        if (!active) {
            return;
        }
        ensureRateTexture(regionW, regionH);
        uploadRate();
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private void ensureRateTexture(int w, int h) {
        if (rateTex == -1) {
            rateTex = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, rateTex);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
        regionW = w;
        regionH = h;
        int targetW = texCapW == 0 ? Math.min(w + MASK_DRIFT_PAD, MAX_MASK_CELLS) : w;
        int targetH = texCapH == 0 ? Math.min(h + MASK_DRIFT_PAD, MAX_MASK_CELLS) : h;
        if (texCapW < targetW || texCapH < targetH) {
            texCapW = Math.max(texCapW, targetW);
            texCapH = Math.max(texCapH, targetH);
            int size = texCapW * texCapH;
            if (rateBytes == null || rateBytes.capacity() < size) {
                rateBytes = BufferUtils.createByteBuffer(size);
            }
            glBindTexture(GL_TEXTURE_2D, rateTex);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, texCapW, texCapH, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
            glBindTexture(GL_TEXTURE_2D, 0);
        }
    }

    private void uploadRate() {
        int size = regionW * regionH;
        rateBytes.clear();
        for (int i = 0; i < size; i++) {
            float v = rateData[i];
            rateBytes.put(v >= 1f ? (byte) 255 : (byte) (v * 255f + 0.5f));
        }
        rateBytes.flip();

        glBindTexture(GL_TEXTURE_2D, rateTex);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, regionW, regionH, GL_RED, GL_UNSIGNED_BYTE, rateBytes);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    public void close() {
        if (fbo != null) {
            fbo.close();
            fbo = null;
        }
        if (rateTex != -1) {
            glDeleteTextures(rateTex);
            rateTex = -1;
            texCapW = 0;
            texCapH = 0;
        }
        if (fullscreenVao != 0) {
            glDeleteVertexArrays(fullscreenVao);
            fullscreenVao = 0;
        }
        if (fullscreenVbo != 0) {
            glDeleteBuffers(fullscreenVbo);
            fullscreenVbo = 0;
        }
        if (blockVao != 0) {
            glDeleteVertexArrays(blockVao);
            blockVao = 0;
        }
        if (blockVbo != 0) {
            glDeleteBuffers(blockVbo);
            blockVbo = 0;
        }
        blockVertices = null;
        blockUploadBuf = null;
        blockVertCount = 0;
    }
}