package core.EventHandling;

import org.lwjgl.glfw.GLFWScrollCallback;

public class MouseScrollCallback extends GLFWScrollCallback {
    private static double scrollOffset = 0.0;

    @Override
    public void invoke(long window, double xOffset, double yOffset) {
        scrollOffset += yOffset;
    }
    public static double getScroll(){
        return scrollOffset;
    }
}