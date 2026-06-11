open module core.main {
    requires transitive org.lwjgl;
    requires transitive org.lwjgl.glfw;
    requires transitive org.lwjgl.opengl;
    requires transitive org.lwjgl.jemalloc;

    requires transitive org.apache.logging.log4j;
    requires transitive org.apache.logging.log4j.iostreams;
    requires transitive org.apache.logging.log4j.core;

    requires transitive com.fasterxml.jackson.databind;
    requires transitive it.unimi.dsi.fastutil;
    requires transitive org.jetbrains.annotations;

    requires java.desktop;
    requires jdk.jshell;
    requires jdk.management;

    requires org.lwjgl.natives;
    requires org.lwjgl.glfw.natives;
    requires org.lwjgl.opengl.natives;
    requires org.lwjgl.jemalloc.natives;

    exports core.EventHandling;
    exports core.UI.Sounds;
    exports core.UI.animation;
    exports core.UI.hud;
    exports core.UI.menu;
    exports core.UI;
    exports core.World.Creatures.Player;
    exports core.World.Creatures;
    exports core.World.StaticWorldObjects;
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
