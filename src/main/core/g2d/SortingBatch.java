package core.g2d;

import core.pool.Pool;
import core.pool.Poolable;

import java.util.PriorityQueue;

// TODO пул есть только для "обычных" запросов, а не для общих штук типа Runnable
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
        draw(new RequestProcedure(z, runnable));
    }

    public void draw(Request request) {
        if (flushing) {
            drawInternal(request);
            return;
        }
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
            // TODO надо решить что делать со следующими ситуациями:
            // 1) Если внутри Runnable происходит вызов любого draw().
            // Сейчас это ConcurrentModificationException в случае попытке добавить в очередь.
            // Можно воспринимать это действия "без очереди"
            // 2) Для RequestTexture на самом деле не нужно хранить (x, y, x2, y2, x3, y3, x4, y4)
            // Можно писать в промежуточный буфер вершин, который потом будет копироваться при отрисовке, а записывать в запрос лишь
            // (offset, length). Разница в потреблении: 56 против 32 байт/объект.
            //   Также хочу отметить, что это позволит сделать интерфейс для прямой записи вершин.
            // Может потом пригодиться для сложных геометрических штук.
            //   На самом деле интересно, как повлияет это копирование из одного массива в другой.
            requests.forEach(this::drawInternal);
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
        public final Runnable runnable;

        public RequestProcedure(int z, Runnable runnable) {
            super(z);
            this.runnable = runnable;
        }
    }
}
