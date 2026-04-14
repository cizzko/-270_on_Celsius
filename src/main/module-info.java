module core.main {
    requires org.lwjgl;
    requires org.lwjgl.glfw;
    requires org.lwjgl.opengl;
    requires org.lwjgl.jemalloc;

    requires jdk.jshell;

    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.iostreams;
    requires org.apache.logging.log4j.core;

    requires com.google.gson;
    requires com.fasterxml.jackson.databind;
    requires it.unimi.dsi.fastutil;

    requires java.desktop;
    requires jdk.management;

    // Авто-модули
    // requires jcodec;
    // requires jcodec.javase;

    requires transitive org.lwjgl.natives;
    requires transitive org.lwjgl.glfw.natives;
    requires transitive org.lwjgl.opengl.natives;
    requires transitive org.lwjgl.jemalloc.natives;
}
