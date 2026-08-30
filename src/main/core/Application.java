package core;

import core.util.FrameTimeProfiler;
import core.util.JavaInterpreter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.NativeResource;
import org.lwjgl.system.Platform;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

public class Application {
    public static final Logger log = LogManager.getLogger("Game");

    private final Thread mainThread;

    protected final ArrayList<NativeResource> natives = new ArrayList<>();

    private boolean running = true;

    private static final VarHandle RUNNING;
    static {
        try {
            RUNNING = MethodHandles.lookup()
                    .findVarHandle(Application.class, "running", boolean.class)
                    .withInvokeExactBehavior();
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public Application() {
        this.mainThread = Thread.currentThread();
    }

    public <N extends NativeResource> N keep(N aNative) {
        Objects.requireNonNull(aNative);
        natives.add(aNative);
        return aNative;
    }

    public boolean isMainThread() {
        return Thread.currentThread() == mainThread;
    }

    public void run() {
        try {
            Thread.currentThread().setName("UpdateThread");
            init();

            while (isRunning()) {
                update();
            }
        } catch (Throwable t) {
            log.error("The fatal exception is caused", t);
        } finally {
            freeNatives();
            JavaInterpreter.close();
            Global.scheduler.shutdown();
            try {
                Global.renderThread.join();
            } catch (InterruptedException e) {}
            cleanup();
        }
    }

    private void freeNatives() {
        for (NativeResource aNative : natives) {
            try {
                aNative.free();
            } catch (Throwable t) {
                log.error("Failed to release the native resource {}", aNative, t);
            }
        }
    }

    protected void update() {

    }

    protected void cleanup() {

    }

    protected void init() throws Throwable {

    }

    public void setFramerate(int framerate) {
        this.framerate = framerate;
    }

    public int framerate() {
        return framerate;
    }

    public void ensureMainThread() {
        if (!isMainThread()) {
            throw new IllegalStateException("Async access");
        }
    }

    private int framerate = -1;
    private long prevFrameTime;
    private long prevSwapTime;
    private long frameCounterTime;
    private int fps, fpsMeasurement;

    {
        prevFrameTime = prevSwapTime = frameCounterTime = System.nanoTime();
    }

    public final FrameTimeProfiler profiler = new FrameTimeProfiler(50);

    protected void updateTime() {
        long now = System.nanoTime();

        float deltaTime = (now - prevFrameTime) * 1e-9f;
        prevFrameTime = now;

        profiler.addFrameTime(deltaTime);
        Time.delta = Math.clamp(deltaTime * Time.ONE_SECOND, 0.0001f, Time.ONE_SECOND / 10f);

        if (now - frameCounterTime >= 1e9f) {
            frameCounterTime = now;

            fps = fpsMeasurement;
            fpsMeasurement = 0;
        }

        fpsMeasurement++;
    }

    protected void nextFrame() {
        int fr = framerate;
        if (fr > 0) {
            double frameTime = 1e9 / fr;
            long elapsedTime = System.nanoTime() - prevSwapTime;

            if (elapsedTime < frameTime) {
                long toWait = (long) (frameTime - elapsedTime);
                long targetTime = System.nanoTime() + toWait;

                while (toWait > 100_000L) {
                    LockSupport.parkNanos(toWait - 50_000L); // закладываем время под spurious wakeup
                    toWait = targetTime - System.nanoTime();
                }
                while (System.nanoTime() < targetTime)
                    Thread.onSpinWait();
            }
            prevSwapTime = System.nanoTime();
        }
    }

    public final int fps() {
        return fps;
    }

    public static void open(String uri) {
        switch (Platform.get()) {
            case LINUX, FREEBSD -> openUri("xdg-open", uri);
            case MACOSX -> openUri("open", uri);
            case WINDOWS -> openUri("rundll32", "url.dll,FileProtocolHandler", uri);
        }
    }

    private static void openUri(String cmd, String arg0, String arg1) {
        Thread.startVirtualThread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, arg0);
                var list = pb.command();
                if (!arg1.isEmpty()) {
                    list.add(arg1);
                }
                pb.start();
            } catch (IOException e) {
                Application.log.warn("Failed to open uri '{}'", cmd, e);
            }
        });
    }

    private static void openUri(String cmd, String uri) {
        openUri(cmd, uri, "");
    }

    public final boolean isRunning() {
        return (boolean) RUNNING.getAcquire(this);
    }

    public final void quit() {
        ensureMainThread();
        RUNNING.setRelease(this, false);
    }
}
