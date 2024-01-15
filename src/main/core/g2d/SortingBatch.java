package core.g2d;

import core.pool.Pool;
import core.pool.Poolable;

import java.util.PriorityQueue;

public class SortingBatch extends Batch {

    private final Pool<RequestTexture> textureRequestsPool;
    private final PriorityQueue<Request> requests;

    protected int z;
    protected boolean flushing;

    private int prevZ;

    public SortingBatch(int bufferSize, int poolSize, int queueSize) {
        super(bufferSize);
        this.textureRequestsPool = new Pool<>(RequestTexture::new, poolSize);
        this.requests = new PriorityQueue<>(queueSize);
    }

    public void draw(Runnable runnable) {
        draw(z, runnable);
    }

    public void draw(int z, Runnable runnable) {
        requests.add(new RequestProcedure(z, blending, runnable));
    }

    public void draw(Request request) {
        requests.add(request);
    }

    public void z(int z) {
        this.prevZ = this.z;
        this.z = z;
    }

    public final void resetZ() {
        z = prevZ;
    }

    @Override
    protected void drawTexture(Drawable drawable,
                               float x, float y,
                               float x2, float y2,
                               float x3, float y3,
                               float x4, float y4) {
        RequestTexture request = textureRequestsPool.obtain();
        request.set(z, drawable, x, y, x2, y2, x3, y3, x4, y4, colorBits);
        draw(request);
    }

    @Override
    public void flush() {
        if (flushing) {
            super.flush();
        } else {
            flushing = true;
            for (Request request : requests) {
                drawInternal(request);
            }
            requests.clear();
            super.flush();
            flushing = false;
        }
    }

    private void drawInternal(Request request) {
        switch (request) {
            case RequestProcedure proc -> proc.runnable.run();
            case RequestTexture tex -> {
                color(tex.colorBits);

                try {
                    super.drawTexture(tex.drawable, tex.x, tex.y, tex.x2, tex.y2, tex.x3, tex.y3, tex.x4, tex.y4);
                } finally {
                    resetColor();

                    textureRequestsPool.free(tex);
                }
            }
        }
    }

    public static abstract sealed class Request implements Comparable<Request> {
        public int z;

        protected Request() {}

        protected Request(int z) {
            this.z = z;
        }

        @Override
        public int compareTo(Request o) {
            return Integer.compare(z, o.z);
        }
    }

    public static final class RequestTexture extends Request implements Poolable {

        public Drawable drawable;
        public float x, y, x2, y2, x3, y3, x4, y4;
        public float colorBits;

        public RequestTexture() {}

        public void set(int z, Drawable drawable,
                        float x, float y,
                        float x2, float y2,
                        float x3, float y3,
                        float x4, float y4,
                        float colorBits) {
            this.z = z;
            this.drawable = drawable;
            this.x = x;
            this.y = y;
            this.x2 = x2;
            this.y2 = y2;
            this.x3 = x3;
            this.y3 = y3;
            this.x4 = x4;
            this.y4 = y4;
            this.colorBits = colorBits;
        }

        @Override
        public void reset() {
            z = 0;
            drawable = null;
            x = 0;
            y = 0;
            x2 = 0;
            y2 = 0;
            x3 = 0;
            y3 = 0;
            x4 = 0;
            y4 = 0;
            colorBits = 0;
        }
    }

    public static final class RequestProcedure extends Request {
        public final Blending blending;
        public final Runnable runnable;

        public RequestProcedure(int z, Blending blending, Runnable runnable) {
            super(z);
            this.blending = blending;
            this.runnable = runnable;
        }
    }
}
