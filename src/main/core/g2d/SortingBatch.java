package core.g2d;

import core.Utils.SimpleColor;
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
    protected void drawTexture(Drawable drawable, float x, float y, float w, float h) {
        RequestTexture request = textureRequestsPool.obtain();
        request.set(z, drawable, x, y, w, h, color);
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
                color(tex.color);

                try {
                    super.drawTexture(tex.drawable, tex.x, tex.y, tex.w, tex.h);
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
        public float x, y, w, h;
        public SimpleColor color;

        public RequestTexture() {}

        public void set(int z, Drawable drawable, float x,
                        float y, float w, float h, SimpleColor color) {
            this.z = z;
            this.drawable = drawable;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.color = color;
        }

        @Override
        public void reset() {
            z = 0;
            drawable = null;
            x = 0;
            y = 0;
            w = 0;
            h = 0;
            color = null;
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
