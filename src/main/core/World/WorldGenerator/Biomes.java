package core.World.WorldGenerator;

import core.Global;

public enum Biomes {
    //чем ближе к 90 тем меньше максимальный угол наклона линии генерации
    mountains(60, 20, 160, 1, getMountains(), "World\\Backdrops\\back"),
    plain(30, 40, 140, 1, getPlain(), "World\\Backdrops\\back"),
    forest(40, 40, 140, 1, getForest(), "World\\Backdrops\\back"),
    desert(30, 60, 120, 1, getDesert(), "World\\Backdrops\\back"),
    snowed(30, 60, 120, 1, getSnowed(), "World\\backdrops\\back");

    private static final Biomes defaultBiome = forest;
    private final int blockGradientChance, upperBorder, bottomBorder, chanceDecrease;
    private final String backdrop;
    private final short[] blocks;

    //int blockGradientChance - насколько острым могут быть углы,
    //int upperBorder - максимальный угол, int bottomBorder - максимальный угол,
    //int chanceDecrease - шанс удаления одного или нескольких верхних блоков 0-100
    Biomes(int blockGradientChance, int upperBorder, int bottomBorder, int chanceDecrease, short[] blocks, String backdrop) {
        this.blockGradientChance = blockGradientChance;
        this.upperBorder = upperBorder;
        this.bottomBorder = bottomBorder;
        this.blocks = blocks;
        this.backdrop = backdrop;
        this.chanceDecrease = chanceDecrease;
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

    public int getChanceDecrease() {
        return chanceDecrease;
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

    private static short shortIdByName(String name) {
        return (short) Global.content.getBlockIdByType(Global.content.getConstById(name));
    }

    private static short[] getMountains() {
        return new short[]{
                shortIdByName("snow"),
                shortIdByName("stone")
        };
    }

    private static short[] getPlain(){
        return new short[]{
                shortIdByName("grass"),
                shortIdByName("dirt"),
                shortIdByName("dirt"),
                shortIdByName("dirtStone"),
                shortIdByName("stone")
        };
    }

    private static short[] getForest() {
        return new short[]{
                shortIdByName("grass"),
                shortIdByName("dirt"),
                shortIdByName("dirtStone"),
                shortIdByName("stone")
        };
    }

    private static short[] getDesert() {
        return new short[]{
                shortIdByName("sand"),
                shortIdByName("sand"),
                shortIdByName("sand"),
                shortIdByName("stone")
        };
    }

    private static short[] getSnowed() {
        return new short[]{
                shortIdByName("snow"),
                shortIdByName("snow"),
                shortIdByName("snow"),
                shortIdByName("snow"),
                shortIdByName("stone")
        };
    }
}
