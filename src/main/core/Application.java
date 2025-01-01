package core;

import core.EventHandling.Logging.Logger;

import java.util.ArrayList;
import java.util.Objects;

public class Application {
    private final Thread mainThread;

    protected final ArrayList<ApplicationListener> listeners = new ArrayList<>();

    public Application() {
        this.mainThread = Thread.currentThread();

        Global.app = this;
    }

    public boolean isMainThread() {
        return Thread.currentThread() == mainThread;
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

    public void suspend() {
        for (ApplicationListener listener : listeners) {
            try {
                listener.suspend();
            } catch (Throwable t) {
                Logger.printException("Failed to suspend ApplicationListener: " + listener, t);
            }
        }
    }

    public void resume() {
        for (ApplicationListener listener : listeners) {
            try {
                listener.resume();
            } catch (Throwable t) {
                Logger.printException("Failed to resume ApplicationListener: " + listener, t);
            }
        }
    }

    private long prevFrameTime = -1; // Для deltaTime
    private long frameCounterTime = -1;
    private int fps, fpsMeasurement;

    protected void updateTime() {
        long now = System.nanoTime();
        if (prevFrameTime == -1) {
            prevFrameTime = now;
        }

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

    public final int getFps() {
        return fps;
    }

    public final int getFpsMeasurement() {
        return fpsMeasurement;
    }
}
