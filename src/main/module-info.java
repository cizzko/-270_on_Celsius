open module core.main {
    requires org.lwjgl;
    requires org.lwjgl.glfw;
    requires org.lwjgl.opengl;
    requires org.lwjgl.jemalloc;

    requires jdk.jshell;

    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.iostreams;
    requires org.apache.logging.log4j.core;

    requires com.fasterxml.jackson.databind;
    requires it.unimi.dsi.fastutil;
    requires org.jetbrains.annotations;

    requires java.desktop;
    requires jdk.management;

    requires transitive org.lwjgl.natives;
    requires transitive org.lwjgl.glfw.natives;
    requires transitive org.lwjgl.opengl.natives;
    requires transitive org.lwjgl.jemalloc.natives;

    exports core.EventHandling;
    exports core.UI.Sounds;
    exports core.UI.animation;
    exports core.UI.hud;
    exports core.UI.menu;
    exports core.UI;
    exports core.World.Creatures.Player;
    exports core.World.Creatures;
    exports core.World.Weather;
    exports core.World.WorldGenerator;
    exports core.World;
    exports core.assets;
    exports core.content.blocks.data;
    exports core.content.blocks;
    exports core.content.creatures;
    exports core.content.entity;
    exports core.content.items.data;
    exports core.content.items;
    exports core.content.serialize;
    exports core.content.strctures;
    exports core.content;
    exports core.g2d;
    exports core.graphic;
    exports core.input;
    exports core.lang;
    exports core.math;
    exports core.pool;
    exports core.util;
    exports core;
}
