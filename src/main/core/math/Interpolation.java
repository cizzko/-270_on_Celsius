package core.math;

public interface Interpolation {
    Interpolation identity = a -> a;

    float apply(float a);
}
