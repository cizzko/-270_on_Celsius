package core.g2d;

import core.assets.EventLoopExecutor;
import org.jctools.queues.atomic.SpscAtomicArrayQueue;
import org.lwjgl.opengl.GL;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static core.Global.app;
import static core.Window.glfwHandle;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL46C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL46C.glClear;

public final class RenderThread extends Thread implements EventLoopExecutor {
    private final SpscAtomicArrayQueue<Runnable> tasks = new SpscAtomicArrayQueue<>(64);

    public RenderThread() {
        super("RenderThread");
    }

    public boolean schedule(Runnable task) {
        return tasks.offer(task);
    }

    public boolean isRenderThread() {
        return Thread.currentThread() == this;
    }

    @Override
    public <T> CompletableFuture<T> submit(Callable<? extends T> action) {
        if (isRenderThread()) {
            try {
                return CompletableFuture.completedFuture(action.call());
            } catch (Throwable t) {
                return CompletableFuture.failedFuture(t);
            }
        }

        var fut = new CompletableFuture<T>();
        tasks.offer(() -> {
            try {
                fut.complete(action.call());
            } catch (Throwable t) {
                fut.completeExceptionally(t);
            }
        });
        return fut;
    }

    private void executePendingTasks() {
        var queue = tasks;
        if (!queue.isEmpty()) // в теории гонка, но не важно, гоняем каждый такт
            queue.drain(Runnable::run);
    }

    private int framerate = -1;
    private long prevSwapTime;
    private long frameCounterTime;
    private int fpsMeasurement;
    @SuppressWarnings("unused")
    private int fps; // opaque поскольку это счётчик и порядок эффектов неважен

    public static final VarHandle FPS;
    static {
        try {
            FPS = MethodHandles.lookup()
                    .findVarHandle(RenderThread.class, "fps", int.class)
                    .withInvokeExactBehavior();
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    {
        prevSwapTime = frameCounterTime = System.nanoTime();
    }

    public int fps() { return (int)FPS.getOpaque(this); }

    private void updateTime() {
        long now = System.nanoTime();

        if (now - frameCounterTime >= 1_000_000_000L) {
            frameCounterTime = now;

            FPS.setOpaque(this, fpsMeasurement);
            fpsMeasurement = 0;
        }

        fpsMeasurement++;
    }

    private void nextFrame() {
        int fr = framerate;
        if (fr > 0) {
            double frameTime = 1e9 / fr;
            long elapsedTime = System.nanoTime() - prevSwapTime;

            if (elapsedTime < frameTime) {
                long toWait = (long) (frameTime - elapsedTime);
                long targetTime = System.nanoTime() + toWait;

                while (toWait > 1_500_000L) {
                    long sleepMs = (toWait / 1_000_000L) - 1;
                    if (sleepMs <= 0) break;

                    try {
                        Thread.sleep(sleepMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }

                    toWait = targetTime - System.nanoTime();
                }

                while (System.nanoTime() < targetTime)
                    Thread.onSpinWait();
            }
            prevSwapTime = System.nanoTime();
        }
    }

    public void run() {

        glfwMakeContextCurrent(glfwHandle);
        GL.createCapabilities(true);

        try {
            MethodHandles.lookup().ensureInitialized(Render.class);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // ААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААААА
        // if (Debug.debugLevel >= 5) {
        //     glEnable(GL_DEBUG_OUTPUT);
        //     Global.app.keep(GLUtil.setupDebugMessageCallback());
        //     Global.app.keep(() -> glDisable(GL_DEBUG_OUTPUT));
        // }

        Render.init();

        glClearColor(206f / 255f, 246f / 255f, 1.0f, 1.0f);

        executePendingTasks();

        var queue = Render.queue();
        var buffer = queue.buffer;
        while (app.isRunning()) {
            updateTime();
            executePendingTasks();

            if (buffer.tryConsume()) {
                var next = buffer.peek();

                glClear(GL_COLOR_BUFFER_BIT);

                queue.submitCommandList(next);
            }

            glfwSwapBuffers(glfwHandle);

            nextFrame();
        }
    }

    public void setFramerate(int framerate) {
        this.framerate = framerate;
    }

    public void setVerticalSync(boolean state) {
        submit(() -> {
            if (state) {
                glfwSwapInterval(1);
            } else {
                glfwSwapInterval(0);
            }
        });
    }

    @Override
    public boolean isExecutorThread() {
        return isRenderThread();
    }

    @Override
    public CompletableFuture<Void> submit(Runnable action) {
        if (isRenderThread()) {
            try {
                action.run();
                return CompletableFuture.completedFuture(null);
            } catch (Throwable t) {
                return CompletableFuture.failedFuture(t);
            }
        }

        var fut = new CompletableFuture<Void>();
        tasks.offer(() -> {
            try {
                action.run();
                fut.complete(null);
            } catch (Throwable t) {
                fut.completeExceptionally(t);
            }
        });
        return fut;
    }

    public void ensureThisThread() {
        if (!isRenderThread())
            throw new IllegalStateException("Async render state access");
    }

    @Override
    public void execute(Runnable action) {
        tasks.offer(action);
    }
}
