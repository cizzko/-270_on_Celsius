package core.World.WorldGenerator;

import core.World.StaticWorldObjects.StaticWorldObjects;

public enum Biomes {
    //чем ближе к 90 тем меньше максимальный угол наклона линии генерации
    mountains(60, 20, 160, getMountains(), "World\\Backdrops\\back"),
    plain(40, 40, 140, getPlain(), "World\\Backdrops\\back"),
    forest(40, 40, 140, getForest(), "World\\Backdrops\\back"),
    desert(30, 60, 120, getDesert(), "World\\Backdrops\\back");

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

    private static short[] getMountains() {
        return new short[]{
                StaticWorldObjects.createStatic("Blocks/stone")};
    }

    //todo снег нарисовать
    private static short[] getPlain(){
        return new short[]{
                StaticWorldObjects.createStatic("Blocks/grass"),
                StaticWorldObjects.createStatic("Blocks/dirt"),
                StaticWorldObjects.createStatic("Blocks/dirt"),
                StaticWorldObjects.createStatic("Blocks/dirtStone"),
                StaticWorldObjects.createStatic("Blocks/stone")};
    }

    private static short[] getForest() {
        return new short[]{
                StaticWorldObjects.createStatic("Blocks/grass"),
                StaticWorldObjects.createStatic("Blocks/dirt"),
                StaticWorldObjects.createStatic("Blocks/dirtStone"),
                StaticWorldObjects.createStatic("Blocks/stone")};
    }

    private static short[] getDesert() {
        return new short[]{
                StaticWorldObjects.createStatic("Blocks/sand"),
                StaticWorldObjects.createStatic("Blocks/sand"),
                StaticWorldObjects.createStatic("Blocks/sand"),
                StaticWorldObjects.createStatic("Blocks/stone")};
    }
}
