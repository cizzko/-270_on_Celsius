package core;

public final class Time {
    private Time() {}

    public static final float ONE_SECOND = 60f;
    public static final float MS_PER_TICK = 1000 / Time.ONE_SECOND;

    public static float delta;
}
