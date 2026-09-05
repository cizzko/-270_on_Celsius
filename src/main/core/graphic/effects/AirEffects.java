package core.graphic.effects;

public final class AirEffects {
    private AirEffects() {
    }

    public static final HeatHazePost heatHazePost = new HeatHazePost();

    public static void update(float dt) {
        heatHazePost.updateCpu(dt);
    }

    public static void updateGpu() {
        heatHazePost.updateGpu();
    }

    public static void bind() {
        if (heatHazePost.active()) {
            heatHazePost.bind();
        }
    }

    public static void draw() {
        if (heatHazePost.active()) {
            heatHazePost.draw();
        }
    }

    public static void unbind() {
        heatHazePost.unbind();
    }
}
