package core;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

//main -> developments -> event handling -> TextureDrawing
//main -> developments -> event handling -> physics -> hit box check -> TextureDrawing
public class main {

    static final Application app = new Application(Duration.ofNanos(300), 128);

    public static void main(String[] args) {
        Window window = Window.get();
        window.run();
        app.start();
    }

    static class Application {

        private final long updateInterval;
        private final LinkedBlockingQueue<Runnable> tasks;

        public Application(Duration updateInterval, int maxTasksCount) {
            this.updateInterval = updateInterval.toNanos();
            this.tasks = new LinkedBlockingQueue<>(maxTasksCount);
        }

        public boolean offerTask(Runnable task) {
            return tasks.offer(task);
        }

        public void start() {
            long wakeup = System.nanoTime() + updateInterval;
            while (!Thread.currentThread().isInterrupted()) {
                long current = System.nanoTime();
                if (wakeup > current) {
                    long sleep = wakeup - current;

                    try {
                        TimeUnit.NANOSECONDS.sleep(sleep);
                    } catch (InterruptedException e) {
                        break;
                    }

                    wakeup += updateInterval;
                } else {
                    wakeup = current + updateInterval;
                }

                // тут можешь что-то важное обновлять

                Runnable r;
                while (((r = tasks.poll()) != null)) {
                    r.run();
                }
            }
        }
    }

}