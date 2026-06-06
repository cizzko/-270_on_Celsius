package core;

public final class Time {
    private Time() {}

    public static final float ONE_SECOND = 60f;
    public static final float ONE_MILLISECOND = 1000 / Time.ONE_SECOND;

    public static float delta;
}
