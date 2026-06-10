package core;

import core.graphic.GuiDrawing;

import static java.lang.Math.floor;

public final class WorldCoordinates {
    private WorldCoordinates() {}

    public static final float BLOCK_SIZE = GuiDrawing.blockSize;
    public static final float INV_BLOCK_SIZE = 1.0f / BLOCK_SIZE;

    public static float toPixels(float world) { return world * BLOCK_SIZE; }

    public static float toWorld(float pixels) { return pixels * INV_BLOCK_SIZE; }

    public static short toBlock(float world) { return (short) floor(world); }
    public static short toBlock(double world) { return (short) floor(world); }

    public static float toOffset(double world) { return (float) (world - floor(world)); }
}
