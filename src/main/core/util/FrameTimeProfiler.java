package core.util;

public class FrameTimeProfiler {
    private final float[] frameTimes;
    private int head = 0;
    private final int maxSamples;

    public FrameTimeProfiler(int maxSamples) {
        this.maxSamples = maxSamples;
        this.frameTimes = new float[maxSamples];
    }

    public void addFrameTime(float dtSecs) {
        frameTimes[head] = dtSecs * 1000f;
        head = (head + 1) % maxSamples;
    }

    public int maxSamples() { return maxSamples; }

    public float getSample(int index) {
        return frameTimes[(head + index) % maxSamples];
    }
}
