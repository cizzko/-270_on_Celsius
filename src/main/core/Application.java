package core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.system.NativeResource;

import java.util.ArrayList;
import java.util.Objects;

import static core.util.ImportClassMethod.exec;
import static core.util.ImportClassMethod.jshell;

public class Application {
    public static final Logger log = LogManager.getLogger("Game");

    private final Thread mainThread;

    protected final ArrayList<NativeResource> natives = new ArrayList<>();
    protected final ArrayList<ApplicationListener> listeners = new ArrayList<>();

    private boolean running = true;

    public Application() {
        this.mainThread = Thread.currentThread();

        Global.app = this;
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
            while (running) {
                update();
            }
        } catch (Throwable t) {
            log.error("The fatal exception is caused", t);
        } finally {
            freeNatives();
            jshell.stop();
            jshell.close();
            exec.shutdown();
            Global.scheduler.shutdown();
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

    public void quit() {
        running = false;
    }

    public void setFramerate(int framerate) {
        this.framerate = framerate;
    }

    public void ensureMainThread() {
        if (!isMainThread()) {
            throw new IllegalStateException("Async access");
        }
    }

    public void addListener(ApplicationListener listener) {
        Objects.requireNonNull(listener);
        ensureMainThread();
        listeners.add(listener);
    }

    private int framerate = -1;
    private long prevFrameTime;
    private long prevSwapTime;
    private long frameCounterTime;
    private int fps, fpsMeasurement;

    {
        prevFrameTime = prevSwapTime = frameCounterTime = System.nanoTime();
    }

    protected void updateTime() {
        long now = System.nanoTime();

        float deltaTime = (now - prevFrameTime) * 1e-9f;
        prevFrameTime = now;

        Time.delta = Math.clamp(deltaTime * Time.ONE_SECOND, 0.0001f, Time.ONE_SECOND / 10f);

        if (now - frameCounterTime >= 1e9f) {
            frameCounterTime = now;

            fps = fpsMeasurement;
            fpsMeasurement = 0;
        }

        fpsMeasurement++;
    }

    protected void nextFrame() {
        if (framerate > 0) {
            long elapsedTime = System.nanoTime() - prevSwapTime;
            double frameTime = 1e9 / framerate;
            if (elapsedTime < frameTime) {
                long toSleep = (long) (frameTime - elapsedTime);
                try {
                    Thread.sleep((toSleep / 1_000_000), (int)(toSleep % 1_000_000));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        prevSwapTime = System.nanoTime();
    }

    public final int getFps() {
        return fps;
    }

    public final int getFpsMeasurement() {
        return fpsMeasurement;
    }
}
