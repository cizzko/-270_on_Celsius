module core.main {
    requires org.lwjgl;
    requires org.lwjgl.glfw;
    requires org.lwjgl.opengl;
    requires org.lwjgl.jemalloc;

    requires com.google.gson;
    requires it.unimi.dsi.fastutil;

    requires java.desktop;
    requires java.compiler;
    requires jdk.management;

    // Авто-модули
    // requires jcodec;
    // requires jcodec.javase;

    requires transitive org.lwjgl.natives;
    requires transitive org.lwjgl.glfw.natives;
    requires transitive org.lwjgl.opengl.natives;
    requires transitive org.lwjgl.jemalloc.natives;
}
