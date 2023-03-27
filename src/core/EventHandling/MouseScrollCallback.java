package core.EventHandling;

import org.lwjgl.glfw.GLFWScrollCallback;

public class MouseScrollCallback extends GLFWScrollCallback {
    private double scrollOffset = 0.0;

    @Override
    public void invoke(long window, double xOffset, double yOffset) {
        scrollOffset += yOffset;
    }
    public double getScroll(){
        return scrollOffset;
    }
}