module core.main {
    requires org.lwjgl.glfw;
    requires org.lwjgl.opengl;
    requires java.desktop;
    requires java.compiler;
    requires com.google.gson;
    requires jdk.management;
    requires it.unimi.dsi.fastutil;

    // Авто-модули
    // requires jcodec;
    // requires jcodec.javase;

    requires transitive org.lwjgl.natives;
    requires transitive org.lwjgl.glfw.natives;
    requires transitive org.lwjgl.opengl.natives;
}
