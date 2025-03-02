package core.World.WorldGenerator;

import core.World.StaticWorldObjects.StaticWorldObjects;

public enum Biomes {
    //чем ближе к 90 тем меньше максимальный угол наклона линии генерации
    mountains(60, 20, 160, getDefBlocks(), "World\\Backdrops\\back"),
    plain(40, 40, 140, getDefBlocks(), "World\\Backdrops\\back"),
    forest(40, 40, 140, getDefBlocks(), "World\\Backdrops\\back"),
    desert(30, 60, 120, getDefBlocks(), "World\\Backdrops\\back");

    private static final Biomes defaultBiome = forest;
    private final int blockGradientChance, upperBorder, bottomBorder;
    private final String backdrop;
    private final short[] blocks;

    Biomes(int blockGradientChance, int upperBorder, int bottomBorder, short[] blocks, String backdrop) {
        this.blockGradientChance = blockGradientChance;
        this.upperBorder = upperBorder;
        this.bottomBorder = bottomBorder;
        this.blocks = blocks;
        this.backdrop = backdrop;
    }

    public int getBlockGradientChance() {
        return blockGradientChance;
    }

    public int getUpperBorder() {
        return upperBorder;
    }

    public int getBottomBorder() {
        return bottomBorder;
    }

    public short[] getBlocks() {
        return blocks;
    }

    public String getBackdrop() {
        return backdrop;
    }

    public static Biomes getDefault() {
        return defaultBiome;
    }

    public static Biomes getRand() {
        return Biomes.values()[(int) (Math.random() * Biomes.values().length)];
    }

    //временно, чтоб закрыть дырки
    private static short[] getDefBlocks() {
        return new short[]{
                StaticWorldObjects.createStatic("Blocks/grass"),
                StaticWorldObjects.createStatic("Blocks/dirt"),
                StaticWorldObjects.createStatic("Blocks/dirtStone"),
                StaticWorldObjects.createStatic("Blocks/stone")};
    }
}
