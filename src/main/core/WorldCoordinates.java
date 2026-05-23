package core;

import core.graphic.GuiDrawing;

import static java.lang.Math.floor;

public class WorldCoordinates {
    public static final float BLOCK_SIZE = GuiDrawing.blockSize;
    public static final float INV_BLOCK_SIZE = 1.0f / BLOCK_SIZE;

    public static float toPixels(float world) { return world * BLOCK_SIZE; }

    public static float toWorld(float pixels) { return pixels * INV_BLOCK_SIZE; }

    public static int toBlock(float world) { return (int) floor(world); }
}
